/*
 * Copyright (c) 2016-2022 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * Licensed under the GNU General Public License v3.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.output;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import org.locationtech.jts.operation.buffer.validate.DistanceToPointFinder;
import org.locationtech.jts.operation.buffer.validate.PointPairDistance;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.io.Utils;
import de.dlr.ivf.urmo.router.shapes.DBEdge;

/**
 * @class EdgeMappingWriter
 * @brief Writes the results of the mapping of sources / destinations to edges
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class EdgeMappingWriter extends BasicCombinedWriter {
	/**
	 * @brief Constructor
	 * 
	 * Opens the connection to a PostGIS database and builds the table
	 * @param format The used format
	 * @param inputParts The definition of the input/output source/destination
	 * @param precision The floating point precision to use
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @param rsid The RSID to use
	 * @throws IOException When something fails
	 */
	public EdgeMappingWriter(Utils.Format format, String[] inputParts, int precision, 
			boolean dropPrevious, int rsid) throws IOException {
		super(format, inputParts, "X-to-road-output", precision, dropPrevious, 
				"(gid bigint, rid text, rpos real, dist real)");
		if(format==Utils.Format.FORMAT_POSTGRES) {
			addGeometryColumn("pos", rsid, "LINESTRING", 2);
		}
	}


	/** @brief Get the insert statement string
	 * @param[in] format The used output format
	 * @param[in] rsid The used projection
	 * @return The insert statement string
	 */
	protected String getInsertStatement(Utils.Format format, int rsid) {
		if(format==Utils.Format.FORMAT_POSTGRES) {
			return "VALUES (?, ?, ?, ?, ST_GeomFromText(?, " + rsid + "))";
		}
		return "VALUES (?, ?, ?, ?, ?)";
	}


	/**
	 * Writes the mapping results
	 * @param nearestEdges The map of edges to objects to write
	 * @throws IOException When something fails
	 */
	public void writeResults(HashMap<DBEdge, Vector<MapResult>> nearestEdges) throws IOException {
		Vector<DBEdge> edges = new Vector<>(nearestEdges.keySet());
		Collections.sort(edges, new Comparator<DBEdge>() {
            public int compare(DBEdge e1, DBEdge e2) {
                return e1.getID().compareTo(e2.getID());
            }});
		for (DBEdge e : edges) {
			if (e == null) {
				continue;
			}
			Vector<MapResult> ress = nearestEdges.get(e);
			Collections.sort(ress, new Comparator<MapResult>() {
	            public int compare(MapResult m1, MapResult m2) {
	            	long i1 = m1.em.getOuterID();
	            	long i2 = m2.em.getOuterID();
	                return i1 > i2 ? 1 : i1 < i2 ? -1 : 0;
	            }});
			for (MapResult o : ress) {
				PointPairDistance ppd = new PointPairDistance();
				DistanceToPointFinder.computeDistance(e.geom, o.em.getPoint().getCoordinate(), ppd);
				if (intoDB()) {
					try {
						_ps.setLong(1, o.em.getOuterID());
						_ps.setString(2, e.id);
						_ps.setFloat(3, (float) o.pos);
						_ps.setFloat(4, (float) o.dist);
						String p1 = ppd.getCoordinate(0).x + " " + ppd.getCoordinate(0).y;
						String p2 = ppd.getCoordinate(1).x + " " + ppd.getCoordinate(1).y;
						_ps.setString(5, "LINESTRING(" + p1 + "," + p2 + ")");
						_ps.addBatch();
					} catch (SQLException ex) {
						throw new IOException(ex);
					}
				} else {
					_fileWriter.append(o.em.getOuterID() + ";" + e.id + ";" 
							+ String.format(Locale.US, _FS, o.pos) + ";" 
							+ String.format(Locale.US, _FS, o.dist) + ";" 
							+ String.format(Locale.US, _FS, ppd.getCoordinate(0).x) + ";" 
							+ String.format(Locale.US, _FS, ppd.getCoordinate(0).y) + ";" 
							+ String.format(Locale.US, _FS, ppd.getCoordinate(1).x) + ";" 
							+ String.format(Locale.US, _FS, ppd.getCoordinate(1).y) + "\n");
				}
			}
		}
	}
	
	
	/**
	 * @brief Adds a comment to the database
	 * @param comment The comment to add
	 * @throws IOException When something fails
	 */
	public void addComment(String comment) throws IOException {
		if (_allowsComments) {
			try {
				String sql = "COMMENT ON TABLE " + _tableName + " IS '" + comment + "';";
				Statement s = _connection.createStatement();
				s.executeUpdate(sql);
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		}
	}

}

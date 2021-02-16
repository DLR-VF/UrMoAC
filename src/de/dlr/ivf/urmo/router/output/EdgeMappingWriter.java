/**
 * Copyright (c) 2016-2021 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * @author: Daniel.Krajzewicz@dlr.de
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
import java.util.HashMap;
import java.util.Vector;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.operation.buffer.validate.DistanceToPointFinder;
import com.vividsolutions.jts.operation.buffer.validate.PointPairDistance;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
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
	 * Opens the connection to a database and builds the table
	 * @param url The URL to the database
	 * @param tableName The name of the table
	 * @param user The name of the database user
	 * @param pw The password of the database user
	 * @param rsid The RSID to use
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @throws SQLException When something fails
	 */
	public EdgeMappingWriter(String url, String tableName, String user, String pw, int rsid, boolean dropPrevious) throws SQLException {
		super(url, user, pw, tableName, "(gid bigint, rid text, rpos real, dist real)", "VALUES (?, ?, ?, ?, ST_GeomFromText(?, " + rsid + "))", dropPrevious);
		addGeometryColumn("pos", rsid, "LINESTRING", 2);
	}


	/**
	 * @brief Constructor
	 * 
	 * Opens the file to write the results to
	 * @param fileName The path to the file to write the results to
	 * @throws IOException When something fails
	 */
	public EdgeMappingWriter(String fileName) throws IOException {
		super(fileName);
	}


	/**
	 * Writes the mapping results
	 * @param nearestEdges The map of edges to objects to write
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 * @throws NoSuchAuthorityCodeException When something fails
	 * @throws FactoryException When something fails
	 * @throws TransformException When something fails
	 */
	public void writeResults(HashMap<DBEdge, Vector<MapResult>> nearestEdges)
			throws SQLException, IOException, NoSuchAuthorityCodeException, FactoryException, TransformException {
		for (DBEdge e : nearestEdges.keySet()) {
			if (e == null) {
				continue;
			}
			Vector<MapResult> ress = nearestEdges.get(e);
			for (MapResult o : ress) {
				PointPairDistance ppd = new PointPairDistance();
				DistanceToPointFinder.computeDistance(e.geom, o.em.getPoint().getCoordinate(), ppd);
				if (intoDB()) {
					ps.setLong(1, o.em.getOuterID());
					ps.setString(2, e.id);
					ps.setFloat(3, (float) o.pos);
					ps.setFloat(4, (float) o.dist);
					String p1 = ppd.getCoordinate(0).x + " " + ppd.getCoordinate(0).y;
					String p2 = ppd.getCoordinate(1).x + " " + ppd.getCoordinate(1).y;
					ps.setString(5, "LINESTRING(" + p1 + "," + p2 + ")");
					ps.addBatch();
				} else {
					fileWriter.append(o.em.getOuterID() + ";" + e.id + ";" + o.pos + ";" + o.dist + ";" 
							+ ppd.getCoordinate(0).x + ";" + ppd.getCoordinate(0).y + ";" 
							+ ppd.getCoordinate(1).x + ";" + ppd.getCoordinate(1).y + "\n");
				}
			}
		}
	}
	
	
	/**
	 * @brief Adds a comment to the database
	 * @param comment The comment to add
	 * @throws SQLException When something fails
	 */
	public void addComment(String comment) throws SQLException {
		if (intoDB()) {
			String sql = "COMMENT ON TABLE " + tableName + " IS '" + comment + "';";
			Statement s = connection.createStatement();
			s.executeUpdate(sql);
		}
	}

}

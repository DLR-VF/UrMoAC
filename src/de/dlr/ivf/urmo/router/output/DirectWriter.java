/*
 * Copyright (c) 2017-2024
 * Institute of Transport Research
 * German Aerospace Center
 * 
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * https://github.com/DLR-VF/UrMoAC
 * Licensed under the Eclipse Public License 2.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraße 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.output;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Locale;
import java.util.Vector;

import org.locationtech.jts.geom.LineString;

import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraEntry;
import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.gtfs.GTFSStop;
import de.dlr.ivf.urmo.router.io.Utils;
import de.dlr.ivf.urmo.router.shapes.GeomHelper;

/**
 * @class DirectWriter
 * @brief Writes ODSingleResult results to a database / file
 * @author Daniel Krajzewicz
 */
public class DirectWriter extends BasicCombinedWriter {
	/// @brief Counter of results added to the database / file so far
	private int batchCount = 0;

	
	/**
	 * @brief Constructor
	 * 
	 * Opens the connection to a PostGIS database and builds the table
	 * @param format The used format
	 * @param inputParts The definition of the input/output source/destination
	 * @param precision The floating point precision to use
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @param epsg The EPSG to use
	 * @throws IOException When something fails
	 */
	public DirectWriter(Utils.Format format, String[] inputParts, int precision, boolean dropPrevious, int epsg) throws IOException {
		super(format, inputParts, "direct-output", precision, dropPrevious,
				"(fid bigint, sid bigint, edge text, line text, mode text, tt real, node text, idx integer)");
		addGeometryColumn("geom", epsg, "LINESTRING", 2);
	}
	
	
	/** @brief Returns the insert statement string
	 * @param[in] format The used output format
	 * @param[in] epsg The used projection
	 * @return The insert statement string
	 */
	protected String getInsertStatement(Utils.Format format, int epsg) {
		if(format==Utils.Format.FORMAT_POSTGRES) {
			return "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ST_GeomFromText(?, " + epsg + "))";
		}
		return "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	}


	/**
	 * @brief Writes the "direct" representation of the result
	 * @param originID The ID of the origin
	 * @param destinationID The ID of the destination
	 * @param destPath The path between the origin and the destination
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	public synchronized void writeResult(SingleODResult result, int beginTime) throws IOException {
		// revert order
		Vector<DijkstraEntry> entries = new Vector<>();
		DijkstraEntry c = result.path;
		do {
			entries.add(c);
			c = c.prev;
		} while(c!=null);
		Collections.reverse(entries);
		//
		long originID = result.origin.em.getOuterID();
		long destinationID = result.destination.em.getOuterID();
		// go through entries
		int index = 0;
		for(DijkstraEntry current : entries) {
			String id = Long.toString(current.n.getID());
			if(current.n instanceof GTFSStop) {
				id = ((GTFSStop) current.n).mid;
			}
			double ttt = current.ttt;
			LineString geom = current.e.getGeometry();
			if(entries.size()==1) {
				ttt = result.tt;
				double beg = 0;
				double end = 0;
				if(result.origin.edge==result.destination.edge) {
					beg = result.origin.pos;
					end = result.destination.pos;
					/*
					if(beg>end) {
						double tmp = beg;
						beg = end;
						end = tmp;
					}
					*/
				} else {
					beg = result.origin.edge.getLength() - result.origin.pos;
					end = result.destination.pos;
					geom = current.e.getOppositeEdge().getGeometry();
				}
				geom = GeomHelper.getSubGeom(geom, beg, end);
			} else if(current==entries.lastElement()) {
				double distOff = 0;
				if(current.wasOpposite) {
					distOff = (result.destination.edge.getLength() - result.destination.pos);
				} else {
					distOff = result.destination.pos;
				}
				ttt = current.ttt * distOff / result.destination.edge.getLength();
				geom = GeomHelper.getGeomUntilDistance(geom, distOff);
			} else if(current==entries.firstElement()) {
				//ttt = current.ttt * result.origin.pos / result.origin.edge.getLength();
				if(result.origin.edge!=current.e) {
					geom = GeomHelper.getGeomBehindDistance(geom, result.origin.edge.getLength() - result.origin.pos);
				} else {
					geom = GeomHelper.getGeomBehindDistance(geom, result.origin.pos);
				}
			}
			String routeID = getLineID(current.ptConnection);
			if (intoDB()) {
				try {
					_ps.setLong(1, originID);
					_ps.setLong(2, destinationID);
					_ps.setString(3, current.e.getID());
					_ps.setString(4, routeID);
					_ps.setString(5, current.usedMode.mml);
					_ps.setDouble(6, ttt);
					_ps.setString(7, id);
					_ps.setInt(8, index);
					_ps.setString(9, geom.toText());
					_ps.addBatch();
					++batchCount;
					if(batchCount>100) {
						_ps.executeBatch();
						_connection.commit();
						batchCount = 0;
					}
				} catch (SQLException ex) {
					throw new IOException(ex);
				}
			} else {
				_fileWriter.append(originID + ";" + destinationID + ";" 
						+ current.e.getID() + ";" + routeID + ";"
						+ current.usedMode.mml + ";"  
						+ String.format(Locale.US, _FS, ttt) + ";" + id + ";" + index + ";"
						+ geom.toText()
						+ "\n");
			}
			++index;
		}
		//}
		if (intoDB()) {
			try {
				_ps.executeBatch();
				_connection.commit();
			} catch (SQLException ex) {
				throw new IOException(ex);
			}	
		}
	}


}

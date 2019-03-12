/**
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * 
 * Copyright © 2016-2019 German Aerospace Center 
 * All rights reserved
 */
package de.dlr.ivf.urmo.router.output;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraEntry;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.gtfs.GTFSStop;
import de.dlr.ivf.urmo.router.shapes.DBEdge;

/**
 * @class DirectWriter
 * @brief Writes PTODSingleResult results to a database / file
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class DirectWriter extends BasicCombinedWriter {
	/// @brief Counter of results added to the database / file so far
	private int batchCount = 0;
	/// @brief A map of edges to assigned destinations
	HashMap<DBEdge, Vector<MapResult>> nearestToEdges;

	
	/**
	 * @brief Constructor
	 * 
	 * Opens the connection to a database and builds the table
	 * @param url The URL to the database
	 * @param tableName The name of the table
	 * @param user The name of the database user
	 * @param pw The password of the database user
	 * @param rsid The RSID to use
	 * @param _nearestToEdges A map of edges to assigned destinations
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @throws SQLException When something fails
	 */
	public DirectWriter(String url, String tableName, String user, String pw, int rsid, 
			HashMap<DBEdge, Vector<MapResult>> _nearestToEdges, boolean dropPrevious) throws SQLException {
		super(url, user, pw, tableName, "(fid bigint, sid bigint, line text, mode text, tt real, node text, idx integer)", "VALUES (?, ?, ?, ?, ?, ?, ?, ST_GeomFromText(?, " + rsid + "))", dropPrevious);
		addGeometryColumn("geom", rsid, "LINESTRING", 2);
		nearestToEdges = _nearestToEdges;
	}


	/**
	 * @brief Constructor
	 * 
	 * Opens the file to write the results to
	 * @param fileName The path to the file to write the results to
	 * @param rsid The RSID to use
	 * @param _nearestToEdges A map of edges to assigned destinations
	 * @throws IOException When something fails
	 */
	public DirectWriter(String fileName, int rsid, HashMap<DBEdge, Vector<MapResult>> _nearestToEdges) throws IOException {
		super(fileName);
	}
	

	/**
	 * @brief Writes the "direct" representation of the result
	 * @param result The result to write
	 * @param from The origin
	 * @param needsPT Whether only results that contain a public transport trip shall be written
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	public synchronized void writeResult(DijkstraResult result, MapResult from, boolean needsPT) throws SQLException, IOException {
		for(DBEdge e : result.edgeMap.keySet()) {
			DijkstraEntry toEdgeEntry = result.getEdgeInfo(e);
			if(!toEdgeEntry.matchesRequirements(needsPT)) {
				continue;
			}
			Vector<MapResult> toObjects = nearestToEdges.get(e);
			for(MapResult toObject : toObjects) {
				int index = 0;
				DijkstraEntry current = toEdgeEntry;
				do {
					String id = Long.toString(current.n.id);
					if(current.n instanceof GTFSStop) {
						id = ((GTFSStop) current.n).mid;
					}
					if (intoDB()) {
						ps.setLong(1, from.em.getOuterID());
						ps.setLong(2, toObject.em.getOuterID());
						ps.setString(3, current.line);
						ps.setString(4, current.usedMode.mml);
						ps.setDouble(5, current.ttt);
						ps.setString(6, id);
						ps.setInt(7, index);
						ps.setString(8, current.e.geom.toText());
						ps.addBatch();
						++batchCount;
						if(batchCount>100) {
							ps.executeBatch();
							connection.commit();
							batchCount = 0;
						}
					} else {
						fileWriter.append(from.em.getOuterID() + ";" + toObject.em.getOuterID() + ";" + current.line + ";" +
								current.usedMode.mml + ";" + current.ttt + ";" + id + ";" + index + "\n");
					}
					++index;
					current = current.prev;
				} while(current!=null);
			}
		}
		if (intoDB()) {
			ps.executeBatch();
			connection.commit();
		}
	}


}

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
package de.dlr.ivf.urmo.router.output.edge_use;

import java.io.IOException;
import java.sql.SQLException;

import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;
import de.dlr.ivf.urmo.router.output.edge_use.EUSingleResult.EdgeParam;

/**
 * @class EUWriter
 * @brief Writes EUSingleResult results to a database / file
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class EUWriter extends AbstractResultsWriter<EUSingleResult> {
	/// @brief Counter of results added to the database / file so far
	private int batchCount = 0;
	
	
	/**
	 * @brief Constructor
	 * 
	 * Opens the connection to a database and builds the table
	 * @param url The URL to the database
	 * @param tableName The name of the table
	 * @param user The name of the database user
	 * @param pw The password of the database user
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @throws SQLException When something fails
	 */
	public EUWriter(String url, String tableName, String user, String pw, boolean dropPrevious) throws SQLException {
		super(url, user, pw, tableName,
				"(fid bigint, sid bigint, eid text, num_walk bigint, num_car bigint, num_pt bigint, num_bike bigint)",
				"VALUES (?, ?, ?, ?, ?, ?, ?)", dropPrevious);
	}


	/**
	 * @brief Constructor
	 * 
	 * Opens the file to write the results to
	 * @param fileName The path to the file to write the results to
	 * @throws IOException When something fails
	 */
	public EUWriter(String fileName) throws IOException {
		super(fileName);
	}

	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	@Override
	public void writeResult(EUSingleResult result) throws SQLException, IOException {
		if (intoDB()) {
			for(String id : result.stats.keySet()) {
				EdgeParam ep = result.stats.get(id);
				ps.setLong(1, result.srcID);
				ps.setLong(2, result.destID);
				ps.setString(3, id);
				ps.setLong(4, ep.numWalk);
				ps.setLong(5, ep.numCar);
				ps.setLong(6, ep.numPT);
				ps.setLong(7, ep.numBike);
				ps.addBatch();
				++batchCount;
				if(batchCount>10000) {
					ps.executeBatch();
					batchCount = 0;
				}
			}
		} else {
			for(String id : result.stats.keySet()) {
				EdgeParam ep = result.stats.get(id);
				fileWriter.append(result.srcID + ";" + result.destID + ";" + id + ";" +
						ep.numWalk + ";" + ep.numCar + ";" + ep.numPT + ";" + ep.numBike + "\n");
			}
		}
	}

}

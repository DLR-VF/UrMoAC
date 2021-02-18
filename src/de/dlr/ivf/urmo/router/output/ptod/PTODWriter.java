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
package de.dlr.ivf.urmo.router.output.ptod;

import java.io.IOException;
import java.sql.SQLException;

import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;

/**
 * @class PTODWriter
 * @brief Writes PTODSingleResult results to a database / file
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class PTODWriter extends AbstractResultsWriter<PTODSingleResult> {
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
	public PTODWriter(String url, String tableName, String user, String pw, boolean dropPrevious) throws SQLException {
		super(url, user, pw, tableName,
				"(fid bigint, sid bigint, "
				+ "avg_access_distance real, avg_access_tt real, avg_egress_distance real, avg_egress_tt real, "
				+ "avg_interchange_distance real, avg_interchange_tt real, avg_pt_distance real, avg_pt_tt real, "
				+ "avg_num_interchanges real, avg_waiting_time real, avg_init_waiting_time real, avg_num real, avg_value real)",
				"VALUES (?, ?,  ?, ?, ?, ?,  ?, ?, ?, ?,  ?, ?, ?, ?, ?)", dropPrevious);
	}


	/**
	 * @brief Constructor
	 * 
	 * Opens the file to write the results to
	 * @param fileName The path to the file to write the results to
	 * @throws IOException When something fails
	 */
	public PTODWriter(String fileName) throws IOException {
		super(fileName);
	}

	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	@Override
	public void writeResult(PTODSingleResult result) throws SQLException, IOException {
		if (intoDB()) {
			ps.setLong(1, result.srcID);
			ps.setLong(2, result.destID);
			ps.setFloat(3, (float) result.weightedAccessDistance);
			ps.setFloat(4, (float) result.weightedAccessTravelTime);
			ps.setFloat(5, (float) result.weightedEgressDistance);
			ps.setFloat(6, (float) result.weightedEgressTravelTime);
			ps.setFloat(7, (float) result.weightedInterchangeDistance);
			ps.setFloat(8, (float) result.weightedInterchangeTravelTime);
			ps.setFloat(9, (float) result.weightedPTDistance);
			ps.setFloat(10, (float) result.weightedPTTravelTime);
			ps.setFloat(11, (float) result.weightedInterchangesNum);
			ps.setFloat(12, (float) result.weightedWaitingTime);
			ps.setFloat(13, (float) result.weightedInitialWaitingTime);
			ps.setFloat(14, (float) result.connectionsWeightSum);
			ps.setFloat(15, (float) result.weightedValue);
			ps.addBatch();
			++batchCount;
			if(batchCount>10000) {
				ps.executeBatch();
				batchCount = 0;
			}
		} else {
			fileWriter.append(result.srcID + ";" + result.destID + ";" 
					+ result.weightedAccessDistance + ";" + result.weightedAccessTravelTime + ";"
					+ result.weightedEgressDistance + ";" + result.weightedEgressTravelTime + ";"
					+ result.weightedInterchangeDistance + ";" + result.weightedInterchangeTravelTime + ";"
					+ result.weightedPTDistance + ";" + result.weightedPTTravelTime + ";" 
					+ result.weightedInterchangesNum + ";" + result.weightedWaitingTime + ";" 
					+ result.weightedInitialWaitingTime + ";"
					+ result.connectionsWeightSum + ";" + result.weightedValue + "\n");
		}
	}

}

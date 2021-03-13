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
package de.dlr.ivf.urmo.router.output.odext;

import java.io.IOException;
import java.sql.SQLException;

import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;

/**
 * @class ODExtendedWriter
 * @brief Writes ODSingleExtendedResult results to a database / file
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class ODExtendedWriter extends AbstractResultsWriter<ODSingleExtendedResult> {
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
	public ODExtendedWriter(String url, String tableName, String user, String pw, boolean dropPrevious) throws SQLException {
		super(url, user, pw, tableName,
				"(fid bigint, sid bigint, avg_distance real, avg_tt real, avg_v real, avg_num real, avg_value real, "
				+ "avg_kcal real, avg_price real, avg_co2 real, avg_interchanges real, avg_access real, avg_egress real, "
				+ "avg_waiting_time real, avg_init_waiting_time real, avg_pt_tt real, avg_pt_interchange_time real, modes text)",
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", dropPrevious);
	}


	/**
	 * @brief Constructor
	 * 
	 * Opens the file to write the results to
	 * @param fileName The path to the file to write the results to
	 * @throws IOException When something fails
	 */
	public ODExtendedWriter(String fileName) throws IOException {
		super(fileName);
	}

	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	@Override
	public void writeResult(ODSingleExtendedResult result) throws SQLException, IOException {
		if (intoDB()) {
			ps.setLong(1, result.srcID);
			ps.setLong(2, result.destID);
			ps.setFloat(3, (float) result.weightedDistance);
			ps.setFloat(4, (float) result.weightedTravelTime);
			ps.setFloat(5, (float) result.weightedSpeed);
			ps.setFloat(6, (float) result.connectionsWeightSum);
			ps.setFloat(7, (float) result.weightedValue);
			ps.setFloat(8, (float) result.weightedKCal);
			ps.setFloat(9, (float) result.weightedPrice);
			ps.setFloat(10, (float) result.weightedCO2);
			ps.setFloat(11, (float) result.weightedInterchanges);
			ps.setFloat(12, (float) result.weightedAccess);
			ps.setFloat(13, (float) result.weightedEgress);
			ps.setFloat(14, (float) result.weightedWaitingTime);
			ps.setFloat(15, (float) result.weightedInitialWaitingTime);
			ps.setFloat(16, (float) result.weightedPTTravelTime);
			ps.setFloat(17, (float) result.weightedInterchangeTime);
			ps.setString(18, result.lines.toString()); // modes
			ps.addBatch();
			++batchCount;
			if(batchCount>10000) {
				ps.executeBatch();
				batchCount = 0;
			}
		} else {
			fileWriter.append(result.srcID + ";" + result.destID + ";" 
					+ result.weightedDistance + ";" + result.weightedTravelTime + ";" + result.weightedSpeed + ";"
					+ result.connectionsWeightSum + ";" + result.weightedValue + ";" 
					+ result.weightedKCal + ";" + result.weightedPrice + ";" + result.weightedCO2 + ";"
					+ result.weightedInterchanges + ";" + result.weightedAccess + ";" + result.weightedEgress + ";" 
					+ result.weightedWaitingTime + ";" + result.weightedInitialWaitingTime + ";"
					+ result.weightedPTTravelTime + ";" + result.weightedInterchangeTime + ";" + result.lines.toString() + "\n");
		}
	}

}

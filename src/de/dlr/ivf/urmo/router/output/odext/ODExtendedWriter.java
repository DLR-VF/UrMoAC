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
 * Rutherfordstraße 2
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
	public ODExtendedWriter(String url, String tableName, String user, String pw, boolean dropPrevious) throws IOException {
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
	public void writeResult(ODSingleExtendedResult result) throws IOException {
		if (intoDB()) {
			try {
				_ps.setLong(1, result.srcID);
				_ps.setLong(2, result.destID);
				_ps.setFloat(3, (float) result.weightedDistance);
				_ps.setFloat(4, (float) result.weightedTravelTime);
				_ps.setFloat(5, (float) result.weightedSpeed);
				_ps.setFloat(6, (float) result.connectionsWeightSum);
				_ps.setFloat(7, (float) result.weightedValue);
				_ps.setFloat(8, (float) result.weightedKCal);
				_ps.setFloat(9, (float) result.weightedPrice);
				_ps.setFloat(10, (float) result.weightedCO2);
				_ps.setFloat(11, (float) result.weightedInterchanges);
				_ps.setFloat(12, (float) result.weightedAccess);
				_ps.setFloat(13, (float) result.weightedEgress);
				_ps.setFloat(14, (float) result.weightedWaitingTime);
				_ps.setFloat(15, (float) result.weightedInitialWaitingTime);
				_ps.setFloat(16, (float) result.weightedPTTravelTime);
				_ps.setFloat(17, (float) result.weightedInterchangeTime);
				_ps.setString(18, result.lines.toString()); // modes
				_ps.addBatch();
				++batchCount;
				if(batchCount>10000) {
					_ps.executeBatch();
					batchCount = 0;
				}
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		} else {
			_fileWriter.append(result.srcID + ";" + result.destID + ";" 
					+ result.weightedDistance + ";" + result.weightedTravelTime + ";" + result.weightedSpeed + ";"
					+ result.connectionsWeightSum + ";" + result.weightedValue + ";" 
					+ result.weightedKCal + ";" + result.weightedPrice + ";" + result.weightedCO2 + ";"
					+ result.weightedInterchanges + ";" + result.weightedAccess + ";" + result.weightedEgress + ";" 
					+ result.weightedWaitingTime + ";" + result.weightedInitialWaitingTime + ";"
					+ result.weightedPTTravelTime + ";" + result.weightedInterchangeTime + ";" + result.lines.toString() + "\n");
		}
	}

}

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
package de.dlr.ivf.urmo.router.output.od;

import java.io.IOException;
import java.sql.SQLException;

import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;

/**
 * @class ODWriter
 * @brief Writes ODSingleResult results to a database / file
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class ODWriter extends AbstractResultsWriter<ODSingleResult> {
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
	public ODWriter(String url, String tableName, String user, String pw, boolean dropPrevious) throws SQLException {
		super(url, user, pw, tableName,
				"(fid bigint, sid bigint, avg_distance real, avg_tt real, avg_num real, avg_value real)",
				"VALUES (?, ?, ?, ?, ?, ?)", dropPrevious);
	}


	/**
	 * @brief Constructor
	 * 
	 * Opens the file to write the results to
	 * @param fileName The path to the file to write the results to
	 * @throws IOException When something fails
	 */
	public ODWriter(String fileName) throws IOException {
		super(fileName);
	}

	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	@Override
	public void writeResult(ODSingleResult result) throws SQLException, IOException {
		if (intoDB()) {
			_ps.setLong(1, result.srcID);
			_ps.setLong(2, result.destID);
			_ps.setFloat(3, (float) result.weightedDistance);
			_ps.setFloat(4, (float) result.weightedTravelTime);
			_ps.setFloat(5, (float) result.connectionsWeightSum);
			_ps.setFloat(6, (float) result.weightedValue);
			_ps.addBatch();
			++batchCount;
			if(batchCount>10000) {
				_ps.executeBatch();
				batchCount = 0;
			}
		} else {
			_fileWriter.append(result.srcID + ";" + result.destID + ";" 
					+ result.weightedDistance + ";" + result.weightedTravelTime + ";"
					+ result.connectionsWeightSum + ";" + result.weightedValue + "\n");
		}
	}

}

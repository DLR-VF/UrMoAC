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
package de.dlr.ivf.urmo.router.output.interchanges;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;
import de.dlr.ivf.urmo.router.output.interchanges.InterchangeSingleResult.InterchangeParam;

/**
 * @class InterchangeWriter
 * @brief Writes InterchangeSingleResult results to a database / file
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class InterchangeWriter extends AbstractResultsWriter<InterchangeSingleResult> {
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
	public InterchangeWriter(String url, String tableName, String user, String pw, boolean dropPrevious) throws SQLException {
		super(url, user, pw, tableName,
				"(fid bigint, sid bigint, halt text, line_from text, line_to text, num bigint, tt real)",
				"VALUES (?, ?, ?, ?, ?, ?, ?)", dropPrevious);
	}


	/**
	 * @brief Constructor
	 * 
	 * Opens the file to write the results to
	 * @param fileName The path to the file to write the results to
	 * @throws IOException When something fails
	 */
	public InterchangeWriter(String fileName) throws IOException {
		super(fileName);
	}

	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	@Override
	public void writeResult(InterchangeSingleResult result) throws SQLException, IOException {
		if (intoDB()) {
			for(String id : result.stats.keySet()) {
				Map<String, InterchangeParam> ssstats = result.stats.get(id);
				for(String id2 : ssstats.keySet()) {
					String[] lineIDs = InterchangeSingleResult.splitLinesKey(id2);
					_ps.setLong(1, result.srcID);
					_ps.setLong(2, result.destID);
					_ps.setString(3, id);
					_ps.setString(4, lineIDs[0]);
					_ps.setString(5, lineIDs[1]);
					_ps.setLong(6, ssstats.get(id2).number);
					_ps.setFloat(7, (float) ssstats.get(id2).weightedTT);
					_ps.addBatch();
					++batchCount;
				}
			}
			if(batchCount>10000) {
				_ps.executeBatch();
				batchCount = 0;
			}
		} else {
			for(String id : result.stats.keySet()) {
				Map<String, InterchangeParam> ssstats = result.stats.get(id);
				for(String id2 : ssstats.keySet()) {
					String[] lineIDs = InterchangeSingleResult.splitLinesKey(id2);
					_fileWriter.append(result.srcID + ";" + result.destID + ";" + id + ";" + 
							lineIDs[0] + ";" + lineIDs[1] + ";" + ssstats.get(id2).number + ";" + ssstats.get(id2).weightedTT + "\n");
				}
			}
			
		}
	}

}

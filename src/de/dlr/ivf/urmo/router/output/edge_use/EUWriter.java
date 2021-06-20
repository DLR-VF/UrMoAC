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
	public EUWriter(String url, String tableName, String user, String pw, boolean dropPrevious) throws IOException {
		super(url, user, pw, tableName,
				"(fid bigint, sid bigint, eid text, num real, srcweight real, normed real)", "VALUES (?, ?, ?, ?, ?, ?)", dropPrevious);
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
	public void writeResult(EUSingleResult result) throws IOException {
		if (intoDB()) {
			try {
				for(String id : result.stats.keySet()) {
					EdgeParam ep = result.stats.get(id);
						_ps.setLong(1, result.srcID);
						_ps.setLong(2, result.destID);
						_ps.setString(3, id);
						_ps.setFloat(4, (float) ep.num);
						_ps.setFloat(5, (float) ep.sourcesWeight);
						_ps.setFloat(6, (float) (ep.num / ep.sourcesWeight));
						_ps.addBatch();
						++batchCount;
						if(batchCount>10000) {
							_ps.executeBatch();
							batchCount = 0;
						}
				}
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		} else {
			for(String id : result.stats.keySet()) {
				EdgeParam ep = result.stats.get(id);
				_fileWriter.append(result.srcID + ";" + result.destID + ";" + id + ";" + ep.num 
						+ ";" + ep.sourcesWeight + ";" + (ep.num / ep.sourcesWeight) + "\n");
			}
		}
	}

}

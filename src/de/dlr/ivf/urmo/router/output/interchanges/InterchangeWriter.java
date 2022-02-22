/*
 * Copyright (c) 2016-2022 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
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
import java.util.Locale;
import java.util.Map;

import de.dlr.ivf.urmo.router.io.Utils;
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
	 * Opens the connection to a PostGIS database and builds the table
	 * @param format The used format
	 * @param inputParts The definition of the input/output source/destination
	 * @param precision The floating point precision to use
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @throws IOException When something fails
	 */
	public InterchangeWriter(Utils.Format format, String[] inputParts, int precision, boolean dropPrevious) throws IOException {
		super(format, inputParts, "interchanges-output", precision, dropPrevious, 
				"(fid bigint, sid bigint, halt text, line_from text, line_to text, num bigint, tt real)");
	}


	/** @brief Get the insert statement string
	 * @param[in] format The used output format
	 * @param[in] rsid The used projection
	 * @return The insert statement string
	 */
	protected String getInsertStatement(Utils.Format format, int rsid) {
		return "VALUES (?, ?, ?, ?, ?, ?, ?)";
	}

	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @throws IOException When something fails
	 */
	@Override
	public void writeResult(InterchangeSingleResult result) throws IOException {
		if (intoDB()) {
			try {
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
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		} else {
			for(String id : result.stats.keySet()) {
				Map<String, InterchangeParam> ssstats = result.stats.get(id);
				for(String id2 : ssstats.keySet()) {
					String[] lineIDs = InterchangeSingleResult.splitLinesKey(id2);
					_fileWriter.append(result.srcID + ";" + result.destID + ";" + id + ";" + 
							lineIDs[0] + ";" + lineIDs[1] + ";" 
							+ ssstats.get(id2).number + ";" 
							+ String.format(Locale.US, _FS, ssstats.get(id2).weightedTT) + "\n");
				}
			}
			
		}
	}

}

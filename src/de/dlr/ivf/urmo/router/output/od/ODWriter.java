/*
 * Copyright (c) 2017-2025
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
package de.dlr.ivf.urmo.router.output.od;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

import de.dlr.ivf.urmo.router.io.Utils;
import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;

/**
 * @class ODWriter
 * @brief Writes ODSingleResult results to a database / file
 * @author Daniel Krajzewicz
 */
public class ODWriter extends AbstractResultsWriter<ODSingleResult> {
	/// @brief Counter of results added to the database / file so far
	private int batchCount = 0;
		
	
	/**
	 * @brief Constructor
	 * 
	 * Opens the connection to a PostGIS database and builds the table
	 * @param format The used format
	 * @param inputParts The definition of the input/output origin/destination
	 * @param precision The floating point precision to use
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @param haveTypes Whether destinations may have different types 
	 * @throws IOException When something fails
	 */
	public ODWriter(Utils.Format format, String[] inputParts, int precision, boolean dropPrevious, boolean haveTypes) throws IOException {
		super(format, inputParts, "od-output", precision, dropPrevious, haveTypes,
				"(fid bigint, sid bigint, avg_distance real, avg_tt real, avg_num real, avg_value real)");
	}


	/** @brief Get the insert statement string
	 * @param format The used output format
	 * @param epsg The used projection
	 * @return The insert statement string
	 */
	protected String getInsertStatement(Utils.Format format, int epsg) {
		return "VALUES (?, ?, ?, ?, ?, ?)";
	}


	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @param destType The type of the destination
	 * @throws IOException When something fails
	 */
	@Override
	public void writeResult(ODSingleResult result, String destType) throws IOException {
		if (intoDB()) {
			try {
				_ps.setLong(1, result.originID);
				_ps.setLong(2, result.destID);
				_ps.setFloat(3, (float) result.weightedDistance);
				_ps.setFloat(4, (float) result.weightedTravelTime);
				_ps.setFloat(5, (float) result.connectionsWeightSum);
				_ps.setFloat(6, (float) result.weightedValue);
				if(_haveTypes) {
					_ps.setString(7, destType);
				}
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
			_fileWriter.append(Long.toString(result.originID)).append(";").append(Long.toString(result.destID)).append(";");
			_fileWriter.append(String.format(Locale.US, _FS, result.weightedDistance)).append(";"); 
			_fileWriter.append(String.format(Locale.US, _FS, result.weightedTravelTime)).append(";");
			_fileWriter.append(String.format(Locale.US, _FS, result.connectionsWeightSum)).append(";");
			_fileWriter.append(String.format(Locale.US, _FS, result.weightedValue));
			if(_haveTypes) {
				_fileWriter.append(";").append(destType);
			}
			_fileWriter.append("\n");
		}
	}

}

/*
 * Copyright (c) 2017-2024
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
package de.dlr.ivf.urmo.router.output.odext;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

import de.dlr.ivf.urmo.router.io.Utils;
import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;

/**
 * @class ODExtendedWriter
 * @brief Writes ODSingleExtendedResult results to a database / file
 * @author Daniel Krajzewicz
 */
public class ODExtendedWriter extends AbstractResultsWriter<ODSingleExtendedResult> {
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
	public ODExtendedWriter(Utils.Format format, String[] inputParts, int precision, boolean dropPrevious) throws IOException {
		super(format, inputParts, "ext-od-output", precision, dropPrevious, 
				"(fid bigint, sid bigint, avg_distance real, avg_tt real, avg_num real, avg_value real, "
				+ "avg_kcal real, avg_price real, avg_co2 real, avg_interchanges real, avg_access real, avg_egress real, "
				+ "avg_waiting_time real, avg_init_waiting_time real, avg_pt_tt real, avg_pt_interchange_time real, modes text)");
	}


	/** @brief Get the insert statement string
	 * @param[in] format The used output format
	 * @param[in] epsg The used projection
	 * @return The insert statement string
	 */
	protected String getInsertStatement(Utils.Format format, int epsg) {
		return "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	}
	
	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
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
				_ps.setFloat(5, (float) result.connectionsWeightSum);
				_ps.setFloat(6, (float) result.weightedValue);
				_ps.setFloat(7, (float) result.weightedKCal);
				_ps.setFloat(8, (float) result.weightedPrice);
				_ps.setFloat(9, (float) result.weightedCO2);
				_ps.setFloat(10, (float) result.weightedInterchanges);
				_ps.setFloat(11, (float) result.weightedAccess);
				_ps.setFloat(12, (float) result.weightedEgress);
				_ps.setFloat(13, (float) result.weightedWaitingTime);
				_ps.setFloat(14, (float) result.weightedInitialWaitingTime);
				_ps.setFloat(15, (float) result.weightedPTTravelTime);
				_ps.setFloat(16, (float) result.weightedInterchangeTime);
				_ps.setString(17, result.lines.toString()); // modes
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
					+ String.format(Locale.US, _FS, result.weightedDistance) + ";" 
					+ String.format(Locale.US, _FS, result.weightedTravelTime) + ";" 
					+ String.format(Locale.US, _FS, result.connectionsWeightSum) + ";" 
					+ String.format(Locale.US, _FS, result.weightedValue) + ";" 
					+ String.format(Locale.US, _FS, result.weightedKCal) + ";" 
					+ String.format(Locale.US, _FS, result.weightedPrice) + ";" 
					+ String.format(Locale.US, _FS, result.weightedCO2) + ";"
					+ String.format(Locale.US, _FS, result.weightedInterchanges) + ";" 
					+ String.format(Locale.US, _FS, result.weightedAccess) + ";" 
					+ String.format(Locale.US, _FS, result.weightedEgress) + ";" 
					+ String.format(Locale.US, _FS, result.weightedWaitingTime) + ";" 
					+ String.format(Locale.US, _FS, result.weightedInitialWaitingTime) + ";"
					+ String.format(Locale.US, _FS, result.weightedPTTravelTime) + ";" 
					+ String.format(Locale.US, _FS, result.weightedInterchangeTime) + ";" 
					+ result.lines.toString() + "\n");
		}
	}

}

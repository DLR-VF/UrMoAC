/*
 * Copyright (c) 2024-2024
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
package de.dlr.ivf.urmo.router.output;

import java.io.IOException;
import java.sql.SQLException;

import de.dlr.ivf.urmo.router.io.Utils;

/**
 * @class CrossingTimesWriter
 * @brief Writes network errors to a database / file
 * @author Daniel Krajzewicz
 */
public class CrossingTimesWriter extends BasicCombinedWriter {
	/**
	 * @brief Constructor
	 * 
	 * Opens output device and, in case of using a database, generates the table
	 * @param format The used format
	 * @param inputParts The definition of the input/output origin/destination
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @throws IOException When something fails
	 */
	public CrossingTimesWriter(Utils.Format format, String[] inputParts, boolean dropPrevious) throws IOException {
		super(format, inputParts, "crossing-times", 1, dropPrevious, false, "(from_edge text, to_edge text, delay real)");
	}
	

	/** @brief Returns the insert statement string
	 * @param[in] format The used output format
	 * @param[in] epsg The used projection
	 * @return The insert statement string
	 */
	protected String getInsertStatement(Utils.Format format, int epsg) {
		return "VALUES (?, ?, ?)";
	}
	

	/**
	 * @brief Writes the delay to get from from_edge to to_edge
	 * @param from The origin edge
	 * @param to The destination edge
	 * @param value The value (delay time)
	 * @throws IOException When something fails
	 */
	public void writeCrossingTime(String from, String to, double value) throws IOException {
		if (intoDB()) {
			try {
				_ps.setString(1, from);
				_ps.setString(2, to);
				_ps.setDouble(3, value);
				_ps.addBatch();
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		} else {
			_fileWriter.append(from + ";" + to + ";" + value + "\n");
		}
	}
	
	
	
}

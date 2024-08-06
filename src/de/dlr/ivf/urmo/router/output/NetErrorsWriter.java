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
 * @class NetErrorsWriter
 * @brief Writes network errors to a database / file
 * @author Daniel Krajzewicz
 */
public class NetErrorsWriter extends BasicCombinedWriter {
	/**
	 * @brief Constructor
	 * 
	 * Opens output device and, in case of using a database, generates the table
	 * @param format The used format
	 * @param inputParts The definition of the input/output origin/destination
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @throws IOException When something fails
	 */
	public NetErrorsWriter(Utils.Format format, String[] inputParts, boolean dropPrevious) throws IOException {
		super(format, inputParts, "net-errors", 1, dropPrevious, "(description text, edge1 text, edge2 text)");
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
	 * @brief Writes the information about an edge with a duplicate ID
	 * @param edge The duplicate edge ID 
	 * @throws IOException When something fails
	 */
	public void writeDuplicate(String edge) throws IOException {
		if (intoDB()) {
			try {
				_ps.setString(1, "duplicate id");
				_ps.setString(2, edge);
				_ps.setString(3, null);
				_ps.addBatch();
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		} else {
			_fileWriter.append("duplicate id;" + edge + "\n");
		}
	}
	
	
	/**
	 * @brief Writes the information about an edge with an allowed speed of 0 or below
	 * @param edge The ID of the edge with the too low speed
	 * @throws IOException When something fails
	 */
	public void writeNoSpeed(String edge) throws IOException {
		if (intoDB()) {
			try {
				_ps.setString(1, "speed<=0");
				_ps.setString(2, edge);
				_ps.setString(3, null);
				_ps.addBatch();
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		} else {
			_fileWriter.append("speed<=0;" + edge + "\n");
		}
	}
	
	
	/**
	 * @brief Writes the information about an edge with a length of 0 or below
	 * @param edge The ID of the edge with the too low length
	 * @throws IOException When something fails
	 */
	public void writeNoLength(String edge) throws IOException {
		if (intoDB()) {
			try {
				_ps.setString(1, "length<=0");
				_ps.setString(2, edge);
				_ps.setString(3, null);
				_ps.addBatch();
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		} else {
			_fileWriter.append("length<=0;" + edge + "\n");
		}
	}
	
	
	/**
	 * @brief Writes the information about an edge that was replaced
	 * @param edge1 The ID of the original edge
	 * @param edge2 The ID of the removed edge
	 * @throws IOException When something fails
	 */
	public void writeEdgeReplacement(String edge1, String edge2) throws IOException {
		if (intoDB()) {
			try {
				_ps.setString(1, "replaced duplicate");
				_ps.setString(2, edge1);
				_ps.setString(3, edge2);
				_ps.addBatch();
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		} else {
			_fileWriter.append("replaced duplicate;" + edge1 + ";" + edge2 + "\n");
		}
	}
	
}

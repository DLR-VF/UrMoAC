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
package de.dlr.ivf.urmo.router.output;

import java.io.IOException;

import de.dlr.ivf.urmo.router.io.Utils;

/**
 * @class AbstractResultsWriter
 * @brief Class with interfaces for writing results of different type to databases or files 
 * @author Daniel Krajzewicz
 * @param <T>
 */
public abstract class AbstractResultsWriter<T> extends BasicCombinedWriter {
	/**
	 * @brief Constructor
	 * 
	 * Opens the connection to a PostGIS database and builds the table
	 * @param format The used format
	 * @param inputParts The definition of the input/output origin/destination
	 * @param fileType The name of the input/output (option name)
	 * @param precision The floating point precision to use
	 * @param dropPrevious Whether a previous table with the name shall be dropped
	 * @param haveTypes Whether destination have different types 
	 * @param tableDef The definition of the database table 
	 * @throws IOException When something fails
	 */
	public AbstractResultsWriter(Utils.Format format, String[] inputParts, String fileType, int precision, 
			boolean dropPrevious, boolean haveTypes, String tableDef) throws IOException {
		super(format, inputParts, fileType, precision, dropPrevious, haveTypes, tableDef);
	}

	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @param destType The type of the destination
	 * @throws IOException When something fails
	 */
	public abstract void writeResult(T result, String destType) throws IOException;

}

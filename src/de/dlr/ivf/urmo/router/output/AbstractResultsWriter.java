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
package de.dlr.ivf.urmo.router.output;

import java.io.IOException;
import java.sql.SQLException;

import de.dlr.ivf.urmo.router.io.Utils;

/**
 * @class AbstractResultsWriter
 * @brief Class with interfaces for writing results of different type to databases or files 
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 * @param <T>
 */
public abstract class AbstractResultsWriter<T> extends BasicCombinedWriter {
	/**
	 * @brief Constructor
	 * 
	 * Opens the connection to a PostGIS database and builds the table
	 * @param format The used format
	 * @param inputParts The definition of the input/output source/destination
	 * @param fileType The name of the input/output (option name)
	 * @param precision The floating point precision to use
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @param tableDef The definition of the database table 
	 * @throws IOException When something fails
	 */
	public AbstractResultsWriter(Utils.Format format, String[] inputParts, String fileType, int precision, 
			boolean dropPrevious, String tableDef) throws IOException {
		super(format, inputParts, fileType, precision, dropPrevious, tableDef);
	}

	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @throws IOException When something fails
	 */
	public abstract void writeResult(T result) throws IOException;

}

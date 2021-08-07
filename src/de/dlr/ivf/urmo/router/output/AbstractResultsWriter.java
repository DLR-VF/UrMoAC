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
	 * Opens the connection to a database and builds the table
	 * @param url The URL to the database
	 * @param user The name of the database user
	 * @param pw The password of the database user
	 * @param _tableName The name of the table
	 * @param tableDef The definition of the table
	 * @param insertStmt The insert statement to use
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @throws SQLException When something fails
	 */
	public AbstractResultsWriter(String url, String user, String pw, String _tableName, String tableDef, 
			String insertStmt, boolean dropPrevious) throws IOException {
		super(url, user, pw, _tableName, tableDef, insertStmt, dropPrevious);
	}


	/**
	 * @brief Constructor
	 * 
	 * Opens the file to write the results to
	 * @param fileName The path to the file to write the results to
	 * @param precision The precision to use
	 * @throws IOException When something fails
	 */
	public AbstractResultsWriter(String fileName, int precision) throws IOException {
		super(fileName, precision);
	}

	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	public abstract void writeResult(T result) throws IOException;

}

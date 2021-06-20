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

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.postgresql.PGConnection;

/**
 * @class BasicCombinedWriter
 * @brief Base class for an output that writes to a database or a file
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class BasicCombinedWriter {
	/// @{ db connection settings
	/// @brief The connection to the database
	protected Connection _connection = null;
	/// @brief The insert statement to use
	protected PreparedStatement _ps = null;
	/// @brief The name of the table to write to
	protected String _tableName = null;
	/// @}

	/// @{ file settings
	/// @brief The writer to use to write to the file
	protected FileWriter _fileWriter = null;
	/// @}
	
	
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
	public BasicCombinedWriter(String url, String user, String pw, String tableName, String tableDef, 
			String insertStmt, boolean dropPrevious) throws IOException {
		try {
			_tableName = tableName;
			_connection = DriverManager.getConnection(url, user, pw);
			_connection.setAutoCommit(true);
			_connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			((PGConnection) _connection).addDataType("geometry", org.postgis.PGgeometry.class);
			if(dropPrevious) {
				String sql = "DROP TABLE IF EXISTS " + _tableName + ";";
				_connection.createStatement().executeUpdate(sql);
			}
			String sql = "CREATE TABLE " + _tableName + " " + tableDef + ";";
			Statement s = _connection.createStatement();
			s.executeUpdate(sql);
			_connection.setAutoCommit(false);
			_ps = _connection.prepareStatement("INSERT INTO " + _tableName + " " + insertStmt + ";");
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}


	/**
	 * @brief Constructor
	 * 
	 * Opens the file to write the results to
	 * @param fileName The path to the file to write the results to
	 * @throws IOException When something fails
	 */
	public BasicCombinedWriter(String fileName) throws IOException {
		_fileWriter = new FileWriter(fileName);
	}


	/**
	 * @brief Whether this writer writes into a database
	 * @return Whether a database is used as output destination
	 */
	protected boolean intoDB() {
		return _connection != null;
	}


	/**
	 * @brief Closes the writing process
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	public synchronized void close() throws IOException {
		if (intoDB()) {
			try {
				_ps.executeBatch();
				_connection.commit();
				_connection.close();
			} catch (SQLException e) {
				throw new IOException(e);
			}
		} else {
			_fileWriter.close();
		}
	}


	/**
	 * @brief Adds a comment (if it's a database connection)
	 * @param comment The comment to add
	 * @throws SQLException When something fails
	 */
	public void addComment(String comment) throws IOException {
		if (intoDB()) {
			String sql = "COMMENT ON TABLE " + _tableName + " IS '" + comment + "';";
			try {
				Statement s = _connection.createStatement();
				s.executeUpdate(sql);
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		}
	}

	
	/**
	 * @brief Adds a geometry column
	 * @param name The name of the column
	 * @param rsid The RSID to use for projection
	 * @param geomType The geometry type to use
	 * @param numDim The number of dimensions of this geometry
	 * @throws SQLException When something fails
	 */
	protected void addGeometryColumn(String name, int rsid, String geomType, int numDim) throws IOException {
		String[] d = _tableName.split("\\.");
		try {
			_connection.createStatement().executeQuery("SELECT AddGeometryColumn('" + d[0] + "', '" + d[1] + "', '" + name
					+ "', " + rsid + ", '" + geomType + "', " + numDim + ");");
			_connection.commit();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}


	/**
	 * @brief Flushes the results added so far to the database / file
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	protected synchronized void flush() throws IOException {
		if (intoDB()) {
			try {
				_ps.executeBatch();
				_connection.commit();
			} catch (SQLException e) {
				throw new IOException(e);
			}
		} else {
			_fileWriter.flush();
		}
	}

}

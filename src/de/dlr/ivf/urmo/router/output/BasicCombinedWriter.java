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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.postgresql.PGConnection;

import de.dlr.ivf.urmo.router.gtfs.GTFSConnection;
import de.dlr.ivf.urmo.router.io.Utils;

/**
 * @class BasicCombinedWriter
 * @brief Base class for an output that writes to a database or a file
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public abstract class BasicCombinedWriter {
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
	/// @brief The floating point precision format string to use
	protected String _FS;
	/// @}
	
	/// @brief The file format
	Utils.Format _format = Utils.Format.FORMAT_UNKNOWN;

	/// @brief Whether comments are supported
	boolean _allowsComments = false;

	
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
	public BasicCombinedWriter(Utils.Format format, String[] inputParts, String fileType, int precision, 
			boolean dropPrevious, String tableDef) throws IOException {
		_format = format;
		switch(format) {
		case FORMAT_POSTGRES:
		case FORMAT_SQLITE:
			try {
				_tableName = Utils.getTableName(format, inputParts, fileType);
				_connection = Utils.getConnection(format, inputParts, fileType);
				_connection.setAutoCommit(true);
				_connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
				if(format==Utils.Format.FORMAT_POSTGRES) {
					((PGConnection) _connection).addDataType("geometry", org.postgis.PGgeometry.class);
				}
				if(dropPrevious) {
					String sql = "DROP TABLE IF EXISTS " + _tableName + ";";
					_connection.createStatement().executeUpdate(sql);
				}
				String sql = "CREATE TABLE " + _tableName + " " + tableDef + ";";
				Statement s = _connection.createStatement();
				s.executeUpdate(sql);
				_connection.setAutoCommit(false);
				if(format==Utils.Format.FORMAT_POSTGRES) {
					_allowsComments = true;
				}
			} catch (SQLException e) {
				throw new IOException(e);
			}
			break;
		case FORMAT_CSV:
		case FORMAT_SHAPEFILE:
		case FORMAT_GEOPACKAGE:
		case FORMAT_SUMO:
			_fileWriter = new FileWriter(inputParts[0]);
			_FS = "%." + precision + "f";
			break;
		case FORMAT_UNKNOWN:
		default:
			throw new IOException("Unknown format for output '" + fileType + "'.");
		}
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
	 * @throws IOException When something fails
	 */
	public void addComment(String comment) throws IOException {
		if (_allowsComments) {
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
	 * @throws IOException When something fails
	 */
	protected void addGeometryColumn(String name, int rsid, String geomType, int numDim) throws IOException {
		try {
			if(_format==Utils.Format.FORMAT_POSTGRES) {
				String[] d = _tableName.split("\\.");
				_connection.createStatement().executeQuery("SELECT AddGeometryColumn('" + d[0] + "', '" + d[1] + "', '" + name
						+ "', " + rsid + ", '" + geomType + "', " + numDim + ");");
				_connection.commit();
			} else if(_format==Utils.Format.FORMAT_SQLITE) {
				_connection.createStatement().executeUpdate("ALTER TABLE " + _tableName + " ADD COLUMN " + name + " text;");
				_connection.commit();
			}
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
	
	
	/** @brief Prepare the insert statement
	 * @param[in] rsid The used projection
	 * @throws IOException When something fails
	 */
	public void createInsertStatement(int rsid) throws IOException {
		if(_format!=Utils.Format.FORMAT_POSTGRES && _format!=Utils.Format.FORMAT_SQLITE) {
			return;
		}
		try {
			_ps = _connection.prepareStatement("INSERT INTO " + _tableName + " " + getInsertStatement(_format, rsid) + ";");
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
	

	/** @brief Get the insert statement string
	 * @param[in] format The used output format
	 * @param[in] rsid The used projection
	 * @return The insert statement string
	 */
	protected abstract String getInsertStatement(Utils.Format format, int rsid);  

	
	/**
	 * @brief Flushes the results added so far to the database / file
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
	
	
	/**
	 * @brief Helper method for getting a route id, if given
	 * @param[in] c The GTFSConnection that represents a pt ride
	 * @return The id of the route or an empty string if the connection is null
	 */
	String getLineID(GTFSConnection c) {
		return c!=null ? c.trip.route.id : "";
	}

}

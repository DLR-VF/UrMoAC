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
package de.dlr.ivf.urmo.router.io;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** @class Utils
 * @brief Some helper methods for output building
 */
public class Utils {
	/** @brief Known file formats / database connections
	 */
	public enum Format {
		/// @brief PostgreSQL database connection
		FORMAT_POSTGRES,
		/// @brief SQLite database connection
		FORMAT_SQLITE,
		/// @brief csv files
		FORMAT_CSV,
		/// @brief csv files with well known text representations
		FORMAT_WKT,
		/// @brief shapefilesfiles
		FORMAT_SHAPEFILE,
		/// @brief geopackage files
		FORMAT_GEOPACKAGE,
		/// @brief SUMO files
		FORMAT_SUMO,
		/// @brief unknown format
		FORMAT_UNKNOWN
	}
	
	
	/** @brief Checks the input/output definition and returns it split to it's sub-parts
	 * @param format The recognized format
	 * @param input The definition of the data source / data destination
	 * @param name The name of the option for reporting purposes
	 * @return The parsed input/output source/destination definition
	 * @throws IOException If the definition is wrong
	 */
	public static String[] getParts(Format format, String input, String name) throws IOException {
		if(input.startsWith("db;") || input.startsWith("file;") || input.startsWith("csv;") || input.startsWith("wkt;")
				|| input.startsWith("shp;") || input.startsWith("gpkg;") || input.startsWith("sumo;")) {
			String prefix = input.substring(0, input.indexOf(";")+1);
			input = input.substring(input.indexOf(";")+1);
			System.err.println("Deprecation warning: the prefix '" + prefix + "' used for option '" + name + "' is no longer needed");
		}
		String[] parsed = input.split(";");
		switch(format) {
		case FORMAT_POSTGRES:
			if(parsed.length==4) {
				return parsed;
			}
			throw new IOException("False Postgres database definition for option '" + name + "'\n"
					+ " should be 'jdbc:postgresql:<DB_HOST>;<SCHEMA.TABLE>;<USER>;<PASSWORD>'\n"
					+ " is '" + input + "'");
		case FORMAT_SQLITE:
			if(parsed.length==2) {
				return parsed;
			}
			throw new IOException("False SQLite database definition for option '" + name + "'\n"
					+ " should be 'jdbc:sqlite:<DB_FILE>;<TABLE>'\n"
					+ " is '" + input + "'");
		case FORMAT_CSV:
		case FORMAT_WKT:
		case FORMAT_SHAPEFILE:
		case FORMAT_GEOPACKAGE:
		case FORMAT_SUMO:
			if(parsed.length==1) {
				return parsed;
			}
			throw new IOException("False file definition for option '" + name + "'\n"
					+ " should be '<FILE>'\n"
					+ " is '" + input + "'");
		case FORMAT_UNKNOWN:
		default:
			throw new IOException("Could not determine format for option '" + name + "' ('" + input + "').");
		}
	}
	
	
	/** @brief Returns a human-readable name of the given format
	 * @param format The format to return the name of
	 * @return The human readable name of the format
	 * @throws IOException If the format is not know
	 */
	public static String getFormatMMLName(Format format) throws IOException {
		switch(format) {
		case FORMAT_POSTGRES:
			return "Postgres database";
		case FORMAT_SQLITE:
			return "SQLite database";
		case FORMAT_CSV:
			return "csv";
		case FORMAT_WKT:
			return "wkt";
		case FORMAT_SHAPEFILE:
			return "shp";
		case FORMAT_GEOPACKAGE:
			return "gpkg";
		case FORMAT_SUMO:
			return "simo";
		default:
			throw new IOException("Format '" + getFormatMMLName(format) + "' is not known.");
		}
	}


	/** @brief Determine and return the format of the given input/output definition
	 * @param input The definition of the input/output source/destination
	 * @return The determined format
	 */
	public static Format getFormat(String input) {
		// old-style definition
		if(input.startsWith("db;jdbc:postgresql:") || input.startsWith("jdbc:postgresql:")) {
			return Format.FORMAT_POSTGRES;
		} else if(input.startsWith("db;jdbc:sqlite:") || input.startsWith("jdbc:sqlite:")) {
			return Format.FORMAT_SQLITE;
		} else if(input.startsWith("file;") || input.startsWith("csv;")) {
			return Format.FORMAT_CSV;
		} else if(input.startsWith("wkt;")) {
			return Format.FORMAT_WKT;
		} else if(input.startsWith("shp;")) {
			return Format.FORMAT_SHAPEFILE;
		} else if(input.startsWith("gpkg;")) {
			return Format.FORMAT_GEOPACKAGE;
		} else if(input.startsWith("sumo;")) {
			return Format.FORMAT_SUMO;
		}   
		// new-style definition
		if(input.startsWith("jdbc:postgresql:")) {
			return Format.FORMAT_POSTGRES;
		} else if(input.startsWith("jdbc:sqlite:")) {
			return Format.FORMAT_SQLITE;
		} else if(input.endsWith(".csv") || input.endsWith(".txt")) {
			return Format.FORMAT_CSV;
		} else if(input.endsWith(".wkt")) {
			return Format.FORMAT_WKT;
		} else if(input.endsWith(".shp")) {
			return Format.FORMAT_SHAPEFILE;
		} else if(input.endsWith(".gpkg")) {
			return Format.FORMAT_GEOPACKAGE;
		} else if(input.endsWith(".net.xml") || input.endsWith(".poi.xml")) {
			return Format.FORMAT_SUMO;
		}   
		
		return Format.FORMAT_UNKNOWN;
	}


	/** @brief Return the connection to a database defined by the input/output parts
	 * 
	 * This method is only valid for database connections.
	 * 
	 * @param format The format of the connection
	 * @param inputParts The definition of the input/output source/destination
	 * @param name The name of the option for reporting purposes
	 * @return The connection to the specified database
	 * @throws IOException When connecting the database fails or the format is a file
	 */
	public static Connection getConnection(Format format, String[] inputParts, String name) throws IOException {
		try {
			switch(format) {
			case FORMAT_POSTGRES:
				return DriverManager.getConnection(inputParts[0], inputParts[2], inputParts[3]);
			case FORMAT_SQLITE:
				return DriverManager.getConnection(inputParts[0]);
			case FORMAT_CSV:
			case FORMAT_SHAPEFILE:
			case FORMAT_GEOPACKAGE:
			case FORMAT_SUMO:
			default:
				throw new IOException("The format '" + getFormatMMLName(format) + "' used in option '" + name + "' does not include table names.");
			}
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
	
	
	/** @brief Returns the name of the database table defined in the input/output definition
	 * @param format The format of the connection
	 * @param inputParts The definition of the input/output source/destination
	 * @param name The name of the option for reporting purposes
	 * @return The name of the specified database table
	 * @throws IOException When the format is a file
	 */
	public static String getTableName(Format format, String[] inputParts, String name) throws IOException {
		switch(format) {
		case FORMAT_POSTGRES:
		case FORMAT_SQLITE:
			return inputParts[1];
		case FORMAT_CSV:
		case FORMAT_SHAPEFILE:
		case FORMAT_GEOPACKAGE:
		case FORMAT_SUMO:
		default:
			throw new IOException("The format '" + getFormatMMLName(format) + "' used in option '" + name + "' does not include table names.");
		}
	}
	
	
	/** @brief Returns whether the defined table exists
	 * @param connection The connection to the database
	 * @param table The name of the table, including the schema
	 * @return Whether the defined table exists
	 * @throws SQLException If something fails
	 */
	public static boolean tableExists(Connection connection, String table) throws SQLException {
		int dot = table.indexOf('.');
		String query = "SELECT EXISTS (SELECT FROM pg_tables WHERE schemaname = '" + table.substring(0, dot) + "' AND tablename = '" + table.substring(dot+1) + "');";
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery(query);
		rs.next();
		boolean ret = rs.getBoolean(1);
		rs.close();
		s.close();
		return ret;
	}
	

}

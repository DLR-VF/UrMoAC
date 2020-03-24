/**
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * 
 * Copyright © 2016-2019 German Aerospace Center 
 * All rights reserved
 */
package de.dlr.ivf.urmo.router.io;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.postgresql.PGConnection;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

import de.dlr.ivf.urmo.router.modes.EntrainmentMap;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.IDGiver;
import de.dlr.ivf.urmo.router.shapes.Layer;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class DBIOHelper
 * @brief Some helper methods for loading data from the database
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class DBIOHelper {
	/**
	 * @brief Loads a set of objects from the db
	 * 
	 * @param url The url of the database
	 * @param table The table to read from
	 * @param user The user name for connecting to the database
	 * @param pw The user's password
	 * @param filter A WHERE-clause statement (optional, empty string if not used)
	 * @param varName The name of the attached variable
	 * @param layerName The name of the layer to generate
	 * @param idGiver A reference to something that supports a running ID
	 * @return The generated layer with the read objects
	 * @throws SQLException
	 * @throws ParseException
	 */
	public static Layer load(String url, String table, String user, String pw, String filter, String varName,
			String idS, String geomS, String layerName, IDGiver idGiver, int epsg) throws SQLException, ParseException {
		if (!"".equals(filter)) {
			filter = " WHERE " + filter;
		}
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
		String query = "SELECT *,";
		if(!"".equals(varName)) {
			query += varName + ",";
		}
		query += "ST_AsBinary(ST_TRANSFORM(" + geomS + "," + epsg + ")) FROM " + table + filter + ";";
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery(query);

		WKBReader wkbRead = new WKBReader();
		Layer layer = new Layer(layerName);
		while (rs.next()) {
			ResultSetMetaData rsmd = rs.getMetaData();
			int numColumns = rsmd.getColumnCount();
			Geometry geom = wkbRead.read(rs.getBytes(numColumns));
			double var = 1;
			if (!"".equals(varName)) {
				var = rs.getDouble(numColumns-1);
			}
			LayerObject o = new LayerObject(idGiver.getNextRunningID(), rs.getLong(idS), var, geom);
			layer.addObject(o);
		}
		return layer;
	}
	
	
	public static EntrainmentMap loadEntrainment(String url, String table, String user, String pw, String filter)  throws SQLException, ParseException {
		if (!"".equals(filter)) {
			filter = " WHERE " + filter;
		}
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		String query = "SELECT carrier,carrier_subtype,carried FROM " + table + filter + ";";
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery(query);
		EntrainmentMap em = new EntrainmentMap();
		while (rs.next()) {
			em.add(""+rs.getString("carrier")+rs.getInt("carrier_subtype"), Modes.getMode(rs.getString("carried")).id);
		}
		return em;
	}


	public static Geometry loadGeometry(String url, String table, String user, String pw, String geomS, int epsg) throws SQLException, ParseException {
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
		String query = "SELECT *,ST_AsBinary(ST_TRANSFORM(" + geomS + "," + epsg + ")) FROM " + table + ";";
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery(query);

		WKBReader wkbRead = new WKBReader();
		while (rs.next()) {
			ResultSetMetaData rsmd = rs.getMetaData();
			Geometry geom = wkbRead.read(rs.getBytes(rsmd.getColumnCount()));
			return geom;
		}
		return null;
	}
	
	
	
	
	/**
	 * @brief Writes the given map of edge values into the database (the
	 *        database must not exist)
	 * 
	 * @param name The name of the database to generate
	 * @param from The name of the starting edge
	 * @param gtfs A container of GTFS information
	 * @param values The values of the edges
	 * @throws SQLException
	 */
	/* !!!
	public static void writeEdgeMap(String name, String from, DijkstraResult res, GTFSData gtfs) throws SQLException {
		name = name.replace('#', '_');
		String url = "jdbc:postgresql://localhost:5432/tests";
		String user = "postgres";
		String pw = "doofesPasswort";
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
		String sql = "CREATE TABLE " + name
				+ " (id varchar(40), sid varchar(40), avg_distance real, avg_tt real, avg_num real, sum_num real, avg_value real, sum_value real, sum_weight real, num_sources real, modes text);";
		Statement s = connection.createStatement();
		s.executeUpdate(sql);
		connection.setAutoCommit(false);
		PreparedStatement ps = connection.prepareStatement("INSERT INTO " + name + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
		for (DBEdge e : res.edgeMap.keySet()) {
			Measurements m = res.edgeMap.get(e);
			float numSources = m.getNumSources();
			float sourcesWeight = m.getSourceWeights();
			ps.setString(1, from);
			ps.setString(2, e.id);
			ps.setFloat(3, (float) (m.distance / m.weight));
			ps.setFloat(4, (float) (m.tt / m.weight));
			ps.setFloat(5, (float) (m.num / numSources));
			ps.setFloat(6, (float) m.num);
			ps.setFloat(7, (float) (m.sum / sourcesWeight));
			ps.setFloat(8, (float) m.sum);
			ps.setFloat(9, (float) m.weight);
			ps.setFloat(10, (float) numSources);
			ps.setString(11, gtfs.getModesString(m.lines));
			ps.addBatch();
		}
		ps.executeBatch();
		connection.commit();
	}
*/

	/**
	 * @brief Parses the given definition string, throwing an exception if the
	 *        size mismatches
	 * 
	 *        Basically, the string is just split
	 * 
	 * @param d The connection definition string
	 * @return The split string
	 * @throws IOException if the string's length does not match the number of required of attributes
	 */
	public static String[] parseOption(String d) throws IOException {
		String[] r = d.split(";");
		if (r.length != 4) {
			throw new IOException("The database must be of the form <URL>;<TABLE>;<USER>;<PW>");
		}
		return r;
	}

}

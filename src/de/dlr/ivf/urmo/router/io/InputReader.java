/**
 * Copyright (c) 2016-2020 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * @author: Daniel.Krajzewicz@dlr.de
 * Licensed under the GNU General Public License v3.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraße 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.cli.CommandLine;
import org.postgresql.PGConnection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
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
public class InputReader {
	
	
	/**
	 * @brief Finds the correct UTM-zone for the given net. Reference is the most south most west point in the from-locations.
	 * The calculation is based on utm-zones for longitudes from -180 to 180 degrees.
	 * The latitude is only valid from -84 to 84 degrees.
	 * The returned UTM-zones start with 32500 for the southern hemisphere and with 32600 for the northern hemisphere. 
	 * @return The epsg-code of the UTM-zone or -1 of no UTM-zone could be found (e.g. north-pole )
	 * @throws SQLException
	 * @throws ParseException
	 * @throws IOException 
	 */
	public static int findUTMZone(CommandLine options) throws SQLException, ParseException, IOException {
		String[] r = Utils.checkDefinition(options.getOptionValue("from", ""), "from");
		if (!r[0].equals("db")) {
			return 0;
		}
		Connection connection = DriverManager.getConnection(r[1], r[3], r[4]);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
		String geomString = options.getOptionValue("from.geom", "the_geom");
		String query = "SELECT min(ST_X(ST_TRANSFORM("+geomString+",4326)))as lon, min(ST_Y(ST_TRANSFORM("+geomString+",4326)))as lat FROM " + r[2] + ";";
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery(query);
		int epsg=-1;
		double lon, lat;
		while (rs.next()) {
			lon = rs.getDouble("lon");
			lat = rs.getDouble("lat");
			if(lat>84.0 || lat<-84.0) {
				//around north or south-pole!
				break;
			}
			if(lon>180.0 || lon<-180.0) {
				//invalid longitude!
				break;
			}
			if(lat>=0) { //northern hemisphere
				epsg = 32600;
			} else { //southern hemisphere
				epsg = 32500;
			}
			epsg += ((180.0 + lon) / 6.) + 1;
		}
		rs.close();
		s.close();
		return epsg;
	}
	
	
	public static Layer loadLayer(CommandLine options, String base, String varName, IDGiver idGiver, int epsg) throws SQLException, ParseException, IOException {
		String filter = varName==null ? "" : options.getOptionValue(base + "-filter", ""); // !!! use something different
		varName = varName==null ? null : options.getOptionValue(varName, "");
		String[] r = Utils.checkDefinition(options.getOptionValue(base, ""), base);
		if (r[0].equals("db")) {
			return loadLayerFromDB(base, r[1], r[2], r[3], r[4], filter, varName, 
					options.getOptionValue(base + ".id", "gid"), options.getOptionValue(base + ".geom", "the_geom"), 
					idGiver, epsg);
		} else {
			return loadLayerFromFile(base, r[1], idGiver);
		}
	}
	
	
	
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
	private static Layer loadLayerFromDB(String layerName, String url, String table, String user, String pw, String filter, String varName,
			String idS, String geomS, IDGiver idGiver, int epsg) throws SQLException, ParseException {
		if (!"".equals(filter)) {
			filter = " WHERE " + filter;
		}
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
		String query = "SELECT " + idS + ",";
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
			byte[] bytes = rs.getBytes(numColumns);
			if(bytes==null) {
				System.err.println(" Object '" + rs.getLong(idS) + "' has no geometry.");
				continue;
			}
			Geometry geom = wkbRead.read(bytes);
			double var = 1;
			if (!"".equals(varName)) {
				var = rs.getDouble(numColumns-1);
			}
			LayerObject o = new LayerObject(idGiver.getNextRunningID(), rs.getLong(idS), var, geom);
			layer.addObject(o);
		}
		rs.close();
		s.close();
		connection.close();
		return layer;
	}
	
	
	/**
	 * @brief Loads a set of objects from file
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
	private static Layer loadLayerFromFile(String layerName, String fileName, IDGiver idGiver) throws ParseException, IOException { 
		Layer layer = new Layer(layerName);
		GeometryFactory gf = new GeometryFactory(new PrecisionModel());
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		do {
			line = br.readLine();
			if(line!=null && line.length()!=0 && line.charAt(0)!='#') {
				String[] vals = line.split(";");
				double var = vals.length==4 ? Double.parseDouble(vals[3]) : 1;
				Point p = gf.createPoint(new Coordinate(Double.parseDouble(vals[1]), Double.parseDouble(vals[2])));
				LayerObject o = new LayerObject(idGiver.getNextRunningID(), Long.parseLong(vals[0]), var, p);
				layer.addObject(o);
			}
	    } while(line!=null);
		br.close();
		return layer;
	}
	
	
	
	public static EntrainmentMap loadEntrainment(CommandLine options)  throws SQLException, ParseException, IOException {
		String[] r = Utils.checkDefinition(options.getOptionValue("entrainment", ""), "entrainment");
		if (r[0].equals("db")) {
			return loadEntrainmentFromDB(r[1], r[2], r[3], r[4]);
		} else {
			return loadEntrainmentFromFile(r[1]);
		}
	}
	
	
	
	private static EntrainmentMap loadEntrainmentFromDB(String url, String table, String user, String pw)  throws SQLException, ParseException {
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		String query = "SELECT carrier,carrier_subtype,carried FROM " + table + ";";
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery(query);
		EntrainmentMap em = new EntrainmentMap();
		while (rs.next()) {
			em.add(""+rs.getString("carrier")+rs.getInt("carrier_subtype"), Modes.getMode(rs.getString("carried")).id);
		}
		rs.close();
		s.close();
		connection.close();
		return em;
	}


	
	private static EntrainmentMap loadEntrainmentFromFile(String fileName) throws IOException {
		EntrainmentMap em = new EntrainmentMap();
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		do {
			line = br.readLine();
			if(line!=null && line.length()!=0 && line.charAt(0)!='#') {
				String[] vals = line.split(";");
				em.add(vals[0]+vals[1], Modes.getMode(vals[2]).id);
				line = br.readLine();
			}
	    } while(line!=null);
		br.close();
		return em;
	}


	public static Geometry loadGeometry(String def, String what, int epsg)  throws SQLException, ParseException, IOException {
		String[] r = Utils.checkDefinition(def, what);
		if (r[0].equals("db")) {
			return loadGeometryFromDB(r[1], r[2], r[3], r[4], epsg);
		} else {
			return loadGeometryFromFile(r[1]);
		}
	}
	
	
	private static Geometry loadGeometryFromDB(String url, String table, String user, String pw, int epsg) throws SQLException, ParseException {
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
		String query = "SELECT ST_AsBinary(ST_TRANSFORM(the_geom," + epsg + ")) FROM " + table + ";";
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery(query);
		Geometry geom = null;
		WKBReader wkbRead = new WKBReader();
		if (rs.next()) {
			geom = wkbRead.read(rs.getBytes(0));
		}
		rs.close();
		s.close();
		connection.close();
		return geom;
	}
	
	
	private static Geometry loadGeometryFromFile(String fileName) throws ParseException, IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = br.readLine();
		while(line!=null && (line.length()==0 || line.charAt(0)=='#')) {
			line = br.readLine();
		}
		br.close();
		String[] vals = line.split(";");
		if((vals.length % 2)!=0) {
			throw new IOException("odd number for coordinates");
		}
		Coordinate[] coords = new Coordinate[(int) (vals.length/2)];
		int j = 0;
		for(int i=0; i<vals.length; i+=2, ++j) {
			coords[j] = new Coordinate(Double.parseDouble(vals[i]), Double.parseDouble(vals[i+1]));
	    }
		GeometryFactory gf = new GeometryFactory(new PrecisionModel());
		return gf.createPolygon(coords);
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


}

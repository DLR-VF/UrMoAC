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
 * Rutherfordstraﬂe 2
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
import java.util.Vector;

import org.postgresql.PGConnection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.DBNode;
import de.dlr.ivf.urmo.router.shapes.IDGiver;

/**
 * @class DBNetLoader
 * @brief Loads the road network stored in the db
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class NetLoader {
	/**
	 * @brief Loads the road network from the database
	 * @return The loaded net
	 * @throws SQLException
	 * @throws ParseException
	 * @throws IOException 
	 */
	public static DBNet loadNet(IDGiver idGiver, String def, int epsg, long uModes) throws SQLException, ParseException, IOException {
		String[] r = Utils.checkDefinition(def, "net");
		DBNet net = null;
		if (r[0].equals("db")) {
			net = loadNetFromDB(idGiver, r[1], r[2], r[3], r[4], epsg, uModes);
		} else {
			net = loadNetFromFile(idGiver, r[1], uModes);
		}
		// add other directions to mode foot
		net.extendDirections();
		return net;
	}

	private static DBNet loadNetFromDB(IDGiver idGiver, String url, String table, String user, String pw, int epsg, long uModes) throws SQLException, ParseException {
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
		String query = "SELECT oid,nodefrom,nodeto,mode_walk,mode_bike,mode_mit,vmax,length,ST_AsBinary(ST_TRANSFORM(the_geom," + epsg + ")) FROM " + table + ";";
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery(query);
		WKBReader wkbRead = new WKBReader();
		DBNet net = new DBNet(idGiver);
		while (rs.next()) {
			long modes = 0;
			if(rs.getBoolean("mode_walk")) modes = modes | Modes.getMode("foot").id;
			if(rs.getBoolean("mode_bike")) modes = modes | Modes.getMode("bicycle").id;
			if(rs.getBoolean("mode_mit")) modes = modes | Modes.getMode("passenger").id;
			//if(rs.getBoolean("mode_walk") || rs.getBoolean("mode_bike")) modes = modes | Modes.getMode("e-scooter").id;
			if(modes==0 && ((modes&uModes)==0)) {
				continue;
			}
			ResultSetMetaData rsmd = rs.getMetaData();
			//double length = rs.getDouble(rsmd.getColumnCount());
			Geometry geom = wkbRead.read(rs.getBytes(rsmd.getColumnCount()));
			// !!! hack - for some reasons, edge geometries are stored as MultiLineStrings in the database 
			if(geom.getNumGeometries()!=1) {
				System.err.println("Edge '" + rs.getString("oid") + "' has a multi geometries...");
			}
			LineString geom2 = (LineString) geom.getGeometryN(0);
			Coordinate[] cs = geom2.getCoordinates();
			DBNode fromNode = net.getNode(rs.getLong("nodefrom"), cs[0]);
			DBNode toNode = net.getNode(rs.getLong("nodeto"), cs[cs.length - 1]);
			DBEdge e = new DBEdge(net.getNextID(), rs.getString("oid"), fromNode, toNode, modes, rs.getDouble("vmax") / 3.6, geom2, rs.getDouble("length"));
			net.addEdge(e);
		}
		rs.close();
		s.close();
		connection.close();
		return net;
	}

	
	private static DBNet loadNetFromFile(IDGiver idGiver, String fileName, long uModes) throws ParseException, IOException {
		DBNet net = new DBNet(idGiver);
		GeometryFactory gf = new GeometryFactory(new PrecisionModel());
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		do {
			line = br.readLine();
			if(line!=null && line.length()!=0 && line.charAt(0)!='#') {
				String[] vals = line.split(";");
				long modes = 0;
				if("true".equals(vals[3])||"1".equals(vals[3])) modes = modes | Modes.getMode("foot").id;
				if("true".equals(vals[4])||"1".equals(vals[4])) modes = modes | Modes.getMode("bicycle").id;
				if("true".equals(vals[5])||"1".equals(vals[5])) modes = modes | Modes.getMode("passenger").id;
				if(modes==0 && ((modes&uModes)==0)) {
					continue;
				}
				int num = vals.length - 8;
				if((num % 2)!=0) {
					throw new IOException("odd number for coordinates");
				}
				Coordinate[] coords = new Coordinate[(int) num/2];
				int j = 0;
				for(int i=8; i<vals.length; i+=2, ++j ) {
					coords[j] = new Coordinate(Double.parseDouble(vals[i]), Double.parseDouble(vals[i+1]));
				}
				DBNode fromNode = net.getNode(Long.parseLong(vals[1]), coords[0]);
				DBNode toNode = net.getNode(Long.parseLong(vals[2]), coords[coords.length - 1]);
				LineString ls = gf.createLineString(coords);
				DBEdge e = new DBEdge(net.getNextID(), vals[0], fromNode, toNode, modes, Double.parseDouble(vals[6]) / 3.6, ls, Double.parseDouble(vals[7]));
				net.addEdge(e);
			}
	    } while(line!=null);
		br.close();
		return net;
	}

	
	
	
	
	public static int loadTravelTimes(DBNet net, String def, boolean verbose) throws SQLException, ParseException, IOException {
		String[] r = Utils.checkDefinition(def, "travel times");
		int numFalse = 0;
		if (r[0].equals("db")) {
			numFalse = loadTravelTimesFromDB(net, r[1], r[2], r[3], r[4], verbose);
		} else {
			numFalse = loadTravelTimesFromFile(net, r[1], verbose);
		}
		net.sortSpeedReductions();
		return numFalse;
	}
		
		
	private static int loadTravelTimesFromDB(DBNet net, String url, String table, String user, String pw, boolean verbose) throws SQLException, ParseException {
		int numFalse = 0;
		int numOk = 0;
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
		String query = "SELECT ibegin,iend,eid,speed FROM " + table + ";";
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery(query);
		while (rs.next()) {
			String eid = rs.getString("eid");
			DBEdge edge = net.getEdgeByName(eid);
			if(edge==null) {
				++numFalse;
				continue;
			}
			++numOk;
			float ibegin = rs.getFloat("ibegin");
			float iending = rs.getFloat("iend");
			float speed = rs.getFloat("speed");
			edge.addSpeedReduction(ibegin, iending, speed);
		}
		rs.close();
		s.close();
		connection.close();
		if(verbose) {
			System.out.println(" " + numFalse + " of " + (numOk+numFalse) + " informations could not been loaded.");
		}
		return numFalse;
	}

	
	private static int loadTravelTimesFromFile(DBNet net, String fileName, boolean verbose) throws ParseException, IOException {
		int numFalse = 0;
		int numOk = 0;
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		do {
			line = br.readLine();
			if(line!=null && line.length()!=0 && line.charAt(0)!='#') {
				String[] vals = line.split(";");
				DBEdge edge = net.getEdgeByName(vals[0]);
				if(edge==null) {
					++numFalse;
					continue;
				}
				++numOk;
				edge.addSpeedReduction(Float.parseFloat(vals[1]), Float.parseFloat(vals[2]), Float.parseFloat(vals[3]));
			}
	    } while(line!=null);
		br.close();
		if(verbose) {
			System.out.println(" " + numFalse + " of " + (numOk+numFalse) + " informations could not been loaded.");
		}
		return numFalse;
	}

	
	
}

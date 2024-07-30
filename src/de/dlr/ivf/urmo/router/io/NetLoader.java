/*
 * Copyright (c) 2016-2024
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
package de.dlr.ivf.urmo.router.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;
import org.postgresql.PGConnection;
import org.xml.sax.SAXException;

import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.output.NetErrorsWriter;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.DBNode;
import de.dlr.ivf.urmo.router.shapes.IDGiver;

/**
 * @class NetLoader
 * @brief Loads the road network stored in a database or files
 * @author Daniel Krajzewicz
 */
public class NetLoader {
	/** @brief Loads the road network
	 * @param idGiver Instance supporting running ids 
	 * @param def Source definition
	 * @param vmaxAttr The attribute (column) to read the maximum velocity from 
	 * @param epsg The projection
	 * @param uModes The modes for which the network shall be loaded
	 * @return The loaded net
	 * @throws IOException When something fails 
	 */
	public static DBNet loadNet(IDGiver idGiver, String def, String netBoudary, String vmaxAttr, int epsg, long uModes, NetErrorsWriter errorsWriter) throws IOException {
		Utils.Format format = Utils.getFormat(def);
		String[] inputParts = Utils.getParts(format, def, "net");
		DBNet net = null;
		switch(format) {
		case FORMAT_POSTGRES:
		case FORMAT_SQLITE:
			net = loadNetFromDB(idGiver, format, inputParts, netBoudary, vmaxAttr, epsg, uModes, errorsWriter);
			break;
		case FORMAT_CSV:
			net = loadNetFromCSVFile(idGiver, inputParts[0], uModes, errorsWriter);
			break;
		case FORMAT_WKT:
			net = loadNetFromWKTFile(idGiver, inputParts[0], uModes, errorsWriter);
			break;
		case FORMAT_SUMO:
			net = loadNetFromSUMOFile(idGiver, inputParts[0], uModes, errorsWriter);
			break;
		case FORMAT_GEOPACKAGE:
			throw new IOException("Reading 'net' from " + Utils.getFormatMMLName(format) + " is not supported.");
		default:
			throw new IOException("Could not recognize the format used for GTFS.");
		}
		if(net==null) {
			throw new IOException("The network could not be loaded");
		}
		// set opposite edges; add opposite pedestrian edges if foot is used
		net.extendDirections((uModes&Modes.getMode("foot").id)!=0);
		return net;
	}

	
	/** @brief Reads the network from a database
	 * @param idGiver Instance supporting running ids 
	 * @param format The source format
	 * @param inputParts The source definition
	 * @param vmax The attribute (column) to read the maximum velocity from 
	 * @param epsg The projection
	 * @param uModes The modes for which the network shall be loaded
	 * @return The loaded net
	 * @throws IOException When something fails 
	 */
	private static DBNet loadNetFromDB(IDGiver idGiver, Utils.Format format, String[] inputParts, String netBoudary, String vmax, int epsg, long uModes, NetErrorsWriter errorsWriter) throws IOException {
		// db jars issue, see https://stackoverflow.com/questions/999489/invalid-signature-file-when-attempting-to-run-a-jar
		try {
			Class.forName("org.sqlite.JDBC");
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			Connection connection = Utils.getConnection(format, inputParts, "net");
			connection.setAutoCommit(true);
			connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
			String query = "SELECT oid,nodefrom,nodeto,mode_walk,mode_bike,mode_mit,"+vmax+",length,ST_AsBinary(ST_TRANSFORM(geom," + epsg + ")) FROM " + Utils.getTableName(format, inputParts, "net");
			if(netBoudary!=null) {
				query += " WHERE ST_Within(ST_TRANSFORM(geom," + epsg + "), " + netBoudary + ")";
			}
			query += ";";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
			WKBReader wkbRead = new WKBReader();
			DBNet net = new DBNet(idGiver, errorsWriter, false); // todo: implement report settings
			boolean ok = true;
			while (rs.next()) {
				long modes = 0;
				if(rs.getBoolean("mode_walk")) modes = modes | Modes.getMode("foot").id;
				if(rs.getBoolean("mode_bike")) modes = modes | Modes.getMode("bike").id;
				if(rs.getBoolean("mode_mit")) modes = modes | Modes.getMode("car").id;
				modes = (modes&Modes.customAllowedAt)!=0 ? modes | Modes.getMode("custom").id : modes;
				if(modes==0 || ((modes&uModes)==0)) {
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
				ok &= net.addEdge(rs.getString("oid"), fromNode, toNode, modes, rs.getDouble(vmax) / 3.6, geom2, rs.getDouble("length"));
			}
			rs.close();
			s.close();
			connection.close();
			return ok ? net : null;
		} catch (SQLException | ParseException e) {
			throw new IOException(e);
		}
	}

	
	/** @brief Reads the network from a CSV file
	 * @param idGiver Instance supporting running ids 
	 * @param fileName The file to read the network from
	 * @param uModes The modes for which the network shall be loaded
	 * @return The loaded net
	 * @throws IOException When something fails 
	 */
	private static DBNet loadNetFromCSVFile(IDGiver idGiver, String fileName, long uModes, NetErrorsWriter errorsWriter) throws IOException {
		DBNet net = new DBNet(idGiver, errorsWriter, false); // todo: implement report settings
		GeometryFactory gf = new GeometryFactory(new PrecisionModel());
		// https://stackoverflow.com/questions/1388602/do-i-need-to-close-both-filereader-and-bufferedreader
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		boolean ok = true;
		do {
			line = br.readLine();
			if(line==null || line.length()==0 || line.charAt(0)=='#') {
				continue;
			}
			String[] vals = line.split(";");
			long modes = 0;
			if("true".equals(vals[3].toLowerCase()) || "1".equals(vals[3])) modes = modes | Modes.getMode("foot").id;
			if("true".equals(vals[4].toLowerCase()) || "1".equals(vals[4])) modes = modes | Modes.getMode("bike").id;
			if("true".equals(vals[5].toLowerCase()) || "1".equals(vals[5])) modes = modes | Modes.getMode("car").id;
			modes = (modes&Modes.customAllowedAt)!=0 ? modes | Modes.getMode("custom").id : modes;
			if(modes==0 || ((modes&uModes)==0)) {
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
			ok &= net.addEdge(vals[0], fromNode, toNode, modes, Double.parseDouble(vals[6]) / 3.6, ls, Double.parseDouble(vals[7]));
	    } while(line!=null);
		br.close();
		return ok ? net : null;
	}
	
	
	/** @brief Reads the network from a WKT file
	 * @param idGiver Instance supporting running ids 
	 * @param fileName The file to read the network from
	 * @param uModes The modes for which the network shall be loaded
	 * @return The loaded net
	 * @throws IOException When something fails 
	 */
	private static DBNet loadNetFromWKTFile(IDGiver idGiver, String fileName, long uModes, NetErrorsWriter errorsWriter) throws IOException {
		try {
			DBNet net = new DBNet(idGiver, errorsWriter, false); // todo: implement report settings
			WKTReader wktReader = new WKTReader();
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line = null;
			boolean ok = true;
			do {
				line = br.readLine();
				if(line==null || line.length()==0 || line.charAt(0)=='#') {
					continue;
				}
				String[] vals = line.split(";");
				long modes = 0;
				if("true".equals(vals[3].toLowerCase()) || "1".equals(vals[3])) modes = modes | Modes.getMode("foot").id;
				if("true".equals(vals[4].toLowerCase()) || "1".equals(vals[4])) modes = modes | Modes.getMode("bike").id;
				if("true".equals(vals[5].toLowerCase()) || "1".equals(vals[5])) modes = modes | Modes.getMode("car").id;
				modes = (modes&Modes.customAllowedAt)!=0 ? modes | Modes.getMode("custom").id : modes;
				if(modes==0 || ((modes&uModes)==0)) {
					continue;
				}
				LineString geom = (LineString) wktReader.read(vals[8]);
				Coordinate cs[] = geom.getCoordinates();
				DBNode fromNode = net.getNode(Long.parseLong(vals[1]), cs[0]);
				DBNode toNode = net.getNode(Long.parseLong(vals[2]), cs[cs.length - 1]);
				ok &= net.addEdge(vals[0], fromNode, toNode, modes, Double.parseDouble(vals[6]) / 3.6, geom, Double.parseDouble(vals[7]));
		    } while(line!=null);
			br.close();
			return ok ? net : null;
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}
	
	
	/** @brief Reads the network from a SUMO road network file
	 * @param idGiver Instance supporting running ids 
	 * @param fileName The file to read the network from
	 * @param uModes The modes for which the network shall be loaded
	 * @return The loaded net
	 * @throws IOException When something fails 
	 */
	private static DBNet loadNetFromSUMOFile(IDGiver idGiver, String fileName, long uModes, NetErrorsWriter errorsWriter) throws IOException {
		try {
			DBNet net = new DBNet(idGiver, errorsWriter, false); // todo: implement report settings
			SAXParserFactory factory = SAXParserFactory.newInstance();
	        SAXParser saxParser = factory.newSAXParser();
	        SUMONetHandler handler = new SUMONetHandler(net, uModes);
	        saxParser.parse(fileName, handler);
	        return net;
		} catch (ParserConfigurationException | SAXException e) {
			throw new IOException(e);
		}
	}

	
	
	/** @brief Reads travel times
	 * @param net The road network
	 * @param def The source definition
	 * @param verbose Whether report more
	 * @return The number of not assigned speed information
	 * @throws IOException When something fails 
	 */
	public static int loadTravelTimes(DBNet net, String def, boolean verbose) throws IOException {
		Utils.Format format = Utils.getFormat(def);
		String[] inputParts = Utils.getParts(format, def, "net");
		int numFalse = 0;
		switch(format) {
		case FORMAT_POSTGRES:
		case FORMAT_SQLITE:
			numFalse = loadTravelTimesFromDB(net, format, inputParts, verbose);
			break;
		case FORMAT_CSV:
			numFalse = loadTravelTimesFromCSVFile(net, inputParts[0], verbose);
			break;
		case FORMAT_SUMO:
			numFalse = loadTravelTimesFromSUMOFile(net, inputParts[0], verbose);
			break;
		case FORMAT_WKT:
		case FORMAT_SHAPEFILE:
		case FORMAT_GEOPACKAGE:
			throw new IOException("Reading 'net' from " + Utils.getFormatMMLName(format) + " is not supported.");
		default:
			throw new IOException("Could not recognize the format used for GTFS.");
		}
		net.sortSpeedReductions();
		return numFalse;
	}
		

	/** @brief Reads travel times from a database
	 * @param net The road network
	 * @param format The source format
	 * @param inputParts The source definition
	 * @param verbose Whether report more
	 * @return The number of not assigned speed information
	 * @throws IOException When something fails 
	 */
	private static int loadTravelTimesFromDB(DBNet net, Utils.Format format, String[] inputParts, boolean verbose) throws IOException {
		// db jars issue, see https://stackoverflow.com/questions/999489/invalid-signature-file-when-attempting-to-run-a-jar
		try {
			Class.forName("org.sqlite.JDBC");
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			int numFalse = 0;
			int numOk = 0;
			Connection connection = Utils.getConnection(format, inputParts, "travel-times");
			connection.setAutoCommit(true);
			connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
			String query = "SELECT ibegin,iend,eid,speed FROM " + Utils.getTableName(format, inputParts, "travel-times") + ";";
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
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	
	/** @brief Reads travel times from a csv file
	 * @param net The road network
	 * @param fileName The file to read the travel times from
	 * @param verbose Whether report more
	 * @return The number of not assigned speed information
	 * @throws IOException When something fails 
	 */
	private static int loadTravelTimesFromCSVFile(DBNet net, String fileName, boolean verbose) throws IOException {
		int numFalse = 0;
		int numOk = 0;
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		do {
			line = br.readLine();
			if(line==null || line.length()==0 || line.charAt(0)=='#') {
				continue;
			}
			String[] vals = line.split(";");
			DBEdge edge = net.getEdgeByName(vals[0]);
			if(edge==null) {
				++numFalse;
				continue;
			}
			++numOk;
			edge.addSpeedReduction(Float.parseFloat(vals[1]), Float.parseFloat(vals[2]), Float.parseFloat(vals[3]));
	    } while(line!=null);
		br.close();
		if(verbose) {
			System.out.println(" " + numFalse + " of " + (numOk+numFalse) + " informations could not been loaded.");
		}
		return numFalse;
	}

	
	/** @brief Reads travel times from a SUMO file
	 * @param net The road network
	 * @param fileName The file to read the travel times from
	 * @param verbose Whether report more
	 * @return The number of not assigned speed information
	 * @throws IOException When something fails 
	 */
	private static int loadTravelTimesFromSUMOFile(DBNet net, String fileName, boolean verbose) throws IOException {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
	        SAXParser saxParser = factory.newSAXParser();
	        SUMOEdgeDumpHandler handler = new SUMOEdgeDumpHandler(net);
	        saxParser.parse(fileName, handler);
	        return handler.getNumFalse();
		} catch (ParserConfigurationException | SAXException e) {
			throw new IOException(e);
		}
	}

	
	
}

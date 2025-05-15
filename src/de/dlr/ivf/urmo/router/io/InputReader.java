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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;
import org.postgresql.PGConnection;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.dks.utils.options.OptionsCont;
import de.dlr.ivf.urmo.router.modes.EntrainmentMap;
import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.DBNode;
import de.dlr.ivf.urmo.router.shapes.DBODRelation;
import de.dlr.ivf.urmo.router.shapes.Layer;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class InputReader
 * @brief Methods for loading origins/destinations, entrainment data, geometries and O/D-connections 
 * @author Daniel Krajzewicz
 */
public class InputReader {
	// --------------------------------------------------------
	// loading layers
	// --------------------------------------------------------
	/**
	 * @brief Loads a set of objects (a layer)
	 * 
	 * @param options The command line options 
	 * @param bounds The bounds to clip the read thing to
	 * @param base The layer/type ("from", "to") of the objects to load
	 * @param varName Name of the variable field
	 * @param dismissWeight Whether the weight shall be discarded
	 * @param epsg The used projection
	 * @return The generated layer with the read objects
	 * @throws IOException When something fails
	 */
	public static Layer loadLayer(OptionsCont options, Geometry bounds, String base, String varName, boolean dismissWeight, int epsg) throws IOException {
		String filter = options.isSet(base+".filter") ? options.getString(base + ".filter") : ""; // !!! use something different
		varName = varName==null ? null : options.getString(varName);
		String def = options.getString(base);
		Utils.Format format = Utils.getFormat(def);
		String[] inputParts = Utils.getParts(format, def, base);
		Layer layer = null;
		switch(format) {
		case FORMAT_POSTGRES:
		case FORMAT_SQLITE:
			layer = loadLayerFromDB(base, bounds, format, inputParts, filter, varName, options.getString(base + ".id"), options.getString(base + ".geom"), epsg, dismissWeight);
			break;
		case FORMAT_CSV:
			layer = loadLayerFromCSVFile(base, bounds, inputParts[0], dismissWeight);
			break;
		case FORMAT_WKT:
			layer = loadLayerFromWKTFile(base, bounds, inputParts[0], dismissWeight);
			break;
		case FORMAT_SUMO:
			layer = loadLayerFromSUMOPOIs(base, bounds, inputParts[0], dismissWeight);
			break;
		case FORMAT_SHAPEFILE:
		case FORMAT_GEOPACKAGE:
			throw new IOException("Reading '" + base + "' from " + Utils.getFormatMMLName(format) + " is not supported.");
		default:
			throw new IOException("Could not recognize the format used for '" + base + "'.");
		}
		if(layer==null) {
			throw new IOException("Objects could not be loaded.");
		}
		return layer;
	}
	
	
	
	/**
	 * @brief Loads a set of objects from the db
	 * 
	 * @param layerName The layer/type ("from", "to") of the objects to load
	 * @param bounds The bounds to clip the read thing to
	 * @param format The format of the source
	 * @param inputParts The definition of the source
	 * @param filter A WHERE-clause statement (optional, empty string if not used)
	 * @param varName The name of the attached variable
	 * @param idS The name of the column to read the IDs from
	 * @param geomS The name of the column to read the geometry from
	 * @param epsg The EPSG of the projection to use
	 * @param dismissWeight Whether the weight shall be discarded
	 * @return The generated layer with the read objects
	 * @throws IOException When something fails
	 */
	private static Layer loadLayerFromDB(String layerName, Geometry bounds, Utils.Format format, String[] inputParts, String filter, String varName,
			String idS, String geomS, int epsg, boolean dismissWeight) throws IOException {
		// db jars issue, see https://stackoverflow.com/questions/999489/invalid-signature-file-when-attempting-to-run-a-jar
		try {
			Class.forName("org.sqlite.JDBC");
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		if(dismissWeight&&(varName!=null&&!"".equals(varName))) {
			System.out.println("Warning: the weight option is not used as no aggregation takes place.");
			varName = null;
		}
		Set<Long> seen = new HashSet<Long>();
		boolean ok = true;
		try {
			if (!"".equals(filter)) {
				filter = " WHERE " + filter;
			}
			if (bounds!=null) {
				if("".equals(filter)) {
					filter = " WHERE ";
				} else {
					filter = filter + " AND ";
				}
				filter = filter + "ST_Within(ST_TRANSFORM(" + geomS + ", " + epsg + "), ST_GeomFromText('" + bounds.toText() + "', " + epsg + "))";
			}
			Connection connection = Utils.getConnection(format, inputParts, layerName);
			connection.setAutoCommit(true);
			connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
			String query = "SELECT " + idS + ",";
			if(varName!=null && !"".equals(varName)) {
				query += varName + ",";
			}
			query += "ST_AsBinary(ST_TRANSFORM(" + geomS + "," + epsg + ")) FROM " + Utils.getTableName(format, inputParts, layerName) + filter + ";";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);

			WKBReader wkbRead = new WKBReader();
			Layer layer = new Layer(layerName, bounds);
			while (rs.next()) {
				ResultSetMetaData rsmd = rs.getMetaData();
				int numColumns = rsmd.getColumnCount();
				byte[] bytes = rs.getBytes(numColumns);
				if(bytes==null) {
					System.err.println(" Object '" + rs.getLong(idS) + "' has no geometry.");
					ok = false;
					continue;
				}
				Geometry geom = wkbRead.read(bytes);
				double var = 1;
				if(varName!=null && !"".equals(varName)) {
					var = rs.getDouble(numColumns-1);
				}
				long id = rs.getLong(idS);
				LayerObject o = new LayerObject(id, var, geom);
				layer.addObject(o);
				// check for duplicates
				if(seen.contains(id)) {
					System.err.println("Duplicate object '" + id + "' occurred.");
					ok = false;
					continue;
				}
				seen.add(id);
			}
			rs.close();
			s.close();
			connection.close();
			return ok ? layer : null;
		} catch (SQLException | ParseException e) {
			throw new IOException(e);
		}
	}
	
		
	/**
	 * @brief Loads a set of objects from a CVS-file
	 * 
	 * @param layerName The name of the layer to generate
	 * @param bounds The bounds to clip the read thing to
	 * @param fileName The name of the file to read
	 * @param dismissWeight Whether the weight shall be discarded
	 * @return The generated layer with the read objects
	 * @throws IOException When something fails
	 */
	private static Layer loadLayerFromCSVFile(String layerName, Geometry bounds, String fileName, boolean dismissWeight) throws IOException { 
		Layer layer = new Layer(layerName, bounds);
		GeometryFactory gf = new GeometryFactory(new PrecisionModel());
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		boolean dismissWeightReported = false;
		Set<Long> seen = new HashSet<Long>();
		boolean ok = true;
		do {
			line = br.readLine();
			if(line==null || line.length()==0 || line.charAt(0)=='#') {
				continue;
			}
			String[] vals = line.split(";");
			Long id = 0l;
			try {
				id = Long.parseLong(vals[0]);
			} catch(NumberFormatException e) {
				System.err.println("Could not parse object id '" + vals[0] + "' to long.");
				ok = false;
				continue;
			}
			Vector<Coordinate> geom = new Vector<>();
			int i = 1;
			boolean hadError = false; 
			for(; i<vals.length-1; i+=2) {
				try {
					geom.add(new Coordinate(Double.parseDouble(vals[i]), Double.parseDouble(vals[i+1])));
				} catch(NumberFormatException e) {
					System.err.println("Broken geometry in object '" + id + "'.");
					ok = false;
					hadError = true; 
					continue;
				}
			}
			if(geom.size()==0) {
				if(!hadError) {
					System.err.println("Missing geometry for object '" + id + "'.");
				}
				ok = false;
				continue;
			}
			Geometry geom2 = null;
			if(geom.size()==1) {
				geom2 = gf.createPoint(geom.get(0));
			} else {
				if(!geom.get(0).equals(geom.get(geom.size()-1))) {
					geom.add(geom.get(0));
				}
				Coordinate[] arr = new Coordinate[geom.size()];
				geom2 = gf.createPolygon(geom.toArray(arr));
			}
			double var = 1;
			if(i<vals.length) {
				if(!dismissWeight) {
					try {
						var = Double.parseDouble(vals[i]);
					} catch(NumberFormatException e) {
						System.err.println("Could not parse object's '" + id + "' variable to double.");
						ok = false;
						continue;
					}
				} else {
					if(!dismissWeightReported) {
						dismissWeightReported = true;
						System.out.println("Warning: the weight option is not used as no aggregation takes place.");
					}
				}
			}
			layer.addObject(new LayerObject(id, var, geom2));
			// check for duplicates
			if(seen.contains(id)) {
				System.err.println("Duplicate object '" + id + "' occurred.");
				ok = false;
			}
			seen.add(id);
	    } while(line!=null);
		br.close();
		return ok ? layer : null;
	}
	
		
	/**
	 * @brief Loads a set of objects from a WKT-file
	 * 
	 * @param layerName The name of the layer to generate
	 * @param bounds The bounds to clip the read thing to
	 * @param fileName The name of the file to read
	 * @param dismissWeight Whether the weight shall be discarded
	 * @return The generated layer with the read objects
	 * @throws IOException When something fails
	 */
	private static Layer loadLayerFromWKTFile(String layerName, Geometry bounds, String fileName, boolean dismissWeight) throws IOException { 
		Set<Long> seen = new HashSet<Long>();
		boolean ok = true;
			Layer layer = new Layer(layerName, bounds);
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			WKTReader wktReader = new WKTReader();
			String line = null;
			boolean dismissWeightReported = false;
			do {
				line = br.readLine();
				if(line==null || line.length()==0 || line.charAt(0)=='#') {
					continue;
				}
				String[] vals = line.split(";");
				Long id = 0l;
				try {
					id = Long.parseLong(vals[0]);
				} catch(NumberFormatException e) {
					System.err.println("Could not parse object id '" + vals[0] + "' to long.");
					ok = false;
					continue;
				}
				Geometry geom = null;
				try {
					geom = wktReader.read(vals[1]);
				} catch (ParseException e) {
					System.err.println("Broken geometry in object '" + id + "'.");
					ok = false;
					continue;
				}
				double var = 1;
				if(vals.length==3) {
					if(!dismissWeight) {
						try {
							var = Double.parseDouble(vals[2]);
						} catch(NumberFormatException e) {
							System.err.println("Could not parse object's '" + id + "' variable to double.");
							ok = false;
							continue;
						}
					} else {
						if(!dismissWeightReported) {
							dismissWeightReported = true;
							System.out.println("Warning: the weight option is not used as no aggregation takes place.");
						}
					}
				}
				layer.addObject(new LayerObject(id, var, geom));
				// check for duplicates
				if(seen.contains(id)) {
					System.err.println("Duplicate object '" + id + "' occurred.");
					ok = false;
				}
				seen.add(id);
		    } while(line!=null);
			br.close();
			return ok ? layer : null;
	}
	

	/**
	 * @brief Loads a set of objects from a SUMO-POI-file
	 * 
	 * @param layerName The name of the layer to generate
	 * @param bounds The bounds to clip the read thing to
	 * @param fileName The name of the file to read
	 * @param dismissWeight Whether the weight shall be discarded
	 * @return The generated layer with the read objects
	 * @throws IOException When something fails
	 */
	private static Layer loadLayerFromSUMOPOIs(String layerName, Geometry bounds, String fileName, boolean dismissWeight) throws IOException {
		try {
			Layer layer = new Layer(layerName, bounds);
	        SAXParserFactory spf = SAXParserFactory.newInstance();
	        spf.setNamespaceAware(true);
	        SAXParser saxParser;
				saxParser = spf.newSAXParser();
	        XMLReader xmlReader = saxParser.getXMLReader();
	        xmlReader.setContentHandler(new SUMOLayerHandler(layer));
	        xmlReader.parse(fileName);
	        return layer;
		} catch (ParserConfigurationException | SAXException e) {
			throw new IOException(e);
		}
	}
	
	
	
	
	// --------------------------------------------------------
	// entrainment loading
	// --------------------------------------------------------
	/** @brief Loads the entrainment map
	 * @param options The options to read the source definition from
	 * @return The loaded entrainment map
	 * @throws IOException When something fails
	 */
	public static EntrainmentMap loadEntrainment(OptionsCont options) throws IOException {
		String d = options.getString("entrainment");
		Utils.Format format = Utils.getFormat(d);
		String[] inputParts = Utils.getParts(format, d, "od-output");
		switch(format) {
		case FORMAT_POSTGRES:
		case FORMAT_SQLITE:
			return loadEntrainmentFromDB(format, inputParts);
		case FORMAT_CSV:
			return loadEntrainmentFromCSVFile(inputParts[0]);
		case FORMAT_WKT:
		case FORMAT_SHAPEFILE:
		case FORMAT_GEOPACKAGE:
		case FORMAT_SUMO:
			throw new IOException("Reading entrainment from " + Utils.getFormatMMLName(format) + " is not supported.");
		default:
			throw new IOException("Could not recognize the format used for 'entrainment'.");
		}
	}
	
	
	/** @brief Loads the entrainment map from a db
	 * @param format The source format
	 * @param inputParts The source definition
	 * @return The loaded entrainment map
	 * @throws IOException When something fails
	 */
	private static EntrainmentMap loadEntrainmentFromDB(Utils.Format format, String[] inputParts) throws IOException {
		// db jars issue, see https://stackoverflow.com/questions/999489/invalid-signature-file-when-attempting-to-run-a-jar
		try {
			Class.forName("org.sqlite.JDBC");
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			Connection connection = Utils.getConnection(format, inputParts, "entrainment");
			connection.setAutoCommit(true);
			connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			String query = "SELECT carrier,carrier_subtype,carried FROM " + Utils.getTableName(format, inputParts, "entrainment") + ";";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
			EntrainmentMap em = new EntrainmentMap();
			while (rs.next()) {
				Mode carried = Modes.getMode(rs.getString("carried"));
				if(carried==null) {
					System.err.println("Error: trying to entrain unknown mode '" + rs.getString("carried") + "'.");
					continue;
				}
				em.add(""+rs.getString("carrier")+rs.getInt("carrier_subtype"), carried.id);
			}
			rs.close();
			s.close();
			connection.close();
			return em;
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}


	/** @brief Loads the entrainment map from a csv file
	 * @param fileName The name of the file to read
	 * @return The loaded entrainment map
	 * @throws IOException When something fails
	 */
	private static EntrainmentMap loadEntrainmentFromCSVFile(String fileName) throws IOException {
		EntrainmentMap em = new EntrainmentMap();
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		do {
			line = br.readLine();
			if(line==null || line.length()==0 || line.charAt(0)=='#') {
				continue;
			}
			String[] vals = line.split(";");
			if(vals.length!=3) {
				System.err.println("Error: entrainment definition should contain <CARRIER>;<CARRIER_SUBMODE>;<CARRIED_MODE>.");
				continue;
			}
			Mode carried = Modes.getMode(vals[2]);
			if(carried==null) {
				System.err.println("Error: trying to entrain unknown mode '" + vals[2] + "'.");
				continue;
			}
			em.add(vals[0]+vals[1], carried.id);
	    } while(line!=null);
		br.close();
		return em;
	}


	
	// --------------------------------------------------------
	// geometry loading
	// --------------------------------------------------------
	public static Geometry getGeometry(String def, String what, int epsg) throws IOException {
		if("".equals(def)) {
			return null;
		}
		String[] vals = def.split(",");
		if(vals.length==4) {
			Vector<Double> bounds = new Vector<>();
			for(int i=0; i<4; ++i) {
				try {
					double d = Double.parseDouble(vals[i]);
					bounds.add(d);
				} catch(NumberFormatException e) {
					throw new IOException("Could not parse bounds for '" + what + "'.");
				}
			}
			GeometryFactory gf = new GeometryFactory(new PrecisionModel());
			Envelope env = new Envelope(bounds.get(0), bounds.get(2), bounds.get(1), bounds.get(3));
			return gf.toGeometry(env);
		}
		return loadGeometry(def, what, epsg);
	}
	
	
	
	
	
	/** @brief Loads a geometry
	 * @param def The source definition (unparsed)
	 * @param what The name of the loaded thing for reporting
	 * @param epsg The used projection
	 * @return The loaded geometry
	 * @throws IOException When something fails
	 */
	private static Geometry loadGeometry(String def, String what, int epsg) throws IOException {
		Utils.Format format = Utils.getFormat(def);
		String[] inputParts = Utils.getParts(format, def, "od-output");
		switch(format) {
		case FORMAT_POSTGRES:
		case FORMAT_SQLITE:
			return loadGeometryFromDB(format, inputParts, epsg, what);
		case FORMAT_CSV:
			return loadGeometryFromCSVFile(inputParts[0]);
		case FORMAT_WKT:
			return loadGeometryFromWKTFile(inputParts[0]);
		case FORMAT_SHAPEFILE:
		case FORMAT_GEOPACKAGE:
		case FORMAT_SUMO:
			throw new IOException("Reading '" + what + "' from " + Utils.getFormatMMLName(format) + " is not supported.");
		default:
			throw new IOException("Could not recognize the format used for '" + what + "'.");
		}
	}
	
	
	/** @brief Loads a geometry from a db
	 * @param format The source format
	 * @param inputParts The source definition
	 * @param epsg The used projection
	 * @param what The name of the loaded thing for reporting
	 * @return The loaded geometry
	 * @throws IOException When something fails
	 */
	private static Geometry loadGeometryFromDB(Utils.Format format, String[] inputParts, int epsg, String what) throws IOException {
		// db jars issue, see https://stackoverflow.com/questions/999489/invalid-signature-file-when-attempting-to-run-a-jar
		try {
			Class.forName("org.sqlite.JDBC");
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			Connection connection = Utils.getConnection(format, inputParts, what);
			connection.setAutoCommit(true);
			connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
			String query = "SELECT ST_AsBinary(ST_TRANSFORM(geom," + epsg + ")) FROM " + Utils.getTableName(format, inputParts, what) + ";";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
			Geometry geom = null;
			WKBReader wkbRead = new WKBReader();
			if (rs.next()) {
				geom = wkbRead.read(rs.getBytes(1));
			}
			rs.close();
			s.close();
			connection.close();
			return geom;
		} catch (SQLException e) {
			throw new IOException(e);
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}
	
	
	/** @brief Loads a geometry from a csv file
	 * @param fileName The file to read the geometry from
	 * @return The loaded geometry
	 * @throws IOException When something fails
	 */
	private static Geometry loadGeometryFromCSVFile(String fileName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = br.readLine();
		while(line!=null && (line.length()==0 || line.charAt(0)=='#')) {
			line = br.readLine();
		}
		br.close();
		String[] vals = line.split(";");
		if((vals.length % 2)!=0) {
			throw new IOException("Could not load geometry from '" + fileName + "': odd number for coordinates.");
		}
		Coordinate[] coords = new Coordinate[(int) (vals.length/2)];
		int j = 0;
		for(int i=0; i<vals.length; i+=2, ++j) {
			coords[j] = new Coordinate(Double.parseDouble(vals[i]), Double.parseDouble(vals[i+1]));
	    }
		GeometryFactory gf = new GeometryFactory(new PrecisionModel());
		return gf.createPolygon(coords);
	}
	
	
	/** @brief Loads a geometry from a WKT file
	 * @param fileName The file to read the geometry from
	 * @return The loaded geometry
	 * @throws IOException When something fails
	 */
	private static Geometry loadGeometryFromWKTFile(String fileName) throws IOException {
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line = br.readLine();
			while(line!=null && (line.length()==0 || line.charAt(0)=='#')) {
				line = br.readLine();
			}
			br.close();
			WKTReader wktReader = new WKTReader();
			return wktReader.read(line);
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}
	
	
	// --------------------------------------------------------
	// od-connections loading
	// --------------------------------------------------------
	/** @brief Loads od-connections
	 * @param def The source definition (unparsed)
	 * @return The loaded od-connections
	 * @throws IOException When something fails
	 */
	public static Vector<DBODRelation> loadODConnections(String def) throws IOException {
		Utils.Format format = Utils.getFormat(def);
		String[] inputParts = Utils.getParts(format, def, "od-connections");
		switch(format) {
		case FORMAT_POSTGRES:
		case FORMAT_SQLITE:
			return loadODConnectionsFromDB(format, inputParts);
		case FORMAT_CSV:
			return loadODConnectionsFromCSVFile(inputParts[0]);
		case FORMAT_WKT:
		case FORMAT_SHAPEFILE:
		case FORMAT_GEOPACKAGE:
		case FORMAT_SUMO:
			throw new IOException("Reading 'od-connections' from " + Utils.getFormatMMLName(format) + " is not supported.");
		default:
			throw new IOException("Could not recognize the format used for 'od-connections'.");
		}
	}


	/** @brief Loads od-connections from a database
	 * @param format The used format
	 * @param inputParts The source definition
	 * @return The loaded od-connections
	 * @throws IOException When something fails
	 */
	private static Vector<DBODRelation> loadODConnectionsFromDB(Utils.Format format, String[] inputParts) throws IOException {
		// db jars issue, see https://stackoverflow.com/questions/999489/invalid-signature-file-when-attempting-to-run-a-jar
		try {
			Class.forName("org.sqlite.JDBC");
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			Connection connection = Utils.getConnection(format, inputParts, "od-connections");
			connection.setAutoCommit(true);
			connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			String query = "SELECT origin,destination FROM " + Utils.getTableName(format, inputParts, "od-connections") + ";";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
			Vector<DBODRelation> ret =  new Vector<>();
			while (rs.next()) {
				ret.add(new DBODRelation(rs.getLong("origin"), rs.getLong("destination"), 1.));
			}
			rs.close();
			s.close();
			connection.close();
			return ret;
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}


	/** @brief Loads od-connections from a csv file
	 * @param fileName The name of the file to read od-connections from
	 * @return The loaded od-connections
	 * @throws IOException When something fails
	 */
	private static Vector<DBODRelation> loadODConnectionsFromCSVFile(String fileName) throws IOException {
		Vector<DBODRelation> ret =  new Vector<>();
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		do {
			line = br.readLine();
			if(line==null || line.length()==0 || line.charAt(0)=='#') {
				continue;
			}
			String[] vals = line.split(";");
			try {
				ret.add(new DBODRelation(Long.parseLong(vals[0]), Long.parseLong(vals[1]), 1.));
			} catch(NumberFormatException e) {
				System.err.println("Broken o/d relation in '" + fileName + "': " + line + ".");
			}
	    } while(line!=null);
		br.close();
		return ret;
	}

	
	// --------------------------------------------------------
	// mode change locations loading
	// --------------------------------------------------------
	/** @brief Loads mode change locations
	 * @param def The source definition (unparsed)
	 * @return Whether the locations could be loaded
	 * @throws IOException When something fails
	 */
	public static boolean loadModeChangeLocations(String def, DBNet net) throws IOException {
		Utils.Format format = Utils.getFormat(def);
		String[] inputParts = Utils.getParts(format, def, "od-connections");
		switch(format) {
		case FORMAT_POSTGRES:
		case FORMAT_SQLITE:
			return loadModeChangeLocationsFromDB(format, inputParts, net);
		case FORMAT_CSV:
			return loadModeChangeLocationsFromCSVFile(inputParts[0], net);
		case FORMAT_WKT:
		case FORMAT_SHAPEFILE:
		case FORMAT_GEOPACKAGE:
		case FORMAT_SUMO:
			throw new IOException("Reading 'od-connections' from " + Utils.getFormatMMLName(format) + " is not supported.");
		default:
			throw new IOException("Could not recognize the format used for 'od-connections'.");
		}
	}


	/** @brief Loads mode change locations from a database
	 * @param format The used format
	 * @param inputParts The source definition
	 * @param net The road network to add the mode change locations to
	 * @return Whether the locations could be loaded
	 * @throws IOException When something fails
	 */
	private static boolean loadModeChangeLocationsFromDB(Utils.Format format, String[] inputParts, DBNet net) throws IOException {
		// db jars issue, see https://stackoverflow.com/questions/999489/invalid-signature-file-when-attempting-to-run-a-jar
		try {
			Class.forName("org.sqlite.JDBC");
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		boolean hadError = false;
		try {
			Connection connection = Utils.getConnection(format, inputParts, "mode-changes");
			connection.setAutoCommit(true);
			connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			String query = "SELECT node_id,from_mode,to_mode,duration,price FROM " + Utils.getTableName(format, inputParts, "mode-changes") + ";";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
			while (rs.next()) {
				DBNode node = net.getExistingNode(rs.getString("node_id"));
				if (node!=null) {
					node.addModeChange(rs.getLong("from_mode"), rs.getLong("to_mode"), rs.getDouble("duration"), rs.getDouble("price"));
				} else {
					System.err.println("Trying to add a mode change to a non-existing node '" + rs.getString("node_id") + "'.");
					hadError = true;
				}
			}
			rs.close();
			s.close();
			connection.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
		return !hadError;
	}


	/** @brief Loads mode change locations from a csv file
	 * @param fileName The name of the file to read od-connections from
	 * @param net The road network to add the mode change locations to
	 * @return Whether the locations could be loaded
	 * @throws IOException When something fails
	 */
	private static boolean loadModeChangeLocationsFromCSVFile(String fileName, DBNet net) throws IOException {
		boolean hadError = false;
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		do {
			line = br.readLine();
			if(line==null || line.length()==0 || line.charAt(0)=='#') {
				continue;
			}
			String[] vals = line.split(";");
			try {
				DBNode node = net.getExistingNode(vals[0]);
				if(node!=null) {
					long fromModeID = Modes.getMode(vals[1]).id; // !!! catch errors
					long toModeID = Modes.getMode(vals[2]).id; // !!! catch errors
					node.addModeChange(fromModeID, toModeID, Double.parseDouble(vals[3]), Double.parseDouble(vals[4]));
				} else {
					System.err.println("Trying to add a mode change to a non-existing node '" + vals[0] + "'.");
					hadError = true;
				}
			} catch(NumberFormatException e) {
				System.err.println("Broken o/d relation in '" + fileName + "': " + line + ".");
			}
	    } while(line!=null);
		br.close();
		return !hadError;
	}
	
	
	
}

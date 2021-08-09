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
 * Rutherfordstraße 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.mosaic.GeoPackageReader;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.postgresql.PGConnection;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.dks.utils.options.OptionsCont;
import de.dlr.ivf.urmo.router.modes.EntrainmentMap;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBODRelation;
import de.dlr.ivf.urmo.router.shapes.IDGiver;
import de.dlr.ivf.urmo.router.shapes.Layer;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class DBIOHelper
 * @brief Some helper methods for loading data
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of Transport Research
 */
public class InputReader {
	// --------------------------------------------------------
	// epsg determination
	// --------------------------------------------------------
	/**
	 * @brief Finds the correct UTM-zone for the given net. Reference is the most south most west point in the from-locations.
	 * 
	 * The calculation is based on utm-zones for longitudes from -180 to 180 degrees.
	 * The latitude is only valid from -84 to 84 degrees.
	 * The returned UTM-zones start with 32500 for the southern hemisphere and with 32600 for the northern hemisphere.
	 * @param[in] options The command line options 
	 * @return The epsg-code of the UTM-zone or -1 of no UTM-zone could be found (e.g. north-pole )
	 * @throws SQLException
	 * @throws ParseException
	 * @throws IOException 
	 */
	public static int findUTMZone(OptionsCont options) throws IOException {
		String[] r = Utils.checkDefinition(options.getString("from"), "from");
		if (!r[0].equals("db")) {
			return 0;
		}
		try {
			Connection connection = DriverManager.getConnection(r[1], r[3], r[4]);
			connection.setAutoCommit(true);
			connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
			String geomString = options.getString("from.geom");
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
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
	
	

	// --------------------------------------------------------
	// loading layers
	// --------------------------------------------------------
	/**
	 * @brief Loads a set of objects (a layer)
	 * 
	 * @param options The command line options 
	 * @param base The layer/type ("from", "to") of the objects to load
	 * @param varName Name of the variable field
	 * @param dismissWeight Whether the weight shall be discarded
	 * @param idGiver An instance to retrieve new ids from
	 * @param epsg The used projection
	 * @return The generated layer with the read objects
	 * @throws IOException
	 */
	public static Layer loadLayer(OptionsCont options, String base, String varName, boolean dismissWeight, IDGiver idGiver, int epsg) throws IOException {
		String filter = options.isSet(base+".filter") ? options.getString(base + ".filter") : ""; // !!! use something different
		varName = varName==null ? null : options.getString(varName);
		String[] r = Utils.checkDefinition(options.getString(base), base);
		if (r[0].equals("db")) {
			try {
				return loadLayerFromDB(base, r[1], r[2], r[3], r[4], filter, varName, options.getString(base + ".id"), options.getString(base + ".geom"), idGiver, epsg);
			} catch (SQLException | ParseException e) {
				throw new IOException(e);
			}
		} else if (r[0].equals("file") || r[0].equals("csv")) {
			try {
				return loadLayerFromCSVFile(base, r[1], idGiver, dismissWeight);
			} catch (ParseException | IOException e) {
				throw new IOException(e);
			}
		} else if (r[0].equals("shp")) {
			try {
				return loadLayerFromShapefile(base, r[1], varName, options.getString(base + ".id"), options.getString(base + ".geom"), idGiver, epsg);
			} catch (MismatchedDimensionException | ParseException | IOException | FactoryException | TransformException e) {
				throw new IOException(e);
			}
		}  else if (r[0].equals("sumo")) {
			try {
				return loadLayerFromSUMOPOIs(base, r[1], idGiver);
			} catch (MismatchedDimensionException | IOException | ParserConfigurationException | SAXException e) {
				throw new IOException(e);
			}
		} else {
			throw new IOException("The prefix '" + r[0] + "' is not known.");
		}
	}
	
	
	
	/**
	 * @brief Loads a set of objects from the db
	 * 
	 * @param layerName The layer/type ("from", "to") of the objects to load
	 * @param url The url of the database
	 * @param table The table to read from
	 * @param user The user name for connecting to the database
	 * @param pw The user's password
	 * @param filter A WHERE-clause statement (optional, empty string if not used)
	 * @param varName The name of the attached variable
	 * @param idS The name of the column to read the IDs from
	 * @param geomS The name of the column to read the geometry from
	 * @param idGiver A reference to something that supports a running ID
	 * @param epsg The EPSG of the projection to use
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
		if(varName!=null && !"".equals(varName)) {
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
			if(varName!=null && !"".equals(varName)) {
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
	 * @brief Loads a set of objects from a CVS-file
	 * 
	 * @param layerName The name of the layer to generate
	 * @param fileName The name of the file to read
	 * @param idGiver A reference to something that supports a running ID
	 * @param dismissWeight Whether the weight shall be discarded
	 * @return The generated layer with the read objects
	 * @throws IOException
	 * @throws ParseException
	 */
	private static Layer loadLayerFromCSVFile(String layerName, String fileName, IDGiver idGiver, boolean dismissWeight) throws ParseException, IOException { 
		Layer layer = new Layer(layerName);
		GeometryFactory gf = new GeometryFactory(new PrecisionModel());
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		boolean dismissWeightReported = false;
		do {
			line = br.readLine();
			if(line!=null && line.length()!=0 && line.charAt(0)!='#') {
				String[] vals = line.split(";");
				Vector<Coordinate> geom = new Vector<>();
				int i = 1;
				for(; i<vals.length-1; i+=2) {
					geom.add(new Coordinate(Double.parseDouble(vals[i]), Double.parseDouble(vals[i+1])));
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
						var = Double.parseDouble(vals[i]);
					} else {
						if(!dismissWeightReported) {
							dismissWeightReported = true;
							System.out.println("Warning: the weight option is not used as no aggregation takes place.");
						}
					}
				}
				layer.addObject(new LayerObject(idGiver.getNextRunningID(), Long.parseLong(vals[0]), var, geom2));
			}
	    } while(line!=null);
		br.close();
		return layer;
	}
	
	
	
	/**
	 * @brief Loads a set of objects from a shapefile
	 * 
	 * @param layerName The name of the layer to generate
	 * @param fileName The name of the file to read
	 * @param varName The name of the attached variable
	 * @param idS The name of the column to read the IDs from
	 * @param geomS The name of the column to read the geometry from
	 * @param idGiver A reference to something that supports a running ID
	 * @param epsg The EPSG of the projection to use
	 * @return The generated layer with the read objects
	 * @throws ParseException
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 * @throws MismatchedDimensionException
	 * @throws TransformException
	 */
	private static Layer loadLayerFromShapefile(String layerName, String fileName, String varName,
			String idS, String geomS, IDGiver idGiver, int epsg) throws ParseException, IOException, NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException { 
		Layer layer = new Layer(layerName);
		File file = new File(fileName);
		if(!file.exists() || !fileName.endsWith(".shp")) {
		    throw new IOException("Invalid shapefile filepath: " + fileName);
		}
		ShapefileDataStore dataStore = new ShapefileDataStore(file.toURL());
		SimpleFeatureSource featureSource = dataStore.getFeatureSource();
		SimpleFeatureCollection featureCollection = featureSource.getFeatures();

		SimpleFeatureType schema = featureSource.getSchema();
		CoordinateReferenceSystem dataCRS = schema.getCoordinateReferenceSystem();
        CoordinateReferenceSystem worldCRS = CRS.decode("EPSG:" + epsg);
        boolean lenient = true; // allow for some error due to different datums
        MathTransform transform = CRS.findMathTransform(dataCRS, worldCRS, lenient);		
		
		SimpleFeatureIterator iterator = featureCollection.features();
		while(iterator.hasNext()) {
		    SimpleFeature feature = iterator.next();
		    Geometry g = (Geometry) feature.getAttribute(geomS);
		    Geometry geom = JTS.transform(g, transform);
		    Integer id = (Integer) feature.getAttribute(idS);
			double var = 1;
			if(varName!=null && !"".equals(varName)) {
				var = (Double) feature.getAttribute(varName);
			}
			LayerObject o = new LayerObject(idGiver.getNextRunningID(), id, var, geom);
			layer.addObject(o);
		}
		return layer;
	}
	

	/**
	 * @brief Loads a set of objects from a SUMO-POI-file
	 * 
	 * @param layerName The name of the layer to generate
	 * @param fileName The name of the file to read
	 * @param idGiver A reference to something that supports a running ID
	 * @return The generated layer with the read objects
	 * @throws IOException
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	private static Layer loadLayerFromSUMOPOIs(String layerName, String fileName, IDGiver idGiver) throws ParserConfigurationException, SAXException, IOException { 
		Layer layer = new Layer(layerName);
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(new SUMOLayerHandler(layer, idGiver));
        xmlReader.parse(fileName);
        return layer;
	}
	
	
	
	/**
	 * @brief Loads a set of objects from a Geopackage file
	 * 
	 * @param layerName The name of the layer to generate
	 * @param fileName The name of the file to read
	 * @param idGiver A reference to something that supports a running ID
	 * @return The generated layer with the read objects
	 * @throws IOException
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	private static Layer loadLayerFromGPKG(String layerName, String fileName, IDGiver idGiver) throws ParserConfigurationException, SAXException, IOException { 
		Layer layer = new Layer(layerName);
		GeoPackage geoPackage = new GeoPackage(new File(fileName));
		GeoPackageReader reader = new GeoPackageReader(fileName, null);
		GeneralParameterValue[] parameters = new GeneralParameterValue[1];
        return layer;
	}
	
	
	
	// --------------------------------------------------------
	// entrainment loading
	// --------------------------------------------------------
	public static EntrainmentMap loadEntrainment(OptionsCont options)  throws IOException {
		String[] r = Utils.checkDefinition(options.getString("entrainment"), "entrainment");
		if (r[0].equals("db")) {
			try {
				return loadEntrainmentFromDB(r[1], r[2], r[3], r[4]);
			} catch (SQLException | ParseException e) {
				throw new IOException(e);
			}
		} else if (r[0].equals("file") || r[0].equals("csv")) {
			return loadEntrainmentFromCSVFile(r[1]);
		} else {
			throw new IOException("The prefix '" + r[0] + "' is not known or does not support entrainment.");
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


	
	private static EntrainmentMap loadEntrainmentFromCSVFile(String fileName) throws IOException {
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


	
	// --------------------------------------------------------
	// geometry loading
	// --------------------------------------------------------
	public static Geometry loadGeometry(String def, String what, int epsg)  throws IOException {
		String[] r = Utils.checkDefinition(def, what);
		if (r[0].equals("db")) {
			try {
				return loadGeometryFromDB(r[1], r[2], r[3], r[4], epsg);
			} catch (SQLException | ParseException e) {
				throw new IOException(e);
			}
		} else if (r[0].equals("file") || r[0].equals("csv")) {
			try {
				return loadGeometryFromCSVFile(r[1]);
			} catch (ParseException | IOException e) {
				throw new IOException(e);
			}
		} else if (r[0].equals("shp")) {
			try {
				return loadGeometryFromShapefile(r[1], epsg);
			} catch (MismatchedDimensionException | ParseException | IOException | FactoryException | TransformException e) {
				throw new IOException(e);
			}
		} else {
			throw new IOException("The prefix '" + r[0] + "' is not known or does not support geometry.");
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
	
	
	private static Geometry loadGeometryFromCSVFile(String fileName) throws ParseException, IOException {
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
	
	
	
	private static Geometry loadGeometryFromShapefile(String fileName, int epsg) throws ParseException, IOException, NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException {
		File file = new File(fileName);
		if(!file.exists() || !fileName.endsWith(".shp")) {
		    throw new IOException("Invalid shapefile filepath: " + fileName);
		}
		ShapefileDataStore dataStore = new ShapefileDataStore(file.toURL());
		SimpleFeatureSource featureSource = dataStore.getFeatureSource();
		SimpleFeatureCollection featureCollection = featureSource.getFeatures();

		SimpleFeatureType schema = featureSource.getSchema();
		CoordinateReferenceSystem dataCRS = schema.getCoordinateReferenceSystem();
        CoordinateReferenceSystem worldCRS = CRS.decode("EPSG:" + epsg);
        boolean lenient = true; // allow for some error due to different datums
        MathTransform transform = CRS.findMathTransform(dataCRS, worldCRS, lenient);		
		
		SimpleFeatureIterator iterator = featureCollection.features();
		while(iterator.hasNext()) {
		    SimpleFeature feature = iterator.next();
		    Geometry g = (Geometry) feature.getAttribute("the_geom");
		    Geometry geom = JTS.transform(g, transform);
		    // !!! clean up
		    return geom;
		}
		throw new IOException("Could not load geometry from '" + fileName + "'.");
	}
	
	
	
	// --------------------------------------------------------
	// od-connections loading
	// --------------------------------------------------------
	public static Vector<DBODRelation> loadODConnections(String def) throws IOException {
		String[] r = Utils.checkDefinition(def, "od-connections");
		if (r[0].equals("db")) {
			try {
				return loadODConnectionsDB(r[1], r[2], r[3], r[4]);
			} catch (SQLException | ParseException e) {
				throw new IOException(e);
			}
		} else if (r[0].equals("file") || r[0].equals("csv")) {
			return loadODConnectionsFromCSVFile(r[1]);
		} else {
			throw new IOException("The prefix '" + r[0] + "' is not known or does not support geometry.");
		}
	}


	private static Vector<DBODRelation> loadODConnectionsDB(String url, String table, String user, String pw) throws SQLException, ParseException {
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		String query = "SELECT origin,destination FROM " + table + ";";
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
	}



	private static Vector<DBODRelation> loadODConnectionsFromCSVFile(String fileName) throws IOException {
		Vector<DBODRelation> ret =  new Vector<>();
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		do {
			line = br.readLine();
			if(line!=null && line.length()!=0 && line.charAt(0)!='#') {
				String[] vals = line.split(";");
				ret.add(new DBODRelation(Long.parseLong(vals[0]), Long.parseLong(vals[1]), 1.));
			}
	    } while(line!=null);
		br.close();
		return ret;
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

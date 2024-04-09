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
import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.dlr.ivf.urmo.router.spring.mode.AlternativeMode;
import de.dlr.ivf.urmo.router.spring.model.NetWrapper;
import de.dlr.ivf.urmo.router.spring.net.Edge;
import de.dlr.ivf.urmo.router.spring.net.Node;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.postgresql.PGConnection;
import org.xml.sax.SAXException;

import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.DBNode;
import de.dlr.ivf.urmo.router.shapes.IDGiver;

/**
 * @class NetLoader
 * @brief Loads the road network stored in a database or files
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of Transport Research
 */
public class NetLoader {

	private static System.Logger logger = System.getLogger(NetLoader.class.getName());
	/** @brief Loads the road network
	 * @param idGiver Instance supporting running ids 
	 * @param def Source definition
	 * @param vmaxAttr The attribute (column) to read the maximum velocity from 
	 * @param epsg The projection
	 * @param uModes The modes for which the network shall be loaded
	 * @return The loaded net
	 * @throws IOException When something fails 
	 */
	public static DBNet loadNet(IDGiver idGiver, String def, String vmaxAttr, int epsg, long uModes) throws IOException {
		Utils.Format format = Utils.getFormat(def);
		String[] inputParts = Utils.getParts(format, def, "net");
		DBNet net = null;
		switch(format) {
		case FORMAT_POSTGRES:
		case FORMAT_SQLITE:
			net = loadNetFromDB(idGiver, format, inputParts, vmaxAttr, epsg, uModes);
			break;
		case FORMAT_CSV:
			net = loadNetFromCSVFile(idGiver, inputParts[0], uModes);
			break;
		case FORMAT_WKT:
			net = loadNetFromWKTFile(idGiver, inputParts[0], uModes);
			break;
		case FORMAT_SHAPEFILE:
			net = loadNetFromShapefile(idGiver, inputParts[0], epsg, uModes);
			break;
		case FORMAT_SUMO:
			net = loadNetFromSUMOFile(idGiver, inputParts[0], uModes);
			break;
		case FORMAT_GEOPACKAGE:
			throw new IOException("Reading 'net' from " + Utils.getFormatMMLName(format) + " is not supported.");
		default:
			throw new IOException("Could not recognize the format used for GTFS.");
		}
		// add other directions to mode foot
		if(net!=null) {
			net.extendDirections();
		}
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
	private static DBNet loadNetFromDB(IDGiver idGiver, Utils.Format format, String[] inputParts, String vmax, int epsg, long uModes) throws IOException {
		try {
			Connection connection = Utils.getConnection(format, inputParts, "net");
			connection.setAutoCommit(true);
			connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
			String query = "SELECT oid,nodefrom,nodeto,mode_walk,mode_bike,mode_mit,"+vmax+",length,ST_AsBinary(ST_TRANSFORM(the_geom," + epsg + ")) FROM " + Utils.getTableName(format, inputParts, "net") + ";";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
			WKBReader wkbRead = new WKBReader();
			DBNet net = new DBNet(idGiver);
			boolean ok = true;
			while (rs.next()) {
				long modes = 0;
				if(rs.getBoolean("mode_walk")) modes = modes | Modes.getMode("foot").id;
				if(rs.getBoolean("mode_bike")) modes = modes | Modes.getMode("bicycle").id;
				if(rs.getBoolean("mode_mit")) modes = modes | Modes.getMode("passenger").id;
				modes = (modes&Modes.customAllowedAt)!=0 ? modes | Modes.getMode("custom").id : modes;
				//if(rs.getBoolean("mode_walk") || rs.getBoolean("mode_bike")) modes = modes | Mode.getMode("e-scooter").id;
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
				ok &= net.addEdge(net.getNextID(), rs.getString("oid"), fromNode, toNode, modes, rs.getDouble(vmax) / 3.6, geom2, rs.getDouble("length"));
			}
			rs.close();
			s.close();
			connection.close();
			return net;
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
	public static DBNet loadNetFromCSVFile(IDGiver idGiver, String fileName, long uModes) throws IOException {
		DBNet net = new DBNet(idGiver);
		GeometryFactory gf = new GeometryFactory(new PrecisionModel());
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		boolean ok = true;
		do {
			line = br.readLine();
			if(line!=null && line.length()!=0 && line.charAt(0)!='#') {
				String[] vals = line.split(";");
				long modes = 0;
				if("t".equals(vals[3].toLowerCase()) || "true".equals(vals[3].toLowerCase()) || "1".equals(vals[3])) modes = modes | Modes.getMode("foot").id;
				if("t".equals(vals[4].toLowerCase()) || "true".equals(vals[4].toLowerCase()) || "1".equals(vals[4])) modes = modes | Modes.getMode("bicycle").id;
				if("t".equals(vals[5].toLowerCase()) || "true".equals(vals[5].toLowerCase()) || "1".equals(vals[5])) modes = (modes | Modes.getMode("passenger").id) | Modes.getMode("bus").id;
				modes = (modes&Modes.customAllowedAt)!=0 ? modes | Modes.getMode("custom").id : modes;
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
				ok &= net.addEdge(net.getNextID(), vals[0], fromNode, toNode, modes, Double.parseDouble(vals[6]) / 3.6, ls, Double.parseDouble(vals[7]));
			}
	    } while(line!=null);
		br.close();
		return net;
	}

	public static NetWrapper newJGraphTNet(String fileName, int minGraphSize) throws IOException {

		long nextId = 0;
		DBNet net = new DBNet();

		Map<AlternativeMode, Graph<Node,Edge>> modeNets = new HashMap<>();

		modeNets.put(AlternativeMode.FOOT, new DirectedWeightedPseudograph<>(Edge.class));
		modeNets.put(AlternativeMode.BIKESHARING, new DirectedWeightedPseudograph<>(Edge.class));
		Graph<Node,Edge> mitNet = new DirectedWeightedPseudograph<>(Edge.class);
		modeNets.put(AlternativeMode.CALLABUS, mitNet);
		modeNets.put(AlternativeMode.CARPOOLING, mitNet);

		GeometryFactory gf = new GeometryFactory(new PrecisionModel());
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		Map<Long,Node> nodeIdMap = new HashMap<>();
		Map<Long,DBEdge> dbEdgeIdMap = new HashMap<>();

		logger.log(Level.INFO, "Loading network file...");
		do {
			line = br.readLine();
			if(line!=null && line.length()!=0 && line.charAt(0)!='#') {
				Set<AlternativeMode> allowedModes = new HashSet<>();

				String[] vals = line.split(";");
				long modes = 0;
				if("t".equals(vals[3].toLowerCase()) || "true".equals(vals[3].toLowerCase()) || "1".equals(vals[3])){
					allowedModes.add(AlternativeMode.FOOT);
					modes = modes | Modes.getMode("foot").id;
				}
				if("t".equals(vals[4].toLowerCase()) || "true".equals(vals[4].toLowerCase()) || "1".equals(vals[4])){
					allowedModes.add(AlternativeMode.BIKESHARING);
					modes = modes | Modes.getMode("bicycle").id;
				}
				if("t".equals(vals[5].toLowerCase()) || "true".equals(vals[5].toLowerCase()) || "1".equals(vals[5])){
					allowedModes.add(AlternativeMode.CARPOOLING);
					allowedModes.add(AlternativeMode.CALLABUS);
					modes = (modes | Modes.getMode("passenger").id) | Modes.getMode("bus").id;
				}

				int num = vals.length - 8;
				if((num % 2)!=0) {
					throw new IOException("odd number for coordinates");
				}
				Coordinate[] coords = new Coordinate[(int) (num/2)];
				int j = 0;
				for(int i=8; i<vals.length; i+=2, ++j ) {
					coords[j] = new Coordinate(Double.parseDouble(vals[i]), Double.parseDouble(vals[i+1]));
				}
				long edgeId = Long.parseLong(vals[0]);
				long fromNodeId = Long.parseLong(vals[1]);
				long toNodeId = Long.parseLong(vals[2]);
				double maxSpeed = Double.parseDouble(vals[6]) / 3.6;
				double length = Double.parseDouble(vals[7]);

				DBNode fromNode = net.addNode(fromNodeId, coords[0]);
				DBNode toNode = net.addNode(toNodeId, coords[coords.length - 1]);

				LineString ls = gf.createLineString(coords);
				dbEdgeIdMap.put(edgeId, new DBEdge(nextId++,String.valueOf(edgeId), fromNode, toNode, modes, maxSpeed, ls, length));
				net.addEdge(nextId++, String.valueOf(edgeId), fromNode, toNode, modes, maxSpeed, ls, length);

				Node fromGraphNode = new Node(fromNodeId);
				Node toGraphNode = new Node(toNodeId);
				nodeIdMap.putIfAbsent(fromNodeId, fromGraphNode);
				nodeIdMap.putIfAbsent(toNodeId, toGraphNode);
				Edge graphEdge = new Edge(edgeId, fromNodeId, toNodeId, length, maxSpeed);

				for(AlternativeMode allowedMode : allowedModes){
					var modeNet = modeNets.get(allowedMode);
					modeNet.addVertex(fromGraphNode);
					modeNet.addVertex(toGraphNode);

					modeNet.addEdge(fromGraphNode,toGraphNode,graphEdge);
				}

			}
		} while(line!=null);
		br.close();


		//now get rid of unconnected sub graphs and create the DBNet
		logger.log(Level.INFO,"Analysing network graphs for each mode...");
		var resultingModeGraphs = removeLooseGraphs(minGraphSize, modeNets);

		logger.log(Level.INFO, "Initializing the UrMo network...");
		var dbNet = generateDbNetFromJGraphTModeNets(dbEdgeIdMap, resultingModeGraphs);

		return new NetWrapper(dbNet, modeNets, nodeIdMap);
	}

	private static DBNet generateDbNetFromJGraphTModeNets(Map<Long, DBEdge> dbEdgeIdMap, Map<AlternativeMode, Graph<Node, Edge>> resultingModeGraphs) {
		Map<Long, DBEdge> visitedEdgesToDbNet = new HashMap<>();
		resultingModeGraphs.forEach((k, v) ->{
			var edges = v.edgeSet();
			edges.forEach(edge -> visitedEdgesToDbNet.putIfAbsent(edge.edgeId(), dbEdgeIdMap.get(edge.edgeId())));
		});

		DBNet urmoNet = new DBNet();

		visitedEdgesToDbNet.forEach((k,v) -> urmoNet.addEdge(v));
		return urmoNet;
	}

	private static Map<AlternativeMode, Graph<Node,Edge>> removeLooseGraphs(int minGraphSize, Map<AlternativeMode, Graph<Node, Edge>> modeNets) {
		Map<AlternativeMode, Graph<Node,Edge>> resultingModeGraphs = new HashMap<>();
		modeNets.forEach((mode, graph) -> {

			ConnectivityInspector<Node,Edge> ci = new ConnectivityInspector(graph);
			//Test whether the graph is connected:
			ci.isConnected();

			List<Set<Node>> connectedNodesSets = ci.connectedSets();

			int connectedNodesSetCount = connectedNodesSets.size();
			int removedNodes = 0;
			int removedSets = 0;
			int remainingSetsCount = 0;
			int biggestSetElementCount = 0;

			Graph<Node,Edge> finalGraph = null;

			for(Set<Node> nodeSet : connectedNodesSets){

				int setSize = nodeSet.size();

				if(setSize < minGraphSize){
					removedSets++;
					removedNodes += setSize;
					continue;
				}

				remainingSetsCount++;

				if(biggestSetElementCount < setSize){
					biggestSetElementCount = setSize;
				}

				Graph<Node,Edge> subGraph = new AsSubgraph<>(graph, nodeSet);

				if(finalGraph == null){
					finalGraph = subGraph;
				}else{
					Graphs.addGraph(finalGraph, subGraph);
				}
			}

			resultingModeGraphs.put(mode,finalGraph);

			String logMessage = String.format("Graph for mode: %s contains %d loose graphs. After pruning: %d loose graphs and a total of %d nodes have been removed resulting in %d remaining sub graphs. Biggest graph contains %d nodes.",mode,connectedNodesSetCount, removedSets, removedNodes, remainingSetsCount,biggestSetElementCount);
			logger.log(Level.INFO, logMessage);
		});
		return resultingModeGraphs;
	}


	/** @brief Reads the network from a WKT file
	 * @param idGiver Instance supporting running ids 
	 * @param fileName The file to read the network from
	 * @param uModes The modes for which the network shall be loaded
	 * @return The loaded net
	 * @throws IOException When something fails 
	 */
	private static DBNet loadNetFromWKTFile(IDGiver idGiver, String fileName, long uModes) throws IOException {
		try {
			DBNet net = new DBNet(idGiver);
			WKTReader wktReader = new WKTReader();
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line = null;
			boolean ok = true;
			do {
				line = br.readLine();
				if(line!=null && line.length()!=0 && line.charAt(0)!='#') {
					String[] vals = line.split(";");
					long modes = 0;
					if("true".equals(vals[3].toLowerCase()) || "1".equals(vals[3])) modes = modes | Modes.getMode("foot").id;
					if("true".equals(vals[4].toLowerCase()) || "1".equals(vals[4])) modes = modes | Modes.getMode("bicycle").id;
					if("true".equals(vals[5].toLowerCase()) || "1".equals(vals[5])) modes = modes | Modes.getMode("passenger").id;
					modes = (modes&Modes.customAllowedAt)!=0 ? modes | Modes.getMode("custom").id : modes;
					if(modes==0 && ((modes&uModes)==0)) {
						continue;
					}
					LineString geom = (LineString) wktReader.read(vals[8]);
					Coordinate cs[] = geom.getCoordinates();
					DBNode fromNode = net.getNode(Long.parseLong(vals[1]), cs[0]);
					DBNode toNode = net.getNode(Long.parseLong(vals[2]), cs[cs.length - 1]);
					ok &= net.addEdge(net.getNextID(), vals[0], fromNode, toNode, modes, Double.parseDouble(vals[6]) / 3.6, geom, Double.parseDouble(vals[7]));
				}
		    } while(line!=null);
			br.close();
			return net;
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}
	
	
	/** @brief Reads the network from a shapefile
	 * @param idGiver Instance supporting running ids 
	 * @param fileName The file to read the network from
	 * @param epsg The projection
	 * @param uModes The modes for which the network shall be loaded
	 * @return The loaded net
	 * @throws IOException When something fails 
	 */
	private static DBNet loadNetFromShapefile(IDGiver idGiver, String fileName, int epsg, long uModes) throws IOException {
		try {
			File file = new File(fileName);
			if(!file.exists() || !fileName.endsWith(".shp")) {
			    throw new IOException("Invalid shapefile filepath: " + fileName);
			}
			ShapefileDataStore dataStore = new ShapefileDataStore(file.toURL());
			SimpleFeatureSource featureSource = dataStore.getFeatureSource();
			SimpleFeatureCollection featureCollection = featureSource.getFeatures();

			SimpleFeatureType schema = featureSource.getSchema();
			CoordinateReferenceSystem dataCRS = schema.getCoordinateReferenceSystem();
	        CoordinateReferenceSystem worldCRS;
				worldCRS = CRS.decode("EPSG:" + epsg);
	        boolean lenient = true; // allow for some error due to different datums
	        MathTransform transform = CRS.findMathTransform(dataCRS, worldCRS, lenient);		

			DBNet net = new DBNet(idGiver);
	        
			SimpleFeatureIterator iterator = featureCollection.features();
			boolean ok = true;
			while(iterator.hasNext()) {
			    SimpleFeature feature = iterator.next();
				long modes = 0;
				if((Boolean) feature.getAttribute("mode_walk")) modes = modes | Modes.getMode("foot").id;
				if((Boolean) feature.getAttribute("mode_bike")) modes = modes | Modes.getMode("bicycle").id;
				if((Boolean) feature.getAttribute("mode_mit")) modes = modes | Modes.getMode("passenger").id;
				modes = (modes&Modes.customAllowedAt)!=0 ? modes | Modes.getMode("custom").id : modes;
				if(modes==0 && ((modes&uModes)==0)) {
					continue;
				}
			    
			    Geometry g = (Geometry) feature.getAttribute("the_geom");
			    Geometry geom = JTS.transform(g, transform);
				if(geom.getNumGeometries()!=1) {
					System.err.println("Edge '" + (String) feature.getAttribute("oid") + "' has a multi geometries...");
				}
				LineString geom2 = (LineString) geom.getGeometryN(0);
				Coordinate[] cs = geom2.getCoordinates();
				DBNode fromNode = net.getNode((Integer) feature.getAttribute("nodefrom"), cs[0]);
				DBNode toNode = net.getNode((Integer) feature.getAttribute("nodeto"), cs[cs.length - 1]);
				ok &= net.addEdge(net.getNextID(), (String) feature.getAttribute("oid"), fromNode, toNode, modes,
						(Double) feature.getAttribute("vmax") / 3.6, geom2, (Double) feature.getAttribute("length"));
			
			}
			return net;
		} catch (FactoryException | MismatchedDimensionException | TransformException e) {
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
	private static DBNet loadNetFromSUMOFile(IDGiver idGiver, String fileName, long uModes) throws IOException {
		try {
			NetLoader nl = new NetLoader();
			DBNet net = new DBNet(idGiver);
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
		case FORMAT_WKT:
		case FORMAT_SHAPEFILE:
		case FORMAT_SUMO:
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

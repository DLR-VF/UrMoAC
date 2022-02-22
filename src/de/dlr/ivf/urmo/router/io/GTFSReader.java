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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.postgresql.PGConnection;

import de.dks.utils.options.OptionsCont;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.NearestEdgeFinder;
import de.dlr.ivf.urmo.router.gtfs.GTFSData;
import de.dlr.ivf.urmo.router.gtfs.GTFSRoute;
import de.dlr.ivf.urmo.router.gtfs.GTFSStop;
import de.dlr.ivf.urmo.router.gtfs.GTFSStopTime;
import de.dlr.ivf.urmo.router.gtfs.GTFSTrip;
import de.dlr.ivf.urmo.router.modes.EntrainmentMap;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.DBNode;
import de.dlr.ivf.urmo.router.shapes.GeomHelper;

/**
 * @class GTFSReader
 * @brief Reads a GTFS plan from a DB or a file
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class GTFSReader {
	/// @brief A list of week day names
	public static String[] weekdays = { "", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" };
	
	
	/** @brief Loads GTFS data from a database or a file
	 * @param options The options to read the input definition from
	 * @param bounds A bounding box for prunning read information
	 * @param net The used network
	 * @param entrainmentMap The used entrainment map
	 * @param epsg The used projection
	 * @param verbose Whether additional information shall be printed
	 * @return The loaded GTFS data
	 * @throws IOException When something fails
	 */
	public static GTFSData load(OptionsCont options, Geometry bounds, DBNet net, EntrainmentMap entrainmentMap, int epsg, boolean verbose) throws IOException {
		if(!options.isSet("date")) {
			throw new IOException("A date must be given when using GTFS.");
		}
		String def = options.getString("pt");
		Utils.Format format = Utils.getFormat(def);
		String[] inputParts = Utils.getParts(format, def, "pt");
		Vector<Integer> allowedCarrier = options.isSet("pt-restriction") ? parseCarrierDef(options.getString("pt-restriction")) : new Vector<>();
		switch(format) {
		case FORMAT_POSTGRES:
		case FORMAT_SQLITE:
			return loadGTFSFromDB(format, inputParts, allowedCarrier, options.getString("date"), bounds, net, entrainmentMap, epsg, verbose);
		case FORMAT_CSV:
			return loadGTFSFromFile(inputParts[0], allowedCarrier, options.getString("date"), bounds, net, entrainmentMap, epsg, verbose);
		case FORMAT_SHAPEFILE:
		case FORMAT_SUMO:
		case FORMAT_GEOPACKAGE:
			throw new IOException("Reading GTFS from " + Utils.getFormatMMLName(format) + " is not supported.");
		default:
			throw new IOException("Could not recognize the format used for GTFS.");
		}
	}
	
	
	/** @brief Reads GTFS from the database
	 * @param format The source format
	 * @param inputParts The source definition
	 * @param allowedCarrier The list of modes to load
	 * @param date The date to use
	 * @param bounds A geometrical bounding box
	 * @param net The used road network
	 * @param entrainmentMap The entrainment map
	 * @param epsg The projection
	 * @param verbose Whether it shall run in verbose mode
	 * @return The loaded GTFS net
	 * @throws IOException When something fails
	 */
	private static GTFSData loadGTFSFromDB(Utils.Format format, String[] inputParts, Vector<Integer> allowedCarrier, String date, 
			Geometry bounds, DBNet net, EntrainmentMap entrainmentMap, int epsg, boolean verbose) throws IOException {
		try {
			GeometryFactory gf = new GeometryFactory(new PrecisionModel());
			Connection connection = Utils.getConnection(format, inputParts, "pt");
				connection.setAutoCommit(true);
			connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);

			// read the boundary
			String boundsFilter = "";
			if(bounds!=null) {
				boundsFilter = " WHERE ST_Within(ST_TRANSFORM(pos, " + epsg + "), ST_GeomFromText('" + bounds.toText() + "', " + epsg + "))";
			}
			
			String tablePrefix = Utils.getTableName(format, inputParts, "pt");
			// read stops, extend network accordingly
			if(verbose) System.out.println(" ... reading stops ...");
			String query = "SELECT stop_id,ST_AsBinary(ST_TRANSFORM(pos," + epsg + ")) FROM " + tablePrefix + "_stops" + boundsFilter + ";";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
			WKBReader wkbRead = new WKBReader();
			HashMap<Long, GTFSStop> stops = new HashMap<>();
			HashMap<String, GTFSStop> id2stop = new HashMap<>();
			Vector<EdgeMappable> stopsV = new Vector<>();
			while (rs.next()) {
				ResultSetMetaData rsmd = rs.getMetaData();
				Geometry geom = wkbRead.read(rs.getBytes(rsmd.getColumnCount()));
				Coordinate[] cs = geom.getCoordinates();
				GTFSStop stop = new GTFSStop(net.getNextID(), rs.getString("stop_id"), cs[0], gf.createPoint(cs[0])); // !!! new id - the nodes should have a new id as well
				if(id2stop.containsKey(stop.mid)) {
					System.out.println("Warning: stop " + stop.mid + " already exists; skipping.");
					continue;
				}
				net.addNode(stop);
				stops.put(stop.id, stop);
				id2stop.put(stop.mid, stop);
				stopsV.add(stop);
			}
			rs.close();
			s.close();
			
			// map stops to edges
			long accessModes = Modes.getMode("foot").id|Modes.getMode("bicycle").id;
			NearestEdgeFinder nef = new NearestEdgeFinder(stopsV, net, accessModes);
			HashMap<DBEdge, Vector<MapResult>> edge2stops = nef.getNearestEdges(false);
			int failed = 0;
			// connect stops to network
			if(verbose) System.out.println(" ... connecting stops ...");
			for (DBEdge e : edge2stops.keySet()) {
				Vector<MapResult> edgeStops = edge2stops.get(e);
				if (e == null) {
					failed += edgeStops.size();
					continue;
				}
				// sort stops along edge
				Collections.sort(edgeStops, new Comparator<MapResult>() {
			        @Override
			        public int compare(MapResult o1, MapResult o2) {
			        	if(o1.pos==o2.pos) {
			        		return 0;
			        	}
		                return o1.pos < o2.pos ? -1 : 1;
			        }           
			    });
				// join stops with similar position
				Vector<Vector<MapResult>> stopClusters = new Vector<>();
				for(MapResult mr : edgeStops) {
					if (mr.edge == null) {
						++failed;
						continue;
					}
					if(stopClusters.size()==0 || stopClusters.lastElement().lastElement().pos-mr.pos>1.) {
						stopClusters.add(new Vector<MapResult>());
					}
					stopClusters.lastElement().add(mr);
				}
				// go through the list, allocate stops
				LineString lastGeom = e.getGeometry();
				DBEdge opp = e.opposite;
				double seen = 0;
				LineString lastOppGeom = e.opposite==null ? null : e.opposite.getGeometry();
				for(Vector<MapResult> mrv : stopClusters) {
					LineString geom;
					double stopEdgePos = mrv.lastElement().pos;
					double stopDist = mrv.lastElement().dist;
					Coordinate pos = GeomHelper.getPointAtDistance(e.getGeometry(), stopEdgePos-seen);
					String stopID = "stop@" + stopEdgePos;
					DBNode intermediateNode = net.getNode(net.getNextID(), pos);
					// build this side access
					geom = GeomHelper.getGeomUntilDistance(lastGeom, stopEdgePos-seen);
					if(!net.addEdge(net.getNextID(), e.id+"-stop@"+stopEdgePos, e.from, intermediateNode, e.modes, e.vmax, geom, geom.getLength())) {
						throw new ParseException("Could not allocate edge '" + e.id+"-"+stopID+ "'");
					}
					lastGeom = GeomHelper.getGeomBehindDistance(lastGeom, stopEdgePos-seen);
					String nextEdgeName = stopID+"-"+e.id;
					if(!net.addEdge(net.getNextID(), nextEdgeName, intermediateNode, e.to, e.modes, e.vmax, lastGeom, lastGeom.getLength())) {
						throw new ParseException("Could not allocate edge '" +stopID+ "-"+e.id + "'");
					}
					// build (optional) opposite side access
					String nextOppEdgeName = "";
					if(opp!=null) {
						lastOppGeom = GeomHelper.getGeomUntilDistance(opp.getGeometry(), opp.length-seen-stopEdgePos);
						nextOppEdgeName = opp.id+"-"+stopID;
						if(!net.addEdge(net.getNextID(), nextOppEdgeName, opp.from, intermediateNode, opp.modes, opp.vmax, lastOppGeom, lastOppGeom.getLength())) {
							throw new ParseException("Could not allocate edge '" + opp.id+"-"+stopID + "'");
						}
						geom = GeomHelper.getGeomBehindDistance(opp.getGeometry(), opp.length-seen-stopEdgePos);
						if(!net.addEdge(net.getNextID(), stopID+"-"+opp.id, intermediateNode, opp.to, opp.modes, opp.vmax, geom, geom.getLength())) {
							throw new ParseException("Could not allocate edge '" + stopID+"-"+opp.id + "'");
						}
					}
					seen += stopDist;
					// build access from / to the network
					for(MapResult mr : mrv) {
						GTFSStop stop = (GTFSStop) mr.em;
						Coordinate[] edgeCoords = new Coordinate[2];
						edgeCoords[0] = new Coordinate(intermediateNode.pos);
						edgeCoords[1] = new Coordinate(stop.pos);
						geom = new LineString(edgeCoords, e.geom.getPrecisionModel(), e.geom.getSRID());
						if(!net.addEdge(net.getNextID(), "on-"+stop.mid, intermediateNode, stop, accessModes, 50, geom, stopDist)) {
							throw new ParseException("Could not allocate edge '" + "on-"+stop.mid + "'");
						}
						edgeCoords[0] = new Coordinate(stop.pos);
						edgeCoords[1] = new Coordinate(intermediateNode.pos);
						geom = new LineString(edgeCoords, e.geom.getPrecisionModel(), e.geom.getSRID());
						if(!net.addEdge(net.getNextID(), "off-"+stop.mid, stop, intermediateNode, accessModes, 50, geom, stopDist)) {
							throw new ParseException("Could not allocate edge '" + "off-"+stop.mid + "'");
						}
					}
					// remove initial edges
					net.removeEdge(e);
					e = net.getEdgeByName(nextEdgeName);
					if(opp!=null) {
						net.removeEdge(opp);
						opp = net.getEdgeByName(nextOppEdgeName);
					}
				}
			}
			
			if(verbose) System.out.println(" " + failed + " stations could not be allocated");

			// read routes
			if(verbose) System.out.println(" ... reading routes ...");
			query = "SELECT route_id,route_short_name,route_type FROM " + tablePrefix + "_routes;";
			s = connection.createStatement();
			rs = s.executeQuery(query);
			HashMap<String, GTFSRoute> routes = new HashMap<>();
			while (rs.next()) {
				GTFSRoute route = new GTFSRoute(rs.getString("route_id"), rs.getString("route_short_name"), rs.getInt("route_type"));
				if(allowedCarrier.size()==0 || allowedCarrier.contains(route.type)) {
					routes.put(rs.getString("route_id"), route);
				}
			}
			rs.close();
			s.close();

			// read services
			if(verbose) System.out.println(" ... reading services ...");
			int dateI = 0;
			Date dateD = null;
			int dayOfWeek = 0;
			if(!"".equals(date)) {
				dateI = parseDate(date);
				SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd");
		        try {
					dateD = parser.parse(date);
					Calendar c = Calendar.getInstance();
					c.setTime(dateD);
					dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
				} catch (java.text.ParseException e1) {
					// has been checked before
				}		
			} else {
				System.err.println("No date information was supported; all schedules will be read from GTFS.");
			}
			Set<String> services = new HashSet<String>();
			if(dateI!=0) {
				query = "SELECT service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date FROM " + tablePrefix + "_calendar;";
				s = connection.createStatement();
				rs = s.executeQuery(query);
				while (rs.next()) {
					int dateBI = parseDate(rs.getString("start_date"));
					int dateEI = parseDate(rs.getString("end_date"));
					if(dateBI>dateI||dateEI<dateI) {
						continue;
					}
					// 
					if(rs.getInt(weekdays[dayOfWeek])!=0) {
						services.add(rs.getString("service_id"));
					}
				}
				rs.close();
				s.close();
				query = "SELECT service_id,date,exception_type FROM " + tablePrefix + "_calendar_dates;";
				s = connection.createStatement();
				rs = s.executeQuery(query);
				while (rs.next()) {
					int dateCI = parseDate(rs.getString("date"));
					if(dateCI!=dateI) {
						continue;
					}
					int et = rs.getInt("exception_type"); 
					String service_id = rs.getString("service_id"); 
					if(et==1) {
						services.add(service_id);
					} else if(et==2) {
						services.remove(service_id);
					} else {
						throw new ParseException("Unkonwn exception type in " + tablePrefix + "_calendar_dates.");
					}
				}
				rs.close();
				s.close();
			}
			
			// read trips and stop times
			if(verbose) System.out.println(" ... reading trips ...");
			query = "SELECT service_id,route_id,trip_id FROM " + tablePrefix + "_trips;";
			s = connection.createStatement();
			rs = s.executeQuery(query);
			HashMap<String, GTFSTrip> trips = new HashMap<>();
			while (rs.next()) {
				String service_id = rs.getString("service_id");
				if(dateI!=0&&!services.contains(service_id)) {
					continue;
				}
				String route_id = rs.getString("route_id");
				if(!routes.containsKey(route_id)) {
					continue;
				}
				GTFSTrip trip = new GTFSTrip(rs.getString("trip_id"), routes.get(route_id));
				trips.put(rs.getString("trip_id"), trip);
			}
			rs.close();
			s.close();
			
			// build intermediate container 
			GTFSData ret = new GTFSData(net, entrainmentMap, stops, routes, trips);
			
			// read stop times, add to the read GTFS data
			if(verbose) System.out.println(" ... reading stop times ...");
			query = "SELECT trip_id,arrival_time,departure_time,stop_id FROM " + tablePrefix + "_stop_times ORDER BY trip_id,stop_sequence;";
			s = connection.createStatement();
			rs = s.executeQuery(query);
			String lastTripID = null;
			Vector<GTFSStopTime> stopTimes = new Vector<>();
			int abs = 0;
			int err = 0;
			while (rs.next()) {
				String tripID = rs.getString("trip_id");
				if(!trips.containsKey(tripID)) {
					continue;
				}
				String stop_id = rs.getString("stop_id");
				if(lastTripID!=null&&!tripID.equals(lastTripID)) {
					err += ret.recheckTimesAndInsert(lastTripID, stopTimes, id2stop);
					abs += stopTimes.size() - 1;
					stopTimes.clear();
				}
				lastTripID = tripID;
				String arrivalTimeS = rs.getString("arrival_time");
				String departureTimeS = rs.getString("departure_time");
				int arrivalTime, departureTime;
				if(arrivalTimeS.indexOf(':')>=0) {
					arrivalTime = parseTime(arrivalTimeS);
					departureTime = parseTime(departureTimeS);
				} else {
					arrivalTime = Integer.parseInt(arrivalTimeS);
					departureTime = Integer.parseInt(departureTimeS);
				}
				GTFSStopTime stopTime = new GTFSStopTime(tripID, arrivalTime, departureTime, stop_id);
				stopTimes.add(stopTime);
			}
			rs.close();
			s.close();
			err += ret.recheckTimesAndInsert(lastTripID, stopTimes, id2stop);
			abs += stopTimes.size() - 1;
			stopTimes.clear();
			ret.sortConnections();
			if(verbose) System.out.println("  " + abs + " connections found of which " + err + " were erroneous");

			// read transfers times (optionally)
			int idx = tablePrefix.indexOf('.');
			String table = tablePrefix + "_transfers";
			String schema = "";
			if(idx>0) {
				String[] defs = tablePrefix.split("\\.");
				table = defs[0] + "_transfers";
				schema = defs[1];
			}
			DatabaseMetaData dbm = connection.getMetaData();
			// check if "employee" table is there
			ResultSet tables = dbm.getTables(null, schema, table, null);
			if (tables.next()) {
				if(verbose) System.out.println(" ... reading transfer times ...");
				query = "SELECT from_stop_id,to_stop_id,transfer_type,from_trip_id,to_trip_id,min_transfer_time FROM " + tablePrefix + "_transfers;";
				s = connection.createStatement();
				rs = s.executeQuery(query);
				while (rs.next()) {
					String fromStop = rs.getString("from_stop_id");
					GTFSStop stop = id2stop.get(fromStop);
					if(stop==null) {
						// may be out of the pt-boundary
						continue;
					}
					String toStop = rs.getString("to_stop_id");
					if(!fromStop.equals(toStop)) {
						continue;
					}
					if(rs.getInt("transfer_type")!=2) {
						continue;
					}
					try {
						String s1 = rs.getString("from_trip_id");
						String s2 = rs.getString("to_trip_id");
						GTFSTrip t1 = trips.get(Integer.parseInt(s1));
						GTFSTrip t2 = trips.get(Integer.parseInt(s2)); // !!! todo: times are given on per-trip, not per-route base
						if(t1!=null&&t2!=null) {
							stop.setInterchangeTime(t1, t2, (double) rs.getInt("min_transfer_time"));
						}
					} catch(NumberFormatException e) {
					}
				}
				rs.close();
				s.close();
			}
			connection.close();
			return ret;
			// !!! dismiss stops which do not have a route assigned?
		} catch (SQLException | ParseException e2) {
			throw new IOException(e2);
		}
	}


	/** @brief Loads GTFS from files
	 * @param fileNamePrefix The path to the files
	 * @param allowedCarrier The list of modes to load
	 * @param date The date to use
	 * @param bounds A geometrical bounding box
	 * @param net The used road network
	 * @param entrainmentMap The entrainment map
	 * @param epsg The projection
	 * @param verbose Whether it shall run in verbose mode
	 * @return The loaded GTFS net
	 * @throws IOException When something fails
	 */
	private static GTFSData loadGTFSFromFile(String fileNamePrefix, Vector<Integer> allowedCarrier, String date, 
			Geometry bounds, DBNet net, EntrainmentMap entrainmentMap, int epsg, boolean verbose) throws IOException {
		throw new IOException("GTFS loading from files is not yet supported!");
	}	

	
	/** @brief Parses the definition of pt carriers to load
	 * @param carrierDef The definition to parse
	 * @return The list of carriers to load
	 */
	private static Vector<Integer> parseCarrierDef(String carrierDef) {
		Vector<Integer> allowedCarrier = null;
		if(!"".equals(carrierDef)) {
			String[] r = carrierDef.split(";");
			allowedCarrier = new Vector<>();
			for(String r1 : r) {
				allowedCarrier.add(Integer.parseInt(r1));
			}
			if(allowedCarrier.size()==0) {
				allowedCarrier = null;
			}
		}
		return allowedCarrier;
	}
	
	
	/** 
	 * @brief Parses the time string to seconds
	 * @param timeS The time string
	 * @return The time in seconds
	 */
	private static int parseTime(String timeS) {
		String[] r = timeS.split(":");
		return Integer.parseInt(r[0])*3600 + Integer.parseInt(r[1])*60 + Integer.parseInt(r[2]);
	}


	/** 
	 * @brief Parses the date string to an int
	 * 
	 * Replaces '-' if given in string (often found albeit not being the standard)
	 * @param dateS The date string
	 * @return The date as an integer
	 */
	private static int parseDate(String dateS) {
		if(dateS.indexOf('-')>=0) {
			dateS = dateS.replace("-", "");
		}
		return Integer.parseInt(dateS);
	}

}

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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.postgresql.PGConnection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

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
 * @class GTFSDBReader
 * @brief Reads a GTFS plan from the db
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class GTFSDBReader {
	/// @brief A list of week day names
	public static String[] weekdays = { "", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" };
	
	
	/**
	 * @brief Loads GTFS data from a db
	 * @param url The url of the database
	 * @param tablePrefix The prefix of the tables to read from
	 * @param user The user name for connecting to the database
	 * @param pw The user's password
	 * @param net The network, used for mapping stations onto it
	 * @return The loaded GTFS data
	 * @throws SQLException
	 * @throws ParseException
	 * @todo which modes to use to access the road network
	 * @todo which modes to use to access the stations
	 */
	public static GTFSData load(String url, String tablePrefix, String user, String pw, String carrierDef, String date, 
			Geometry bounds, DBNet net, EntrainmentMap entrainmentMap, int epsg)
			throws SQLException, ParseException {
		// parse modes vector
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
		//
		GeometryFactory gf = new GeometryFactory(new PrecisionModel());
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);

		// read the boundary
		String boundsFilter = "";
		if(bounds!=null) {
			boundsFilter = " WHERE ST_Within(ST_TRANSFORM(pos, " + epsg + "), ST_GeomFromText('" + bounds.toText() + "', " + epsg + "))";
		}
		
		// read stops, extend network accordingly
		System.out.println(" ... reading stops ...");
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
		System.out.println(" ... connecting stops ...");
		HashMap<EdgeMappable, MapResult> stop2edge = NearestEdgeFinder.results2edgeSet(edge2stops);
		for (EdgeMappable stopM : stop2edge.keySet()) {
			GTFSStop stop = (GTFSStop) stopM;
			MapResult mr = stop2edge.get(stop);
			if (mr==null || mr.edge == null) {
				++failed;
			} else {
				LineString geom;
				Coordinate pos = GeomHelper.getPointAtDistance((LineString) mr.edge.getGeometry(), mr.pos);
				DBNode intermediateNode = net.getNode(net.getNextID(), pos);
				
				net.removeEdge(mr.edge);
				geom = GeomHelper.getGeomUntilDistance((LineString) mr.edge.getGeometry(), mr.pos);
				DBEdge e11 = new DBEdge(net.getNextID(), mr.edge.id+"-"+stop.mid, mr.edge.from, intermediateNode, mr.edge.modes, mr.edge.vmax, geom, geom.getLength()/*mr.pos*/);
				net.addEdge(e11);
				geom = GeomHelper.getGeomBehindDistance((LineString) mr.edge.getGeometry(), mr.pos);
				DBEdge e12 = new DBEdge(net.getNextID(), stop.mid+"-"+mr.edge.id, intermediateNode, mr.edge.to, mr.edge.modes, mr.edge.vmax, geom, geom.getLength()/*mr.edge.length-mr.pos*/);
				net.addEdge(e12);
				
				DBEdge opp = mr.edge.opposite;
				if(opp!=null) {
					net.removeEdge(opp);
					geom = GeomHelper.getGeomUntilDistance((LineString) opp.getGeometry(), opp.length-mr.pos);
					DBEdge e21 = new DBEdge(net.getNextID(), opp.id+"-"+stop.mid, opp.from, intermediateNode, opp.modes, opp.vmax, geom, geom.getLength()/*opp.length-mr.pos*/);
					net.addEdge(e21);
					geom = GeomHelper.getGeomBehindDistance((LineString) opp.getGeometry(), opp.length-mr.pos);
					DBEdge e22 = new DBEdge(net.getNextID(), stop.mid+"-"+opp.id, intermediateNode, opp.to, opp.modes, opp.vmax, geom, geom.getLength()/*mr.pos*/);
					net.addEdge(e22);
				}
				
				Coordinate[] edgeCoords = new Coordinate[2];
				edgeCoords[0] = new Coordinate(intermediateNode.pos);
				edgeCoords[1] = new Coordinate(stop.pos);
				geom = new LineString(edgeCoords, mr.edge.geom.getPrecisionModel(), mr.edge.geom.getSRID());
				new DBEdge(net.getNextID(), "on-"+stop.mid, intermediateNode, stop, accessModes, 50, geom, mr.dist);
				edgeCoords[0] = new Coordinate(stop.pos);
				edgeCoords[1] = new Coordinate(intermediateNode.pos);
				geom = new LineString(edgeCoords, mr.edge.geom.getPrecisionModel(), mr.edge.geom.getSRID());
				new DBEdge(net.getNextID(), "off-"+stop.mid, stop, intermediateNode, accessModes, 50, geom, mr.dist);
			}
		}
		System.out.println(" " + failed + " stations could not be allocated");

		// read routes
		System.out.println(" ... reading routes ...");
		query = "SELECT route_id,route_short_name,route_type FROM " + tablePrefix + "_routes;";
		s = connection.createStatement();
		rs = s.executeQuery(query);
		HashMap<String, GTFSRoute> routes = new HashMap<>();
		while (rs.next()) {
			GTFSRoute route = new GTFSRoute(rs.getString("route_id"), rs.getString("route_short_name"), rs.getInt("route_type"));
			if(allowedCarrier==null || allowedCarrier.contains(route.type)) {
				routes.put(rs.getString("route_id"), route);
			}
		}
		rs.close();
		s.close();

		// read services
		System.out.println(" ... reading services ...");
		int dateI = 0;
		Date dateD = null;
		int dayOfWeek = 0;
		if(!"".equals(date)) {
			dateI = Integer.parseInt(date);
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
				int dateBI = rs.getInt("start_date");
				int dateEI = rs.getInt("end_date");
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
				int dateCI = rs.getInt("date");
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
		System.out.println(" ... reading trips ...");
		query = "SELECT service_id,route_id,trip_id FROM " + tablePrefix + "_trips;";
		s = connection.createStatement();
		rs = s.executeQuery(query);
		HashMap<Integer, GTFSTrip> trips = new HashMap<>();
		while (rs.next()) {
			String service_id = rs.getString("service_id");
			if(dateI!=0&&!services.contains(service_id)) {
				continue;
			}
			if(!routes.containsKey(rs.getString("route_id"))) {
				continue;
			}
			GTFSTrip trip = new GTFSTrip(rs.getString("route_id"), service_id, rs.getInt("trip_id"));
			trips.put(rs.getInt("trip_id"), trip);
		}
		rs.close();
		s.close();
		
		// build intermediate container 
		GTFSData ret = new GTFSData(net, entrainmentMap, stops, routes, trips);
		
		// read stop times, add to the read GTFS data
		System.out.println(" ... reading stop times ...");
		query = "SELECT trip_id,arrival_time,departure_time,stop_id FROM " + tablePrefix + "_stop_times ORDER BY trip_id,stop_sequence;";
		s = connection.createStatement();
		rs = s.executeQuery(query);
		int lastTripID = -1;
		Vector<GTFSStopTime> stopTimes = new Vector<>();
		int abs = 0;
		int err = 0;
		while (rs.next()) {
			int tripID = rs.getInt("trip_id");
			if(!trips.containsKey(tripID)) {
				continue;
			}
			if(tripID!=lastTripID&&lastTripID!=-1) {
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
			GTFSStopTime stopTime = new GTFSStopTime(tripID, arrivalTime, departureTime, rs.getString("stop_id"));
			stopTimes.add(stopTime);
		}
		rs.close();
		s.close();
		err += ret.recheckTimesAndInsert(lastTripID, stopTimes, id2stop);
		abs += stopTimes.size() - 1;
		stopTimes.clear();
		ret.sortConnections();
		System.out.println("  " + abs + " connections found of which " + err + " were erroneous");

		// read transfers times
		System.out.println(" ... reading transfer times ...");
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
					stop.setInterchangeTime(t1.routeID, t2.routeID, (double) rs.getInt("min_transfer_time"));
				}
			} catch(NumberFormatException e) {
			}
		}
		rs.close();
		s.close();
		
		return ret;
		// !!! dismiss stops which do not have a route assigned?
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

}

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

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.postgresql.PGConnection;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.gtfs.GTFSData;
import de.dlr.ivf.urmo.router.gtfs.GTFSRoute;
import de.dlr.ivf.urmo.router.gtfs.GTFSStop;
import de.dlr.ivf.urmo.router.gtfs.GTFSStopTime;
import de.dlr.ivf.urmo.router.gtfs.GTFSTrip;
import de.dlr.ivf.urmo.router.shapes.DBNet;

/**
 * @class GTFSReader_File
 * @brief Reads a GTFS plan from the DB
 * @author Daniel Krajzewicz
 */
public class GTFSReader_DB extends AbstractGTFSReader {
	/// @brief The SQL WHERE-clause that applies a bounding box
	private String boundsFilter = "";
	/// @brief The prefix of the tables
	private String tablePrefix;
	/// @brief The connection to the database
	private Connection connection;
	
	
	/** @brief Constructor
	 * @param net The network to use
	 * @param epsg The used projection
	 * @param date The date to read the GTFS information for
     * @param allowedCarrier List of allowed carriers (todo: recheck)
     */
	public GTFSReader_DB(DBNet net, int epsg, String date, Vector<Integer> allowedCarrier) {
		super(net, epsg, date, allowedCarrier);
	}

	
	
	/// @brief Implemented abstract methods
	/// @{
	
	/** @brief Initialises the reader
	 * @param format The format the reader uses
	 * @param inputParts The input access definition
	 * @param bounds The bounding box to use
	 * @throws IOException If something fails
	 */
	protected void init(Utils.Format format, String[] inputParts, Geometry bounds) throws IOException {
		// db jars issue, see https://stackoverflow.com/questions/999489/invalid-signature-file-when-attempting-to-run-a-jar
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			connection = Utils.getConnection(format, inputParts, "pt");
			connection.setAutoCommit(true);
			connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);

			// read the boundary
			if(bounds!=null) {
				boundsFilter = " WHERE ST_Within(ST_TRANSFORM(pos, " + epsg + "), ST_GeomFromText('" + bounds.toText() + "', " + epsg + "))";
			}
			tablePrefix = Utils.getTableName(format, inputParts, "pt");
		} catch (SQLException e2) {
			throw new IOException(e2);
		}
	}
	
	
	/** @brief Reads stops
	 * @param stops The mapping from an internal ID to the stop to fill
	 * @param id2stop The mapping from the GTFS stop ID to the stop to fill
	 * @param stopsV The list of stops to fill
	 * @throws IOException If something fails
	 */
	protected void readStops(HashMap<Long, GTFSStop> stops, HashMap<String, GTFSStop> id2stop, Vector<EdgeMappable> stopsV) throws IOException {
		try {
			String query = "SELECT stop_id,ST_AsBinary(ST_TRANSFORM(pos," + epsg + ")) FROM " + tablePrefix + "_stops" + boundsFilter + ";";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
			WKBReader wkbRead = new WKBReader();
			while (rs.next()) {
				ResultSetMetaData rsmd = rs.getMetaData();
				Geometry geom = wkbRead.read(rs.getBytes(rsmd.getColumnCount()));
				Coordinate[] cs = geom.getCoordinates();
				GTFSStop stop = new GTFSStop(net.getNextID(), rs.getString("stop_id"), cs[0], net.getGeometryFactory().createPoint(cs[0])); // !!! new id - the nodes should have a new id as well
				if(id2stop.containsKey(stop.mid)) {
					System.out.println("Warning: stop " + stop.mid + " already exists; skipping.");
					continue;
				}
				if(!net.addNode(stop, stop.mid)) {
					throw new IOException("A node with id '" + stop.getID() + "' already exists.");
				}
				stops.put(stop.getID(), stop);
				id2stop.put(stop.mid, stop);
				stopsV.add(stop);
			}
			rs.close();
			s.close();
		} catch (SQLException | ParseException e2) {
			throw new IOException(e2);
		}
	}
	

	/** @brief Reads routes
	 * @param routes The mapping from ID to route to fill
	 * @throws IOException If something fails
	 */
	protected void readRoutes(HashMap<String, GTFSRoute> routes) throws IOException {
		try {
			String query = "SELECT route_id,route_short_name,route_type FROM " + tablePrefix + "_routes;";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
			while (rs.next()) {
				GTFSRoute route = new GTFSRoute(rs.getString("route_id"), rs.getString("route_short_name"), rs.getInt("route_type"));
				if(allowedCarrier.size()==0 || allowedCarrier.contains(route.type)) {
					routes.put(rs.getString("route_id"), route);
				}
			}
			rs.close();
			s.close();
		} catch (SQLException e2) {
			throw new IOException(e2);
		}
	}
	
	
	/** @brief Reads services
	 * @param dateI The integer representation of the date (todo: recheck)
	 * @param dayOfWeek The day of the week
	 * @param services The set of services to fill
	 * @throws IOException If something fails
	 */
	protected void readServices(int dateI, int dayOfWeek, Set<String> services) throws IOException {
		if(dateI==0) {
			return;
		}
		try {
			String query = "SELECT service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date FROM " + tablePrefix + "_calendar;";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
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
			if(Utils.tableExists(connection, tablePrefix + "_calendar_dates")) {
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
						throw new IOException("Unkonwn exception type in " + tablePrefix + "_calendar_dates.");
					}
				}
				rs.close();
				s.close();
			}
		} catch (SQLException e2) {
			throw new IOException(e2);
		}
	}
	
	
	/** @brief Reads trips
	 * @param services The read services
	 * @param routes The read routes
	 * @param dateI The integer representation of the date (todo: recheck)
	 * @param trips The mapping of IDs to trips to fill
	 * @throws IOException If something fails
	 */
	protected void readTrips(Set<String> services, HashMap<String, GTFSRoute> routes, int dateI, HashMap<String, GTFSTrip> trips) throws IOException {
		try {
			String query = "SELECT service_id,route_id,trip_id FROM " + tablePrefix + "_trips;";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
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
		} catch (SQLException e2) {
			throw new IOException(e2);
		}
	}
	
	
	/** @brief Reads stop times
	 * @param ret The GTFS container to get additional information from
	 * @param trips The read trips
	 * @param id2stop The read stops
	 * @param verbose Whether additional information shall be printed
	 * @throws IOException If something fails
	 */
	protected void readStopTimes(GTFSData ret, HashMap<String, GTFSTrip> trips, HashMap<String, GTFSStop> id2stop, boolean verbose) throws IOException {
		try {
			String query = "SELECT trip_id,arrival_time,departure_time,stop_id FROM " + tablePrefix + "_stop_times ORDER BY trip_id,stop_sequence;";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
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
		} catch (SQLException e2) {
			throw new IOException(e2);
		}
	}
	

	/** @brief Reads transfer times
	 * @param trips The read trips
	 * @param id2stop The read stops
	 * @param verbose Whether additional information shall be printed
	 * @throws IOException If something fails
	 */
	protected void readTransfers(HashMap<String, GTFSTrip> trips, HashMap<String, GTFSStop> id2stop, boolean verbose) throws IOException {
		try {
			if(!Utils.tableExists(connection, tablePrefix + "_transfers")) {
				return;
			}
			if(verbose) System.out.println(" ... reading transfer times ...");
			String query = "SELECT from_stop_id,to_stop_id,transfer_type,from_trip_id,to_trip_id,min_transfer_time FROM " + tablePrefix + "_transfers;";
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
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
				GTFSTrip t1 = trips.get(rs.getString("from_trip_id"));
				GTFSTrip t2 = trips.get(rs.getString("to_trip_id"));
				if(t1!=null&&t2!=null) {
					stop.setInterchangeTime(t1, t2, (double) rs.getInt("min_transfer_time"));
				}
			}
			rs.close();
			s.close();
		} catch (SQLException e2) {
			throw new IOException(e2);
		}
	}
	
	
	/** @brief Closes the connection / file
	 * @throws IOException If something fails
	 */
	protected void close() throws IOException {
		try {
			connection.close();
		} catch (SQLException e2) {
			throw new IOException(e2);
		}
	}
	
	/// @}
	
}

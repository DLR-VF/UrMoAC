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
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.NearestEdgeFinder;
import de.dlr.ivf.urmo.router.gtfs.GTFSData;
import de.dlr.ivf.urmo.router.gtfs.GTFSRoute;
import de.dlr.ivf.urmo.router.gtfs.GTFSStop;
import de.dlr.ivf.urmo.router.gtfs.GTFSTrip;
import de.dlr.ivf.urmo.router.modes.EntrainmentMap;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.DBNode;
import de.dlr.ivf.urmo.router.shapes.GeomHelper;

/**
 * @class GTFSReader_File
 * @brief Abstract class for reading GTFS data
 * @author Daniel Krajzewicz
 */
public abstract class AbstractGTFSReader {
	/// @brief A list of week day names
	public static String[] weekdays = { "", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" };
	/// @brief The network to use
	protected DBNet _net;
	/// @brief The used projection
	protected int _epsg;
	/// @brief List of allowed carriers (todo: recheck)
	protected Vector<Integer> _allowedCarrier;
	/// @brief The date to read the GTFS information for
	protected String _date;

	
	/** @brief Constructor
	 * @param net The network to use
	 * @param epsg The used projection
	 * @param date The date to read the GTFS information for
     * @param allowedCarrier List of allowed carriers (todo: recheck)
     */
	public AbstractGTFSReader(DBNet net, int epsg, String date, Vector<Integer> allowedCarrier) {
		_net = net;
		_epsg = epsg;
		_allowedCarrier = allowedCarrier;
		_date = date;
	}
	
	
	/// @brief Abstract methods
	/// @{
	
	/** @brief Initialises the reader
	 * @param format The format the reader uses
	 * @param inputParts The input access definition
	 * @param bounds The bounding box to use
	 * @throws IOException If something fails
	 */
	protected abstract void init(Utils.Format format, String[] inputParts, Geometry bounds) throws IOException;
	
	
	/** @brief Reads stops
	 * @param stops The mapping from an internal ID to the stop to fill
	 * @param id2stop The mapping from the GTFS stop ID to the stop to fill
	 * @param stopsV The list of stops to fill
	 * @throws IOException If something fails
	 */
	protected abstract void readStops(HashMap<Long, GTFSStop> stops, HashMap<String, GTFSStop> id2stop, Vector<EdgeMappable> stopsV) throws IOException;
	
	
	/** @brief Reads routes
	 * @param routes The mapping from ID to route to fill
	 * @throws IOException If something fails
	 */
	protected abstract void readRoutes(HashMap<String, GTFSRoute> routes) throws IOException;
	
	
	/** @brief Reads services
	 * @param dateI The integer representation of the date (todo: recheck)
	 * @param dayOfWeek The day of the week
	 * @param services The set of services to fill
	 * @throws IOException If something fails
	 */
	protected abstract void readServices(int dateI, int dayOfWeek, Set<String> services) throws IOException;
	
	
	/** @brief Reads trips
	 * @param services The read services
	 * @param routes The read routes
	 * @param dateI The integer representation of the date (todo: recheck)
	 * @param trips The mapping of IDs to trips to fill
	 * @throws IOException If something fails
	 */
	protected abstract void readTrips(Set<String> services, HashMap<String, GTFSRoute> routes, int dateI, HashMap<String, GTFSTrip> trips) throws IOException;
	
	
	/** @brief Reads stop times
	 * @param ret The GTFS container to get additional information from
	 * @param trips The read trips
	 * @param id2stop The read stops
	 * @param verbose Whether additional information shall be printed
	 * @throws IOException If something fails
	 */
	protected abstract void readStopTimes(GTFSData ret, HashMap<String, GTFSTrip> trips, HashMap<String, GTFSStop> id2stop, boolean verbose) throws IOException;
	
	
	/** @brief Reads transfer times
	 * @param trips The read trips
	 * @param id2stop The read stops
	 * @param verbose Whether additional information shall be printed
	 * @throws IOException If something fails
	 */
	protected abstract void readTransfers(HashMap<String, GTFSTrip> trips, HashMap<String, GTFSStop> id2stop, boolean verbose) throws IOException;
	
	
	/** @brief Closes the connection / file
	 * @throws IOException If something fails
	 */
	protected abstract void close() throws IOException;
	
	/// @}

	

	/** @brief Reads GTFS from the database
	 * @param format The source format
	 * @param inputParts The source definition
	 * @param bounds A geometric bounding box
	 * @param entrainmentMap The entrainment map
	 * @param numThreads The number of threads to use for mapping halts to the road network
	 * @param verbose Whether it shall run in verbose mode
	 * @return The loaded GTFS net
	 * @throws IOException When something fails
	 */
	public GTFSData load(Utils.Format format, String[] inputParts, Geometry bounds,
			EntrainmentMap entrainmentMap, int numThreads, boolean verbose) throws IOException {
		try {
			init(format, inputParts, bounds);
			// read stops, extend network accordingly
			if(verbose) System.out.println(" ... reading stops ...");
			HashMap<Long, GTFSStop> stops = new HashMap<>();
			HashMap<String, GTFSStop> id2stop = new HashMap<>();
			Vector<EdgeMappable> stopsV = new Vector<>();
			readStops(stops, id2stop, stopsV);
			// map stops to edges
			long accessModes = Modes.getMode("foot").id|Modes.getMode("bike").id;
			NearestEdgeFinder nef = new NearestEdgeFinder(stopsV, _net, accessModes);
			HashMap<DBEdge, Vector<MapResult>> edge2stops = nef.getNearestEdges(false, false, numThreads);
			// connect stops to network
			if(verbose) System.out.println(" ... connecting stops ...");
			int failed = connectStops(edge2stops, accessModes);
			if(verbose) System.out.println(" " + failed + " stations could not be allocated");
			// read routes
			if(verbose) System.out.println(" ... reading routes ...");
			HashMap<String, GTFSRoute> routes = new HashMap<>();
			readRoutes(routes);
			// parse date
			int dateI = 0;
			Date dateD = null;
			int dayOfWeek = 0;
			if(!"".equals(_date)) {
				dateI = parseDate(_date);
				SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd");
		        try {
					dateD = parser.parse(_date);
					Calendar c = Calendar.getInstance();
					c.setTime(dateD);
					dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
				} catch (java.text.ParseException e1) {
					// has been checked before
				}		
			} else {
				System.err.println("No date information was supported; all schedules will be read from GTFS.");
			}
			// read services
			if(verbose) System.out.println(" ... reading services ...");
			Set<String> services = new HashSet<String>();
			readServices(dateI, dayOfWeek, services);
			// read trips and stop times
			if(verbose) System.out.println(" ... reading trips ...");
			HashMap<String, GTFSTrip> trips = new HashMap<>();
			readTrips(services, routes, dateI, trips);
			// build intermediate container 
			GTFSData ret = new GTFSData(_net, entrainmentMap, stops, routes, trips);
			
			// read stop times, add to the read GTFS data
			if(verbose) System.out.println(" ... reading stop times ...");
			readStopTimes(ret, trips, id2stop, verbose);
			// read transfers times (optionally)
			readTransfers(trips, id2stop, verbose);
			// close
			close();
			return ret;
			// !!! dismiss stops which do not have a route assigned?
		} catch (ParseException e2) {
			throw new IOException(e2);
		}
	}
	
	
	/** 
	 * @brief Parses the time string to seconds
	 * @param timeS The time string
	 * @return The time in seconds
	 */
	protected int parseTime(String timeS) {
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
	protected int parseDate(String dateS) {
		if(dateS.indexOf('-')>=0) {
			dateS = dateS.replace("-", "");
		}
		return Integer.parseInt(dateS);
	}
	
	
	/** @brief Connects stops with GTFSEdges
	 * @param edge2stops The computed mapping of stops to edges 
	 * @param accessModes The modes used to access the stops
	 * @return How many stops could not be connected
	 * @throws ParseException If a geometry could not been added
	 * @throws IOException 
	 */
	protected int connectStops(HashMap<DBEdge, Vector<MapResult>> edge2stops, long accessModes) throws ParseException, IOException {
		int failed = 0;
		HashMap<DBEdge, DBEdge> seenOpposite = new HashMap<DBEdge, DBEdge>();
		for (DBEdge e : edge2stops.keySet()) {
			if (e == null) {
				failed += edge2stops.get(e).size();
				continue;
			}
			// we have to add 
			if(seenOpposite.containsKey(e)) {
				continue;
			}
			Vector<MapResult> edgeStops = new Vector<MapResult>(edge2stops.get(e));
			if(e.getOppositeEdge()!=null&&edge2stops.containsKey(e.getOppositeEdge())) {
				for(MapResult stop : edge2stops.get(e.getOppositeEdge())) {
					MapResult mr = new MapResult(stop.em, stop.edge, stop.dist, e.getLength()-stop.pos, false);
					edgeStops.add(mr);
				}
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
			DBEdge opp = e.getOppositeEdge();
			double seen = 0;
			LineString lastOppGeom = null;
			if(e.getOppositeEdge()!=null) {
				lastOppGeom = e.getOppositeEdge().getGeometry();
				seenOpposite.put(e.getOppositeEdge(), e);
			}
			for(Vector<MapResult> mrv : stopClusters) {
				LineString geom;
				double stopEdgePos = mrv.lastElement().pos;
				double stopDist = mrv.lastElement().dist;
				Coordinate pos = GeomHelper.getPointAtDistance(e.getGeometry(), stopEdgePos-seen);
				String stopID = "stop@" + stopEdgePos;
				DBNode intermediateNode = _net.getNode(_net.getNextID(), pos);
				// build this side access
				geom = GeomHelper.getGeomUntilDistance(lastGeom, stopEdgePos-seen);
				if(!_net.addEdge(e.getID()+"-"+stopID, e.getFromNode(), intermediateNode, e.getModes(), e.getVMax(), geom, geom.getLength())) {
					throw new ParseException("Could not allocate edge '" + e.getID()+"-"+stopID+ "'");
				}
				lastGeom = GeomHelper.getGeomBehindDistance(lastGeom, stopEdgePos-seen);
				String nextEdgeName = stopID+"-"+e.getID();
				if(!_net.addEdge(nextEdgeName, intermediateNode, e.getToNode(), e.getModes(), e.getVMax(), lastGeom, lastGeom.getLength())) {
					throw new ParseException("Could not allocate edge '" +stopID+ "-"+e.getID() + "'");
				}
				// build (optional) opposite side access
				String nextOppEdgeName = "";
				if(opp!=null) {
					lastOppGeom = GeomHelper.getGeomUntilDistance(opp.getGeometry(), opp.getLength()-seen-stopEdgePos);
					nextOppEdgeName = opp.getID()+"-"+stopID;
					if(!_net.addEdge(nextOppEdgeName, opp.getFromNode(), intermediateNode, opp.getModes(), opp.getVMax(), lastOppGeom, lastOppGeom.getLength())) {
						throw new ParseException("Could not allocate edge '" + opp.getID()+"-"+stopID + "'");
					}
					geom = GeomHelper.getGeomBehindDistance(opp.getGeometry(), opp.getLength()-seen-stopEdgePos);
					if(!_net.addEdge(stopID+"-"+opp.getID(), intermediateNode, opp.getToNode(), opp.getModes(), opp.getVMax(), geom, geom.getLength())) {
						throw new ParseException("Could not allocate edge '" + stopID+"-"+opp.getID() + "'");
					}
				}
				seen += stopDist;
				// build access from / to the network
				for(MapResult mr : mrv) {
					GTFSStop stop = (GTFSStop) mr.em;
					Coordinate[] edgeCoords = new Coordinate[2];
					edgeCoords[0] = new Coordinate(intermediateNode.getCoordinate());
					edgeCoords[1] = new Coordinate(stop.getCoordinate());
					geom = e.getGeometry().getFactory().createLineString(edgeCoords);
					if(!_net.addEdge("on-"+stop.mid, intermediateNode, stop, accessModes, 50, geom, Math.max(stopDist, 0.1))) {
						throw new ParseException("Could not allocate edge '" + "on-"+stop.mid + "'");
					}
					edgeCoords[0] = new Coordinate(stop.getCoordinate());
					edgeCoords[1] = new Coordinate(intermediateNode.getCoordinate());
					geom = e.getGeometry().getFactory().createLineString(edgeCoords);
					if(!_net.addEdge("off-"+stop.mid, stop, intermediateNode, accessModes, 50, geom, Math.max(stopDist, 0.1))) {
						throw new ParseException("Could not allocate edge '" + "off-"+stop.mid + "'");
					}
				}
				// remove initial edges
				_net.removeEdge(e);
				e = _net.getEdgeByName(nextEdgeName);
				if(opp!=null) {
					_net.removeEdge(opp);
					opp = _net.getEdgeByName(nextOppEdgeName);
				}
			}
		}
		return failed;
	}
	
	
}

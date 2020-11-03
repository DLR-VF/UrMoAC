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
package de.dlr.ivf.urmo.router.gtfs;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import de.dlr.ivf.urmo.router.modes.EntrainmentMap;
import de.dlr.ivf.urmo.router.shapes.DBNet;

/**
 * @class GTFSData
 * @brief A container for read GTFS data
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class GTFSData {
	/// @brief A map of ids to the respective stop
	public HashMap<Long, GTFSStop> stops;
	/// @brief A map of ids to the respective route
	public HashMap<String, GTFSRoute> routes;
	/// @brief A map of ids to the respective trip
	public HashMap<String, GTFSTrip> trips;
	/// @brief A set of edges (!!! unused?)
	public Set<GTFSEdge> ptedges = new HashSet<>();
	/// @brief The network to refer to
	private DBNet net;
	/// @brief The entrainment map used
	private EntrainmentMap entrainmentMap;
	

	/// @brief TODO: some kind of an intermediate storage for dealing with a
	/// line names; to be replaced by something honest
	private HashMap<String, String> namemap = new HashMap<>();
	
	


	/**
	 * @brief Constructor
	 * @param _stops A map of ids to the respective stop
	 * @param _routes A map of ids to the respective route
	 * @param trips2 A list of trips
	 * @param _connections A set of edges (!!! unused?)
	 */
	public GTFSData(DBNet _net, EntrainmentMap _entrainmentMap, HashMap<Long, GTFSStop> _stops, HashMap<String, GTFSRoute> _routes, HashMap<String, GTFSTrip> _trips) {
		net = _net;
		entrainmentMap = _entrainmentMap;
		stops = _stops;
		routes = _routes;
		trips = _trips;

		namemap.put("100", "RE");
		namemap.put("109", "S-Bahn");
		namemap.put("400", "U-Bahn");
		namemap.put("700", "Bus");
		namemap.put("900", "Tram");
		namemap.put("1000", "Ferry");
	}


	/**
	 * @brief TODO: a hack for obtaining the used mode
	 * @param lines The used lines
	 * @return The used modes
	 */
	public String getModesString(Set<String> lines) {
		Set<String> modes = new HashSet<>();
		for (String line : lines) {
			if (line == null) {
				continue;
			}
			if ("foot".equals(line)) {
				modes.add("foot");
				continue;
			}
			if ("bicycle".equals(line)) {
				modes.add("bicycle");
				continue;
			}
			if ("passenger".equals(line)) {
				modes.add("passenger");
				continue;
			}
			/* !!!!
			String mode = line;
			GTFSRoute route = routes.get(Long.decode(line));
			if(route!=null) {
				String mode2 = namemap.get("" + routes.get(Long.decode(line)).type);
				if(mode2!=null) {
					mode = mode2;
				}
			}
			modes.add(mode);
			*/
		}
		return modes.toString();
	}



	/** @brief Revisits connections correcting the times and inserts them into respective edges
	 * 
	 * It may happen that a pt carrier departs a stop and enters the next at the same time. This is patched by adding / subtracting
	 * 15s.
	 * 
	 * After this is done, the connections are inserted into the respective edges.
	 * 
	 * @param lastConnections The connections to recheck
	 */
	public int recheckTimesAndInsert(String tripID, Vector<GTFSStopTime> stopTimes, HashMap<String, GTFSStop> id2stop) {
		Collections.sort(stopTimes, new Comparator<GTFSStopTime>() {
            public int compare(GTFSStopTime obj1, GTFSStopTime obj2) {
                return obj1.arrivalTime - obj2.arrivalTime;
            }
		});
		
		Vector<GTFSConnection> connections = new Vector<>();
		GTFSStopTime lastStopTime = null;
		for(Iterator<GTFSStopTime> i=stopTimes.iterator(); i.hasNext();) {
			GTFSStopTime stopTime = i.next();
			if (lastStopTime == null) {
				lastStopTime = stopTime;
				continue;
			}
			GTFSStop stop = id2stop.get(stopTime.stopID);
			GTFSStop lastStop = id2stop.get(lastStopTime.stopID);
			if(stop!=null && lastStop!=null) {
				GTFSTrip trip = trips.get(tripID);
				GTFSRoute route = routes.get(trip.routeID);
				GTFSEdge e = lastStop.getEdgeTo(stop, net.getNextID(), route, entrainmentMap, net.getPrecisionModel(), net.getSRID());
				ptedges.add(e);
				GTFSConnection c = new GTFSConnection(e, trip.serviceID, trip.tripID, lastStopTime.departureTime, stopTime.arrivalTime);
				connections.add(c);
			}
			lastStopTime = stopTime;		
		}
		
		
		
		
		// check arrival / departure times
		int err = 0;
		for(int i=0; i<connections.size(); ++i) {
			GTFSConnection curr = connections.elementAt(i);
			if(curr.departureTime>curr.arrivalTime) {
				//int n = curr.departureTime;
				//curr.departureTime = curr.arrivalTime;
				//curr.arrivalTime = n;
				//System.err.println("A connection of line " + curr.line + " departs at " + curr.departureTime + " and arrives at " + curr.arrivalTime + ".");
				++err;
				continue;
				//throw new RuntimeException("A connection of line " + curr.line + " departs at " + curr.departureTime + " and arrives at " + curr.arrivalTime + ".");
			}
			if(curr.departureTime!=curr.arrivalTime) {
				continue;
			}
			// patch the departure time if possible
			// search backwards for to find some seconds that can be used
			int ib = i;
			while(ib>=0) {
				GTFSConnection beg = connections.elementAt(ib);
				if(beg.departureTime!=curr.departureTime) {
					break;
				}
				--ib;
			}
			if(ib<0) {
				ib = 0;
			}
			int ie = i;
			while(ie<connections.size()) {
				GTFSConnection end = connections.elementAt(ie);
				if(end.arrivalTime!=curr.arrivalTime) {
					break;
				}
				++ie;
			}
			if(ie==connections.size()) {
				ie = connections.size()-1;
			}
			if(ie==ib) {
				connections.elementAt(i).departureTime -= 15;
				connections.elementAt(i).arrivalTime += 15;
				
			} else {
				int timeSpan = connections.elementAt(ie).arrivalTime - connections.elementAt(ib).departureTime;
				int dt = timeSpan / (ie-ib+1);
				int t = connections.elementAt(ib).departureTime;
				for(int j=ib; j<=ie; ++j) {
					connections.elementAt(j).departureTime = t;
					t = t + dt;
					connections.elementAt(j).arrivalTime = t;
				}
			}
		}
		// insert into edges
		for(GTFSConnection c : connections) {
			c.edge.addConnection(c);
		}
		return err;
	}	

	
	public void sortConnections() {
		for (GTFSEdge e : ptedges) {
			e.sortConnections();
		}
	}
	
}

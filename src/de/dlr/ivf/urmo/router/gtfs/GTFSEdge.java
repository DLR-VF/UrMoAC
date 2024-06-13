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
package de.dlr.ivf.urmo.router.gtfs;

import java.util.Comparator;
import java.util.Set;
import java.util.Vector;

import org.locationtech.jts.geom.LineString;

import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNode;

/**
 * @class GTFSEdge
 * @brief A pt edge - a connection between two GTFSStop instances that may be
 *        traveled using a pt vehicle
 * 
 * This class is a subclass of DBEdge (a usual transport network edge),
 * mainly extended by operating types and accordingly changed travel time
 * function.
 * @author Daniel Krajzewicz
 */
public class GTFSEdge extends DBEdge {
	/// @brief The route that realises this connection
	public GTFSRoute route;
	/// @brief A list of operating times
	private Vector<GTFSConnection> connections = new Vector<>();


	/**
	 * @brief Constructor
	 * @param _numID A numerical id of the edge (internal, used?!!!)
	 * @param _id The id of the edge as given in the db
	 * @param _from A reference to the node this edge starts at (A GTFStop instance)
	 * @param _to A reference to the node this edge ends at (A GTFStop instance)
	 * @param _modes The allowed modes of transport
	 * @param _vmax The maximum velocity allowed at this edge
	 * @param _geom The geometry of this edge
	 * @param _length The length of this edge
	 * @param _route The route that realises this connection between two nodes
	 */
	public GTFSEdge(long _numID, String _id, DBNode _from, DBNode _to, long _modes, double _vmax, LineString _geom,
			double _length, GTFSRoute _route) {
		super(_numID, _id, _from, _to, _modes, _vmax, _geom, _length);
		route = _route;
	}


	/** 
	 * @brief Returns the next available connection at this edge
	 * @param time The time of arrival at this edge/stop
	 * @return The next connection on this edge
	 * @todo: Use a binary search here
	 * @todo: We could dismiss earlier rides when loading
	 * @todo: While progressing, we could keep track of last seen ride
	 */
	public GTFSConnection getConnection(double time) {
		for (GTFSConnection c : connections) {
			if (c.departureTime >= time) {
				return c;
			}
		}
		return null;
	}

		
	/**
	 * @brief Adds a connection (depart/arrival times, route) to this edge
	 * 
	 * Note that the connections must be sorted increasingly by time
	 * @see sortConnections
	 * @param c The connection to add
	 */
	public void addConnection(GTFSConnection c) {
		connections.add(c);
	}


	/**
	 * @brief Sorts the connections by arrival time (increasing)
	 */
	public void sortConnections() {
		connections.sort(new Comparator<GTFSConnection>() {
			@Override
			public int compare(GTFSConnection a1, GTFSConnection a2) {
				if (a1.arrivalTime == a2.arrivalTime) {
					return 0;
				} else if (a1.arrivalTime < a2.arrivalTime) {
					return -1;
				} else {
					return 1;
				}
			}
		});
	}


	/**
	 * @brief Returns the kilocalories used to pass this edge
	 * @param usedMode The used mode of transport
	 * @param tt The travel time at this edge
	 * @return The used kilocalories
	 * 	
	 */
	@Override
	public double getKKC(Mode usedMode, double tt) {
		return Modes.getMode("bus").kkcPerHour * tt / 3600.;
	}

		
	/**
	 * @brief Returns the amount of emitted CO2 in g
	 * @param usedMode The used mode of transport
	 * @return The amount of emitted CO2 in g
	 */
	@Override
	public double getCO2(Mode usedMode) {
		return Modes.getMode("bus").co2PerKm / 1000 * getLength();
	}

		
	/**
	 * @brief Returns the price to pass this edge
	 * @param usedMode The used mode of transport
	 * @param nlines The previously used lines
	 * @return The price to pass this edge
	 */
	@Override
	public double getPrice(Mode usedMode, Set<String> nlines) {
		if(!nlines.contains("pt")) {
			nlines.add("pt");
			return 95;
		}
		return 0;
	}

	
	/**
	 * @brief Returns whether this is a GTFS edge (true)
	 * @return yes, this is a GTFS edge
	 */
	@Override
	public boolean isGTFSEdge() {
		return true;
	}
	
}

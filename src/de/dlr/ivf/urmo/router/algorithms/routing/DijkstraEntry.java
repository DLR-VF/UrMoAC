/*
 * Copyright (c) 2016-2025
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
package de.dlr.ivf.urmo.router.algorithms.routing;

import java.util.HashMap;

import de.dlr.ivf.urmo.router.gtfs.GTFSConnection;
import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNode;

/**
 * @class DijkstraEntry
 * @brief A single Dijkstra step
 * @author Daniel Krajzewicz
 */
public class DijkstraEntry {
	/// @brief Previous entry
	public DijkstraEntry prev;
	/// @brief Reached node
	public DBNode n;
	/// @brief Used edge
	public DBEdge e;
	/// @brief The modes used as last
	public Mode usedMode;
	/// @brief Used GTFS connection
	public GTFSConnection ptConnection;
	/// @brief The traveled distance from the starting point
	public double distance;
	/// @brief The travel time since the begin of the route
	public double tt;
	/// @brief The travel time needed to pass this edge
	public double ttt;
	/// @brief The interchange time at this node
	public double interchangeTT;
	/// @brief Whether the edge was approached using the opposite direction edge
	public boolean wasOpposite;
	/// @brief Additional measures for weighting the route
	public HashMap<String, Object> measures;
	/// @brief Reference to the first entry
	public DijkstraEntry first = null;
	

	/** @brief Constructor
	 * @param measure The route weighting function
	 * @param _prev The previous edge
	 * @param _n The last node
	 * @param _e The current edge
	 * @param _usedMode The currently used mode
	 * @param _distance The overall distance
	 * @param _tt The travel time on this edge
	 * @param _ptConnection The used pt line
	 * @param _ttt The overall travel time
	 * @param _interchangeTT Time needed for the interchange
	 * @param _wasOpposite Whether it is the opposite direction of the current edge
	 */
	public DijkstraEntry(AbstractRouteWeightFunction measure, DijkstraEntry _prev, DBNode _n, DBEdge _e, Mode _usedMode, 
			double _distance, double _tt, GTFSConnection _ptConnection, double _ttt, double _interchangeTT, boolean _wasOpposite) {
		prev = _prev;
		n = _n;
		e = _e;
		distance = _distance;
		tt = _tt;
		ttt = _ttt;
		usedMode = _usedMode;
		ptConnection = _ptConnection;
		interchangeTT = _interchangeTT;
		wasOpposite = _wasOpposite;
		measures = measure.buildMeasures(_prev, this);
		if(prev==null) {
			first = this;
		} else {
			first = prev.first;
		}
	}


	/** @brief Constructor
	 * @param orig The DijkstraEntry to copy values from
	 * @param mode The mode to set as being currently used
	 */
	@SuppressWarnings("unchecked")
	public DijkstraEntry(DijkstraEntry orig, Mode mode) {
		prev = orig.prev;
		n = orig.n;
		e = orig.e;
		distance = orig.distance;
		tt = orig.tt;
		ttt = orig.ttt;
		usedMode = mode;
		ptConnection = orig.ptConnection;
		interchangeTT = orig.interchangeTT;
		wasOpposite = orig.wasOpposite;
		measures = orig.measures==null ? null : (HashMap<String, Object>) orig.measures.clone();
		first = orig.first;
	}

	
	
	/** 
	 * @brief Returns the name for the used line
	 * @return The name of the used line
	 */
	public String buildLineModeID() {
		if(ptConnection==null) {
			return usedMode.mml;
		}
		return ptConnection.trip.route.id;
	}


}

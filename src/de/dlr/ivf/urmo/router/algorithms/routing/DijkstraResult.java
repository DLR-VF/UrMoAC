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
package de.dlr.ivf.urmo.router.algorithms.routing;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class DijkstraResult
 * @brief A storage for routing results (one source to multiple destinations)
 * 
 * The Dijkstra results stores subsequently filled access from a single origin to a set of destinations.
 *  
 * @author Daniel Krajzewicz
 */
public class DijkstraResult {
	/// @brief The origin of routing
	public MapResult origin;
	/// @brief A map of edges to their accessibility measures
	public HashMap<DBEdge, DijkstraEntry> edgeMap = new HashMap<>();
	/// @brief Sum of seen destination weights
	public double seenVar = 0;
	/// @brief Number of destinations to find (-1 if not used)
	public int boundNumber;
	/// @brief Maximum travel time (-1 if not used)
	public double boundTT;
	/// @brief Maximum distance (-1 if not used)
	public double boundDist;
	/// @brief Maximum weight sum to find (-1 if not used)
	public double boundVar;
	/// @brief Whether only the next item shall be found
	public boolean shortestOnly;
	/// @brief Starting time
	public int time;
	/// @brief Map of seen destinations to the paths to them
	public Map<MapResult, SingleODResult> seen = new HashMap<>();
	
	
	/** @brief Constructor
	 * @param _toFind List of objects to find
	 * @param _boundNumber Number of destinations to find (-1 if not used)
	 * @param _boundTT Maximum travel time (-1 if not used)
	 * @param _boundDist Maximum distance (-1 if not used)
	 * @param _boundVar Maximum weight sum to find (-1 if not used)
	 * @param _shortestOnly Whether only the next item shall be found
	 * @param _time Starting time
	 */
	public DijkstraResult(MapResult _origin, int _boundNumber, double _boundTT, double _boundDist, 
			double _boundVar, boolean _shortestOnly, int _time) {
		origin = _origin;
		boundNumber = _boundNumber;
		boundTT = _boundTT;
		boundDist = _boundDist;
		boundVar = _boundVar;
		shortestOnly = _shortestOnly;
		time = _time;
	}
	

	/** @brief Adds the information about an accessed edge
	 * @param measure The routing weight function to use
	 * @param oe The accessed edge
	 * @param newValue The routing element used to approach the edge
	 * @return Whether all needed destinations were found
	 */
	public boolean visitEdge(AbstractRouteWeightFunction measure, DBEdge oe, DijkstraEntry newValue, HashMap<DBEdge, Vector<MapResult>> nearestToEdges) {
		// check only edges that have attached destinations
		if(oe.getAttachedObjectsNumber()==0) {
			return false;
		}
		// add the way to this edge if it's the first or the best one
		boolean isUpdate = edgeMap.containsKey(oe);
		if(!isUpdate || measure.compare(edgeMap.get(oe), newValue)>0) { // !!! on an edge base? 
			edgeMap.put(oe, newValue);
			Vector<MapResult> toObjects = nearestToEdges.get(oe);
			for(MapResult mr : toObjects) {
				LayerObject lo = (LayerObject) mr.em;
				SingleODResult path = new SingleODResult(origin, mr, newValue, time);
				seen.put(mr, path);
				if(!isUpdate) {
					seenVar += lo.getAttachedValue();
				}
			}
		}
		if (shortestOnly&&seen.size()>0) {
			return true;
		}
		// nope, we have seen the wanted number of elements
		if (boundNumber > 0 && seen.size() >= boundNumber) {
			return true;
		}
		// nope, we have seen the number of values to find
		if (boundVar > 0 && seenVar >= boundVar) {
			return true;
		}
		return false;
	}

	
	/** @brief Returns the path to the given destination
	 * 
	 * @param to The destination to get the path to
	 * @return The path to the given destination
	 */
	public SingleODResult getResult(MapResult to) {
		return seen.get(to);
	}
	

	/** @brief Returns the seen destinations
	 * 
	 * @return All seen destinations
	 */
	public Vector<MapResult> getSeenDestinations() {
		Vector<MapResult> ret = new Vector<>();
		ret.addAll(seen.keySet());
		Collections.sort(ret, new Comparator<MapResult>() {
			@Override
			public int compare(MapResult o1, MapResult o2) {
				long id1 = o1.em.getOuterID();
				long id2 = o2.em.getOuterID();
				return id1 < id2 ? -1 : id1 > id2 ? 1 : 0;
			}
		});
		return ret;
	}
	
}

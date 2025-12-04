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
import java.util.Set;
import java.util.Vector;

import de.dlr.ivf.urmo.router.output.SingleResultComparator_DestinationID;
import de.dlr.ivf.urmo.router.output.SingleResultComparator_TT;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/** @class DijkstraResultsStorage_MultiType
 * @brief A storage for Dijkstra results.
 * 
 * This class stores the results assuming different types of destination exist.
 * @see DijkstraResultsStorage
 * @author Daniel Krajzewicz
 */
public class DijkstraResultsStorage_MultiType extends DijkstraResultsStorage {
	/// @brief Map from types to results
	HashMap<String, ResultSet> seen = new HashMap<>();
	/// @brief A map from type ID to the respective type names
	HashMap<Long, Set<String>> destTypes;
	
	
	/** @brief Constructor
	 * 
	 * @param _boundNumber The maximum number of destinations to find
	 * @param _boundTT The maximum travel time bounds
	 * @param _boundDist The maximum distance bounds
	 * @param _boundVar The maximum value bounds
	 * @param _shortestOnly Whether only the nearest destination shall be found
	 * @param _destTypes The map from type ID to the respective type names
	 */
	public DijkstraResultsStorage_MultiType(int _boundNumber, double _boundTT, double _boundDist, 
			double _boundVar, boolean _shortestOnly, HashMap<Long, Set<String>> _destTypes) {
		super(_boundNumber, _boundTT, _boundDist, _boundVar, _shortestOnly);
		for(Long obj : _destTypes.keySet()) {
			for(String type : _destTypes.get(obj)) {
				if(!seen.containsKey(type)) {
					seen.put(type, new DijkstraResultsStorage.ResultSet());
				}
			}
		}
		destTypes = _destTypes;
	}


	/** @brief Adds a result
	 * 
	 * @param lo The destination to add
	 * @param path The path to the destination
	 */
	public void addResult(LayerObject lo, SingleODResult path) {
		for(String type : destTypes.get(path.destination.em.getOuterID())) {
			seen.get(type)._addResult(lo, path);
		}
	}

	
	/** @brief Returns whether the search can be finished
	 * @return Whether all destinations were found
	 */
	public boolean finished() {
		for(String type : seen.keySet()) {
			if(!seen.get(type)._finished()) {
				return false;
			}
		}
		return true;
	}


	/** @brief Collects the results
	 * 
	 * @param comparator The comparator used to sort the results
	 * @param sorter The sorting comparator
	 * @param needsPT Whether only paths that contain public transport shall be returned
	 * @param singleDestination The destination to find explicitly
	 * @return The sorted list of found destinations
	 */
	public Vector<SingleODResult> collectResults(SingleResultComparator_TT comparator, SingleResultComparator_DestinationID sorter, boolean needsPT, long singleDestination) {
		Vector<SingleODResult> ret = new Vector<>();
		for(String type : seen.keySet()) {
			if(!seen.containsKey(type)) {
				continue;
			}
			ResultSet rs = seen.get(type);
			Vector<SingleODResult> typeResults = rs.getResults(comparator, needsPT, singleDestination);
			typeResults.sort(comparator);
			typeResults = filterResults(typeResults);
			typeResults.sort(sorter);
			ret.addAll(typeResults);
		}
		return ret;
	}
	
	
	
}

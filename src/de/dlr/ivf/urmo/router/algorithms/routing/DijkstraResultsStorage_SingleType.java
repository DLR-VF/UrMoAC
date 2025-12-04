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

import java.util.Vector;

import de.dlr.ivf.urmo.router.output.SingleResultComparator_DestinationID;
import de.dlr.ivf.urmo.router.output.SingleResultComparator_TT;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/** @class DijkstraResultsStorage_MultiType
 * @brief A storage for Dijkstra results.
 * 
 * This class stores the results assuming only one type of destinations.
 * @see DijkstraResultsStorage
 * @author Daniel Krajzewicz
 */
public class DijkstraResultsStorage_SingleType extends DijkstraResultsStorage {
	/// @brief The results storage
	ResultSet seen;
	
	
	/** @brief Constructor
	 * 
	 * @param _boundNumber The maximum number of destinations to find
	 * @param _boundTT The maximum travel time bounds
	 * @param _boundDist The maximum distance bounds
	 * @param _boundVar The maximum value bounds
	 * @param _shortestOnly Whether only the nearest destination shall be found
	 */
	public DijkstraResultsStorage_SingleType(int _boundNumber, double _boundTT, double _boundDist, 
			double _boundVar, boolean _shortestOnly) {
		super(_boundNumber, _boundTT, _boundDist, _boundVar, _shortestOnly);
		seen = new DijkstraResultsStorage.ResultSet();
	}


	/** @brief Adds a result
	 * 
	 * @param lo The destination to add
	 * @param path The path to the destination
	 */
	public void addResult(LayerObject lo, SingleODResult path) {
		seen._addResult(lo, path);
	}

	
	/** @brief Returns whether the search can be finished
	 * @return Whether all destinations were found
	 */
	public boolean finished() {
		return seen._finished();
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
		Vector<SingleODResult> results = seen.getResults(comparator, needsPT, singleDestination);
		results.sort(comparator);
		results = filterResults(results);
		results.sort(sorter);
		return results;
	}
	

}

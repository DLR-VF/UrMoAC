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
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/** @class BoundDijkstraBase
 * @brief A 1-to-many Dijkstra that may be bound by some values
 * @author Daniel Krajzewicz
 * @todo Check which parameter should be included in the constructor and which in the run method
 */
public abstract class BoundDijkstraBase implements IBoundDijkstra {
	/// @brief The origin of routing
	protected MapResult origin;
	/// @brief Starting time
	protected int time;
	/// @brief The route weighting function to use
	protected AbstractRouteWeightFunction measure = null;
	/// @brief The priority queue holding the next elements to process
	protected PriorityQueue<DijkstraEntry> next = null;
	/// @brief Information about visited edges
	protected Map<DBEdge, DijkstraEntry> edgeMap = new HashMap<>();
	/// @brief The seen destinations with paths to them
	protected DijkstraResultsStorage seen;
	/// @brief The maximum travel time to bound the search by
	protected double boundTT;
	/// @brief The maximum distance to bound the search by
	protected double boundDist;

	
	/** @brief Constructor
	 * @param _measure The route weighting function to use
	 * @param _origin The origin of routing
	 * @param _boundNumber Number of destinations to find (-1 if not used)
	 * @param _boundTT Maximum travel time (-1 if not used)
	 * @param _boundDist Maximum distance (-1 if not used)
	 * @param _boundVar Maximum weight sum to find (-1 if not used)
	 * @param _shortestOnly Whether only the next item shall be found
	 * @param _time Starting time
	 * @param destTypes Map of destination types
	 */
	public BoundDijkstraBase(AbstractRouteWeightFunction _measure, MapResult _origin, int _boundNumber, double _boundTT, double _boundDist, 
			double _boundVar, boolean _shortestOnly, int _time, HashMap<Long, Set<String>> destTypes) {
		origin = _origin;
		time = _time;
		measure = _measure;
		next = new PriorityQueue<DijkstraEntry>(1000, _measure);
		if(destTypes==null) {
			seen = new DijkstraResultsStorage_SingleType(_boundNumber, _boundTT, _boundDist, _boundVar, _shortestOnly);
		} else {
			seen = new DijkstraResultsStorage_MultiType(_boundNumber, _boundTT, _boundDist, _boundVar, _shortestOnly, destTypes);
		}
		boundTT = _boundTT;
		boundDist = _boundDist;
	}

	
	/** @brief Performs the search / routing
	 * @param ends The destination candidates
	 * @param nearestFromEdges The map from edges to destinations
	 * @return The results of the Dijkstra search
	 */
	public abstract DijkstraResultsStorage run(Set<DBEdge> ends, HashMap<DBEdge, Vector<MapResult>> nearestFromEdges);
	
	
	/** @brief Adds the information about the first edge
	 * 
	 * @param measure The routing weight function to use
	 * @param oe The accessed edge
	 * @param newValue The routing element used to approach the edge
	 * @param edges2dests The map from edges to destinations
	 * @param isOpposite Whether the edge is at the opposite side of the accessed edge
	 * @return Whether all needed destinations were found
	 */
	public boolean visitFirstEdge(AbstractRouteWeightFunction measure, DBEdge oe, DijkstraEntry newValue, HashMap<DBEdge, Vector<MapResult>> edges2dests, boolean isOpposite) {
		// check only edges that have attached destinations
		if(oe.getAttachedObjectsNumber()==0) {
			return false;
		}
		Vector<MapResult> toObjects = edges2dests.get(oe);
		for(MapResult mr : toObjects) {
			LayerObject lo = (LayerObject) mr.em;
			SingleODResult path = new SingleODResult(origin, mr, newValue, time);
			seen.addResult(lo, path);
		}
		return seen.finished();
	}


	/** @brief Adds the information about an accessed edge
	 * 
	 * For the first edge and its opposite edge, it performs a comparison for the positions --> visitFirstEdge
	 * 
	 * @param measure The routing weight function to use
	 * @param oe The accessed edge
	 * @param newValue The routing element used to approach the edge
	 * @param edges2dests The map from edges to destinations
	 * @return Whether all needed destinations were found
	 */
	public boolean visitEdge(AbstractRouteWeightFunction measure, DBEdge oe, DijkstraEntry newValue, HashMap<DBEdge, Vector<MapResult>> edges2dests) {
		// check only edges that have attached destinations
		if(oe.getAttachedObjectsNumber()==0) {
			return false;
		}
		// add the way to this edge if it's the first or the best one
		boolean isUpdate = edgeMap.containsKey(oe);
		if(!isUpdate || measure.compare(edgeMap.get(oe), newValue)>=0) { // !!! on an edge base? 
			edgeMap.put(oe, newValue);
			Vector<MapResult> toObjects = edges2dests.get(oe);
			for(MapResult mr : toObjects) {
				LayerObject lo = (LayerObject) mr.em;
				SingleODResult path = new SingleODResult(origin, mr, newValue, time);
				seen.addResult(lo, path);
			}
		}
		return seen.finished();
	}
	
	
	/** @brief Returns the number of visited edges (with destinations)
	 * @return The number of seen edges with destinations
	 */
	public long getSeenEdgesNum() {
		return edgeMap.size();
	}

}

/*
 * Copyright (c) 2016-2023 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * https://github.com/DLR-VF/UrMoAC
 * Licensed under the Eclipse Public License 2.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.algorithms.routing;

import java.util.HashMap;
import java.util.HashSet;

import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNode;

/**
 * @class DijkstraResult
 * @brief A storage for dijkstra results (1-to-many)
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class DijkstraResult {
	/// @brief A map of nodes to their accessibility measures
	public HashMap<DBNode, HashMap<Long, DijkstraEntry> > nodeMap = new HashMap<DBNode, HashMap<Long, DijkstraEntry>>();
	/// @brief A map of edges to their accessibility measures
	public HashMap<DBEdge, DijkstraEntry> edgeMap = new HashMap<>();
	/// @brief The number of seen objects
	public int seenObjects = 0;
	/// @brief Sum of seen destination weights
	public double seenVar = 0;
	/// @brief List of edges to find/visit
	public HashSet<DBEdge> toFind;
	/// @brief Number of destinations to find (-1 if not used)
	public int boundNumber;
	/// @brief Maximum travel time (-1 if not used)
	public int boundTT;
	/// @brief Maximum distance (-1 if not used)
	public int boundDist;
	/// @brief Maximum weight sum to find (-1 if not used)
	public double boundVar;
	/// @brief Whether only the next item shall be found
	public boolean shortestOnly;
	/// @brief Starting time
	public int time;
	/// @brief The result seen as last
	public DijkstraEntry lastSeen = null;

	
	/** @brief Contructor
	 * @param _toFind List of edges to find/visit
	 * @param _boundNumber Number of destinations to find (-1 if not used)
	 * @param _boundTT Maximum travel time (-1 if not used)
	 * @param _boundDist Maximum distance (-1 if not used)
	 * @param _boundVar Maximum weight sum to find (-1 if not used)
	 * @param _shortestOnly Whether only the next item shall be found
	 * @param _time Starting time
	 */
	public DijkstraResult(HashSet<DBEdge> _toFind, int _boundNumber, double _boundTT, double _boundDist, 
			double _boundVar, boolean _shortestOnly, int _time) {
		toFind = _toFind;
		boundNumber = _boundNumber;
		boundVar = _boundVar;
		shortestOnly = _shortestOnly;
		time = _time;
	}
	

	/** @brief Adds the information about an accessed node
	 * @param node The seen node
	 * @param availableModes The still available modes
	 * @param m The routing result
	 * @todo Recheck whether the mode is needed
	 */
	public void addNodeInfo(DBNode node, long availableModes, DijkstraEntry m) {
		if(!nodeMap.containsKey(node)) {
			nodeMap.put(node, new HashMap<Long, DijkstraEntry>());
		}
		HashMap<Long, DijkstraEntry> nodeVals = nodeMap.get(node);
		nodeVals.put(availableModes, m);
	}
	
	
	/** @brief Returns the information about the prior node
	 * @param node The accessed node
	 * @param availableModes The still available modes
	 * @return The prior node used to access the given one using the given modes
	 */
	public DijkstraEntry getPriorNodeInfo(DBNode node, long availableModes) {
		if(!nodeMap.containsKey(node)) {
			return null;
		}
		HashMap<Long, DijkstraEntry> nodeVals = nodeMap.get(node);
		if(!nodeVals.containsKey(availableModes)) {
			return null;
		}
		return nodeVals.get(availableModes); 
	}


	/** @brief Adds the information about an accessed edge
	 * @param measure The routing weight function to use
	 * @param oe The accessed edge
	 * @param newValue The routing element used to approach the edge
	 * @return Whether all needed destinations were found
	 */
	public boolean addEdgeInfo(AbstractRouteWeightFunction measure, DBEdge oe, DijkstraEntry newValue) {
		// check only edges that have attached destinations
		if(oe.getAttachedObjectsNumber()==0) {
			return false;
		}
		///
		if(!toFind.contains(oe)) {
			return false;
		}
		// add the fastest way to the edge, update seen objects and value if 
		// the edge is visited for the first time 
		if(!edgeMap.containsKey(oe)) {
			edgeMap.put(oe, newValue);
			seenObjects += oe.getAttachedObjectsNumber();
			seenVar += oe.getAttachedValues();
			toFind.remove(oe);
			if (shortestOnly) {
				return true;
			}
			// nope, we do not have anything more to find
			if (toFind.size() == 0) {
				return true;
			}
			// nope, we have seen the wanted number of elements
			if (boundNumber > 0 && seenObjects >= boundNumber) {
				return true;
			}
			// nope, we have seen the number of values to find
			if (boundVar > 0 && seenVar >= boundVar) {
				return true;
			}
		} else if(measure.compare(edgeMap.get(oe), newValue)>0) {
			// we've seen this already before...
			// TODO !!!: eigentlich: fuer jede Quelle/Ziel-Beziehung pruefen, ob die Reisezeit hoeher als die vorherige ist oder nicht
			edgeMap.put(oe, newValue);
		}
		lastSeen = newValue;
		return false;
	}

	
	/** @brief Returns the information how the given edge was accessed
	 * @param edge The approached edge
	 * @return Information how this edge was approached 
	 */
	public DijkstraEntry getEdgeInfo(DBEdge edge) {
		return edgeMap.get(edge);
	}
	
	
	/** @brief Returns whether all destination edges were found
	 * @todo Why is this needed, but not other limits
	 * @return Whether all destination edges were found
	 */
	public boolean allFound() {
		return toFind.size()==0;
	}

}

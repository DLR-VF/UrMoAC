/**
 * Copyright (c) 2016-2021 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * @author: Daniel.Krajzewicz@dlr.de
 * Licensed under the GNU General Public License v3.0
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
	/// @brief A map of node to their accessibility measures
	public HashMap<DBNode, HashMap<Long, DijkstraEntry> > nodeMap = new HashMap<DBNode, HashMap<Long, DijkstraEntry>>();
	public HashMap<DBEdge, DijkstraEntry> edgeMap = new HashMap<>();
	public int seenObjects = 0;
	public double seenVar = 0;
	public HashSet<DBEdge> toFind;
	public int boundNumber;
	public int boundTT;
	public int boundDist;
	public double boundVar;
	public boolean shortestOnly;
	public int time;
	
	public DijkstraEntry lastSeen = null;

	
	public DijkstraResult(HashSet<DBEdge> _toFind, int _boundNumber, double _boundTT, double _boundDist, 
			double _boundVar, boolean _shortestOnly, int _time) {
		toFind = _toFind;
		boundNumber = _boundNumber;
		boundVar = _boundVar;
		shortestOnly = _shortestOnly;
		time = _time;
	}
	

	public void addNodeInfo(DBNode node, long availableModes, DijkstraEntry m) {
		if(!nodeMap.containsKey(node)) {
			nodeMap.put(node, new HashMap<Long, DijkstraEntry>());
		}
		HashMap<Long, DijkstraEntry> nodeVals = nodeMap.get(node);
		nodeVals.put(availableModes, m);
	}
	
	
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


	public boolean addEdgeInfo(AbstractRoutingMeasure measure, DBEdge oe, DijkstraEntry newValue) {
		// check only edges that have attached destinations
		if(oe.getAttachedObjectsNumber()==0) {
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

	
	public DijkstraEntry getEdgeInfo(DBEdge edge) {
		return edgeMap.get(edge);
	}
	
	
	public boolean allFound() {
		return toFind.size()==0;
	}

}

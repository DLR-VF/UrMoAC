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

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNode;

/** @class BoundDijkstra_UniModal
 * @brief A 1-to-many Dijkstra that may be bound by some values (unimodal variant)
 * @author Daniel Krajzewicz
 * @todo Check which parameter should be included in the constructor and which in the run method
 */
public class BoundDijkstra_UniModal extends BoundDijkstraBase {
	private Mode mode;
	/// @brief The information about node access
	private HashMap<DBNode, DijkstraEntry> nodeMap = new HashMap<DBNode, DijkstraEntry>();
	
	
	/** @brief Constructor
	 * @param _mode The mode to use
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
	public BoundDijkstra_UniModal(Mode _mode, AbstractRouteWeightFunction _measure, MapResult _origin, int _boundNumber, double _boundTT, double _boundDist, 
			double _boundVar, boolean _shortestOnly, int _time, HashMap<Long, Set<String>> destTypes) {
		super(_measure, _origin, _boundNumber, _boundTT, _boundDist, _boundVar, _shortestOnly, _time, destTypes);
		mode = _mode;
	}

	
	
	/**
	 * @brief Computes a bound 1-to-many shortest paths using the Dijkstra algorithm (unimodal variant)
	 * 
	 * @param ends The destination candidates
	 * @param edges2dests The map from edges to destinations
	 * @return The results of the Dijkstra search
	 */
	@Override
	public DijkstraResultsStorage run(Set<DBEdge> ends, HashMap<DBEdge, Vector<MapResult>> edges2dests) {
		boolean hadExtension = false;
		DBEdge startEdge = origin.edge;
		double tt = startEdge.getTravelTime(mode.vmax, time) * (startEdge.getLength()-origin.pos) / startEdge.getLength();
		DijkstraEntry nm = new DijkstraEntry(measure, null, startEdge.getToNode(), startEdge, mode,
				(startEdge.getLength()-origin.pos), tt, null, tt, 0, false);
		addNodeInfo(startEdge.getToNode(), nm);
		next.add(nm);
		if(visitFirstEdge(measure, startEdge, nm, edges2dests, false)) {
			boundTT = Math.max(boundTT, startEdge.getTravelTime(mode.vmax, time));
			hadExtension = true; // there won't be a better way
		} 
		// consider starting in the opposite direction
		if(startEdge.getOppositeEdge()!=null) {
			DBEdge e = startEdge.getOppositeEdge();
			tt = e.getTravelTime(mode.vmax, time) * origin.pos / e.getLength();
			nm = new DijkstraEntry(measure, null, e.getToNode(), e, mode, (origin.pos), tt, null, tt, 0, true);
			next.add(nm);
			addNodeInfo(e.getToNode(), nm);
			if(visitFirstEdge(measure, e, nm, edges2dests, true)) {
				if(!hadExtension) {
					boundTT = Math.max(boundTT, e.getTravelTime(mode.vmax, time));
					hadExtension = true; // there won't be a better way
				}
			}
		}
		
		while (!next.isEmpty()) {
			DijkstraEntry nns = next.poll();
			// check bounds
			if (boundTT >= 0 && nns.tt > boundTT) {
				continue;
			}
			if (boundDist >= 0 && nns.distance > boundDist) {
				continue;
			}
			// iterate over outgoing edges
			Vector<DBEdge> oes = nns.n.getOutgoing();
			for (DBEdge oe : oes) {
				double edge_tt = oe.getTravelTime(mode.vmax, time + nns.tt) + nns.e.getCrossingTimeTo(oe);
				DBNode n = oe.getToNode();
				double distance = nns.distance + oe.getLength();
				double ctt = nns.tt + edge_tt;
				DijkstraEntry oldValue = getPriorNodeInfo(n);
				DijkstraEntry newValue = new DijkstraEntry(measure, nns, n, oe, mode, distance, ctt, null, edge_tt, 0, false);
				if(oldValue==null) {
					next.add(newValue);
					addNodeInfo(n, newValue);
				} else if(measure.compare(oldValue, newValue)>0) {
					next.remove(oldValue);
					next.add(newValue);
					addNodeInfo(n, newValue);
				}
				if(visitEdge(measure, oe, newValue, edges2dests)) {
					if(!hadExtension) {
						boundTT = Math.max(boundTT, ctt+newValue.first.e.getTravelTime(mode.vmax, time));
						hadExtension = true;
					}
				}
				// check opposite direction
				if(oe.getOppositeEdge()!=null && oe.getOppositeEdge().getAttachedObjectsNumber()!=0) {
					// !!! add edge crossing time
					DijkstraEntry newOppositeValue = new DijkstraEntry(measure, nns, n, oe.getOppositeEdge(), mode, distance, ctt, null, edge_tt, 0, true);
					if(visitEdge(measure, oe.getOppositeEdge(), newOppositeValue, edges2dests)) {
						if(!hadExtension) {
							boundTT = Math.max(boundTT, ctt+newOppositeValue.first.e.getTravelTime(mode.vmax, time));
							hadExtension = true;
						}
					}
				}
				
			}
		}
		return seen;
	}
	


	/** @brief Adds the information about the access to a node
	 * 
	 * @param node The node to add the information about
	 * @param m The path to the node
	 */
	public void addNodeInfo(DBNode node, DijkstraEntry m) {
		nodeMap.put(node, m);
	}
	
	
	/** @brief Returns the information about a previously visited node
	 * @param node The accessed node
	 * @return The prior node used to access the given one using the given modes
	 */
	public DijkstraEntry getPriorNodeInfo(DBNode node) {
		if(!nodeMap.containsKey(node)) {
			return null;
		}
		return nodeMap.get(node); 
	}


	/** @brief Returns the number of visited nodes
	 * @return The number of visited nodes
	 */
	public long getSeenNodesNum() {
		return nodeMap.size();
	}


}

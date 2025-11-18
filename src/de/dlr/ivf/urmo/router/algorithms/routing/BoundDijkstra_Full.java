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

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.gtfs.GTFSConnection;
import de.dlr.ivf.urmo.router.gtfs.GTFSEdge;
import de.dlr.ivf.urmo.router.gtfs.GTFSStop;
import de.dlr.ivf.urmo.router.gtfs.GTFSTrip;
import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNode;

/** @class BoundDijkstra_Full
 * @brief A 1-to-many Dijkstra that may be bound by some values (intermodal variant)
 * @author Daniel Krajzewicz
 * @todo Check which parameter should be included in the constructor and which in the run method
 */
public class BoundDijkstra_Full extends BoundDijkstraBase {
	private Vector<Mode> modes;
	/// @brief The information about node access
	private HashMap<DBNode, HashMap<Mode, DijkstraEntry> > nodeMap = new HashMap<DBNode, HashMap<Mode, DijkstraEntry>>();
	
	
	/** @brief Constructor
	 * @param _modes The list of sable modes
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
	public BoundDijkstra_Full(Vector<Mode> _modes, AbstractRouteWeightFunction _measure, MapResult _origin, int _boundNumber, double _boundTT, double _boundDist, 
			double _boundVar, boolean _shortestOnly, int _time, HashMap<Long, Set<String>> destTypes) {
		super(_measure, _origin, _boundNumber, _boundTT, _boundDist, _boundVar, _shortestOnly, _time, destTypes);
		modes = _modes;
	}

	
	
	/**
	 * @brief Computes a bound 1-to-many shortest paths using the Dijkstra algorithm (intermodal variant)
	 * 
	 * @param ends The destination candidates
	 * @param edges2dests The map from edges to destinations
	 * @return The results of the Dijkstra search
	 */
	@Override
	public DijkstraResultsStorage run(Set<DBEdge> ends, HashMap<DBEdge, Vector<MapResult>> edges2dests) {
		boolean hadExtension = false;
		DBEdge startEdge = origin.edge;
		for(Mode usedMode : modes) {
			if(!startEdge.allows(usedMode)) {
				continue;
			}
			double tt = startEdge.getTravelTime(usedMode.vmax, time) * (startEdge.getLength()-origin.pos) / startEdge.getLength();
			DijkstraEntry nm = new DijkstraEntry(measure, null, startEdge.getToNode(), startEdge, usedMode,
					(startEdge.getLength()-origin.pos), tt, null, tt, 0, false);
			addNodeInfo(startEdge.getToNode(), usedMode, nm);
			addModalVariants(nm);
			if(visitFirstEdge(measure, startEdge, nm, edges2dests, false)) {
				boundTT = Math.max(boundTT, startEdge.getTravelTime(usedMode.vmax, time));
				hadExtension = true; // there won't be a better way
			} 
		}
		// consider starting in the opposite direction
		if(startEdge.getOppositeEdge()!=null) {
			DBEdge e = startEdge.getOppositeEdge();
			for(Mode usedMode : modes) {
				if(!e.allows(usedMode)) {
					continue;
				}
				double tt = e.getTravelTime(usedMode.vmax, time) * (origin.pos) / e.getLength();
				DijkstraEntry nm = new DijkstraEntry(measure, null, e.getToNode(), e, usedMode, (origin.pos), tt, null, tt, 0, true);
				next.add(nm);
				addNodeInfo(e.getToNode(), usedMode, nm);
				if(visitFirstEdge(measure, e, nm, edges2dests, true)) {
					if(!hadExtension) {
						boundTT = Math.max(boundTT, e.getTravelTime(usedMode.vmax, time));
						hadExtension = true; // there won't be a better way
					}
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
				Mode usedMode = nns.usedMode;
				if (!oe.allows(usedMode)) {
					continue;
				}
				GTFSConnection ptConnection = null;
				double edge_tt = 0;
				double interchangeTT = 0;
				if(oe.isGTFSEdge()) {
					GTFSEdge ge = (GTFSEdge) oe;
					// @todo: this is not correct, the interchange should be regarded here, not in the ttt computation below
					// @todo: how to select the trip continuation first?
					ptConnection = ge.getConnection(time + nns.tt);
					if(ptConnection==null) {
						// @todo: what about connections during the next day?
						continue; // no valid pt connection
					}
					GTFSTrip prevTrip = nns.ptConnection!=null ? nns.ptConnection.trip : null;
					if(!ptConnection.trip.equals(prevTrip)) {
						interchangeTT = ((GTFSStop) nns.n).getInterchangeTime(ptConnection.trip, prevTrip, 0);
					}
					edge_tt = ptConnection.arrivalTime - time - nns.tt + interchangeTT;
				} else {
					edge_tt = oe.getTravelTime(usedMode.vmax, time + nns.tt) + nns.e.getCrossingTimeTo(oe);
					// @todo: interchange times at nodes
				}
				DBNode n = oe.getToNode();
				double distance = nns.distance + oe.getLength();
				double ctt = nns.tt + edge_tt;
				DijkstraEntry oldValue = getPriorNodeInfo(n, usedMode);
				DijkstraEntry newValue = new DijkstraEntry(measure, nns, n, oe, usedMode, distance, ctt, ptConnection, edge_tt, interchangeTT, false);
				if(oldValue==null) {
					addModalVariants(newValue);
					addNodeInfo(n, usedMode, newValue);
				} else if(measure.compare(oldValue, newValue)>0) {
					next.remove(oldValue);
					addModalVariants(newValue);
					addNodeInfo(n, usedMode, newValue);
				}
				if(visitEdge(measure, oe, newValue, edges2dests)) {
					if(!hadExtension) {
						boundTT = Math.max(boundTT, ctt+newValue.first.e.getTravelTime(newValue.first.usedMode.vmax, time));
						hadExtension = true;
					}
				}
				// check opposite direction
				if(oe.getOppositeEdge()!=null && oe.getOppositeEdge().getAttachedObjectsNumber()!=0) {
					DijkstraEntry newOppositeValue = new DijkstraEntry(measure, nns, n, oe.getOppositeEdge(), usedMode, distance, ctt, ptConnection, edge_tt, interchangeTT, true);
					if(visitEdge(measure, oe.getOppositeEdge(), newOppositeValue, edges2dests)) {
						if(!hadExtension) {
							boundTT = Math.max(boundTT, ctt+newOppositeValue.first.e.getTravelTime(newOppositeValue.first.usedMode.vmax, time));
							hadExtension = true;
						}
					}
				}
				
			}
		}
		return seen;
	}
	

	private void addModalVariants(DijkstraEntry entry) {
		next.add(entry);
		// no mode change possible at the current node
		if(!entry.n.allowsModeChange()) {
			return;
		}
		// check which changes are possible
		Vector<DBNode.AllowedModeChange> allowedChanges = entry.n.getAllowedModeChanges();
		for (DBNode.AllowedModeChange mc : allowedChanges) {
			if(mc.getFromMode()!=entry.usedMode.id) { // !!! make a hashmap of from modes!?
				// not the starting one
				continue;
			}
			long toModeID = mc.getToMode();
			Mode toMode = Modes.getMode(toModeID);
			DijkstraEntry newEntry = new DijkstraEntry(entry, toMode);
			next.add(newEntry);
		}
	}



	/** @brief Adds the information about the access to a node
	 * 
	 * @param node The node to add the information about
	 * @param mode The used mode
	 * @param m The path to the node
	 */
	public void addNodeInfo(DBNode node, Mode mode, DijkstraEntry m) {
		if(!nodeMap.containsKey(node)) {
			nodeMap.put(node, new HashMap<Mode, DijkstraEntry>());
		}
		HashMap<Mode, DijkstraEntry> nodeVals = nodeMap.get(node);
		nodeVals.put(mode, m);
	}
	
	
	/** @brief Returns the information about a previously visited node
	 * @param node The accessed node
	 * @param mode The used mode
	 * @return The prior node used to access the given one using the given modes
	 */
	public DijkstraEntry getPriorNodeInfo(DBNode node, Mode mode) {
		if(!nodeMap.containsKey(node)) {
			return null;
		}
		HashMap<Mode, DijkstraEntry> nodeVals = nodeMap.get(node);
		if(!nodeVals.containsKey(mode)) {
			return null;
		}
		return nodeVals.get(mode); 
	}


	/** @brief Returns the number of visited nodes
	 * @return The number of visited nodes
	 */
	public long getSeenNodesNum() {
		return nodeMap.size();
	}

}

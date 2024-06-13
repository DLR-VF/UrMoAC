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
import java.util.PriorityQueue;
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

/**
 * @brief A 1-to-many Dijkstra that may be bound by some values
 * @author Daniel Krajzewicz
 */
public class BoundDijkstra {
	/**
	 * @brief Computes a bound 1-to-many shortest paths using the Dijkstra algorithm
	 * 
	 * @param origin The origin to start routing at
	 * @param measure The measure computer and comparator to use for routing
	 * @param time The time the trip starts at
	 * @param usedModeID The first used mode
	 * @param modes Bitset of usable transport modes
	 * @param ends A set of all destinations
	 * @param boundNumber If >0 then the router will stop after this number of ends has been found
	 * @param boundTT If >0 then the router will stop after this travel time has been reached
	 * @param boundDist If >0 then the router will stop after this distance has been reached
	 * @param boundVar If >0 then the router will stop after the bound variable reaches this limit
	 * @param shortestOnly Whether the router shall end as soon as a sink was seen
	 * @param nearestFromEdges The map of edges destinations are located at to the destinations
	 * @return A results container
	 * @see DijkstraResult
	 */
	public static DijkstraResult run(MapResult origin, AbstractRouteWeightFunction measure, int time, long usedModeID, long modes, 
			Set<DBEdge> ends, int boundNumber, double boundTT, double boundDist, double boundVar, boolean shortestOnly,
			HashMap<DBEdge, Vector<MapResult>> nearestFromEdges) {
		
		HashMap<DBNode, HashMap<Long, DijkstraEntry> > nodeMap = new HashMap<DBNode, HashMap<Long, DijkstraEntry>>();
		boolean hadExtension = false;
		long availableModes = modes;
		Mode usedMode = Modes.getMode(usedModeID);
		
		DijkstraResult ret = new DijkstraResult(origin, boundNumber, boundTT, boundDist, boundVar, shortestOnly, time);
		DBEdge startEdge = origin.edge;
		double tt = startEdge.getTravelTime(usedMode.vmax, time) * (startEdge.getLength()-origin.pos) / startEdge.getLength();
		PriorityQueue<DijkstraEntry> next = new PriorityQueue<DijkstraEntry>(1000, measure);
		DijkstraEntry nm = new DijkstraEntry(measure, null, startEdge.getToNode(), startEdge, availableModes, usedMode,
				(startEdge.getLength()-origin.pos), tt, null, tt, 0, false);
		next.add(nm);
		addNodeInfo(nodeMap, startEdge.getToNode(), availableModes, nm);
		if(ret.visitEdge(measure, startEdge, nm, nearestFromEdges)) {
			if(!hadExtension) {
				boundTT = Math.max(boundTT, tt*2);
				hadExtension = true;
			}
		} 
		// consider starting in the opposite direction
		if(startEdge.getOppositeEdge()!=null && startEdge.getOppositeEdge().allows(usedMode)) {
			DBEdge e = startEdge.getOppositeEdge();
			tt = e.getTravelTime(usedMode.vmax, time) * (origin.pos) / e.getLength();
			nm = new DijkstraEntry(measure, null, e.getToNode(), e, availableModes, usedMode, (origin.pos), tt, null, tt, 0, true);
			next.add(nm);
			addNodeInfo(nodeMap, e.getToNode(), availableModes, nm);
			if(ret.visitEdge(measure, e, nm, nearestFromEdges)) {
				if(!hadExtension) {
					boundTT = Math.max(boundTT, tt*2);
					hadExtension = true;
				}
			}
		}
		
		while (!next.isEmpty()) {
			DijkstraEntry nns = next.poll();
			// check bounds
			if (boundTT > 0 && nns.tt >= boundTT) {
				continue;
			}
			if (boundDist > 0 && nns.distance >= boundDist) {
				continue;
			}
			Vector<DBEdge> oes = nns.n.getOutgoing();
			for (DBEdge oe : oes) {
				availableModes = nns.availableModes;
				usedMode = nns.usedMode;
				if(!oe.allowsAny(availableModes)) { 
					continue;
				}
				double interchangeTT = 0;
				if (!oe.allows(usedMode)) {
					// todo: pt should entrain bikes, foot, etc. be put on top of this
					// (check also computation of the current line in outputs)
					availableModes = availableModes & oe.getModes();
					if(availableModes==0) {
						continue;
					}
					usedMode = Modes.selectModeFrom(availableModes);
					// @todo: what is an inter-mode interchange time? interchangeTT = 0;
				}
				GTFSConnection ptConnection = null;
				double ttt;
				if(oe.isGTFSEdge()) {
					GTFSEdge ge = (GTFSEdge) oe;
					// @todo: this is not correct, the interchange should be regarded here, not in the ttt computation below
					ptConnection = ge.getConnection(time + nns.tt);
					if(ptConnection==null) {
						ttt = 86400;
					} else {
						GTFSTrip prevTrip = nns.line!=null ? nns.line.trip : null;
						if(!ptConnection.trip.equals(prevTrip)) {
							interchangeTT = ((GTFSStop) nns.n).getInterchangeTime(ptConnection.trip, prevTrip, 0);
						}
						ttt = ptConnection.arrivalTime - time - nns.tt + interchangeTT;
					}
				} else {
					ttt = oe.getTravelTime(usedMode.vmax, time + nns.tt) + interchangeTT;
					// @todo: interchange times at nodes
				}
				DBNode n = oe.getToNode();
				double distance = nns.distance + oe.getLength();
				tt = nns.tt + ttt;
				DijkstraEntry oldValue = getPriorNodeInfo(nodeMap, n, availableModes);
				DijkstraEntry newValue = new DijkstraEntry(measure, nns, n, oe, availableModes, usedMode, distance, tt, ptConnection, ttt, interchangeTT, false);
				if(oldValue==null) {
					next.add(newValue);
					addNodeInfo(nodeMap, n, availableModes, newValue);
				} else if(measure.compare(oldValue, newValue)>0) {
					next.remove(oldValue);
					next.add(newValue);
					addNodeInfo(nodeMap, n, availableModes, newValue);
				}
				if(ret.visitEdge(measure, oe, newValue, nearestFromEdges)) {
					if(!hadExtension) {
						boundTT = Math.max(boundTT, tt+newValue.first.ttt+ttt);
						hadExtension = true;
					}
				}
				
				// check opposite direction
				if(oe.getOppositeEdge()!=null && oe.getOppositeEdge().getAttachedObjectsNumber()!=0) {
					DijkstraEntry newOppositeValue = new DijkstraEntry(measure, nns, n, oe.getOppositeEdge(), availableModes, usedMode, distance, tt, ptConnection, ttt, interchangeTT, true);
					if(ret.visitEdge(measure, oe.getOppositeEdge(), newOppositeValue, nearestFromEdges)) {
						if(!hadExtension) {
							boundTT = Math.max(boundTT, tt+newOppositeValue.first.ttt+ttt);
							hadExtension = true;
						}
					}
				}
				
			}
		}
		return ret;
	}
	

	/** @brief Adds the information about the access to a node
	 * 
	 * @param nodeMap The information storage
	 * @param node The node to add the information about
	 * @param availableModes The still available modes
	 * @param m The path to the node
	 */
	public static void addNodeInfo(HashMap<DBNode, HashMap<Long, DijkstraEntry> > nodeMap, DBNode node, long availableModes, DijkstraEntry m) {
		if(!nodeMap.containsKey(node)) {
			nodeMap.put(node, new HashMap<Long, DijkstraEntry>());
		}
		HashMap<Long, DijkstraEntry> nodeVals = nodeMap.get(node);
		nodeVals.put(availableModes, m);
	}
	
	
	/** @brief Returns the information about a previously visited node
	 * @param node The accessed node
	 * @param availableModes The still available modes
	 * @return The prior node used to access the given one using the given modes
	 */
	public static DijkstraEntry getPriorNodeInfo(HashMap<DBNode, HashMap<Long, DijkstraEntry> > nodeMap, DBNode node, long availableModes) {
		if(!nodeMap.containsKey(node)) {
			return null;
		}
		HashMap<Long, DijkstraEntry> nodeVals = nodeMap.get(node);
		if(!nodeVals.containsKey(availableModes)) {
			return null;
		}
		return nodeVals.get(availableModes); 
	}

	
	

}

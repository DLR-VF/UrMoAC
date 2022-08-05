/*
 * Copyright (c) 2016-2022 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * Licensed under the GNU General Public License v3.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraße 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.algorithms.routing;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Vector;

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
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class BoundDijkstra {
	/**
	 * @brief Computes a bound 1-to-many shortest paths using the Dijkstra
	 *        algorithm
	 * 
	 * @param measure The measure computer and comperator to use for routing
	 * @param time The time the trip starts at
	 * @param startEdge The starting road
	 * @param usedModesIDs The first used mode
	 * @param modes Bitset of usable transport modes
	 * @param ends A set of all destinations
	 * @param boundNumber If >0 then the router will stop after this number of ends has been found
	 * @param boundTT If >0 then the router will stop after this travel time has been reached
	 * @param boundDist If >0 then the router will stop after this distance has been reached
	 * @param boundVar If >0 then the router will stop after the bound variable reaches this limit
	 * @param shortestOnly Whether the router shall end as soon as a sink was seen
	 * @return A results container
	 * @see DijkstraResult
	 */
	public static DijkstraResult run(AbstractRouteWeightFunction measure, int time, DBEdge startEdge, long usedModesIDs, long modes, 
			Set<DBEdge> ends, int boundNumber, double boundTT, double boundDist, double boundVar, boolean shortestOnly) {
		
		boolean hadExtension = false;
		long availableModes = modes;
		Mode usedMode = Modes.getMode(usedModesIDs);
		double tt = startEdge.getTravelTime(usedMode.vmax, time);
		DijkstraResult ret = new DijkstraResult(new HashSet<>(ends), boundNumber, boundTT, boundDist, boundVar, shortestOnly, time);
		PriorityQueue<DijkstraEntry> next = new PriorityQueue<DijkstraEntry>(1000, measure);
		DijkstraEntry nm = new DijkstraEntry(measure, null, startEdge.getToNode(), startEdge, availableModes, usedMode,
				startEdge.getLength(), tt, null, tt, 0, false);
		// originally, "startPos" was used - currently the offset of the mappable object is not regarded in the 
		// distance limit computation
		next.add(nm);
		ret.addNodeInfo(startEdge.getToNode(), availableModes, nm);
		if(ret.addEdgeInfo(measure, startEdge, nm)) {
			if(!hadExtension&&!ret.allFound()) {
				boundTT = Math.max(boundTT, tt*2);
				hadExtension = true;
			}
		} 
		
		// consider starting in the opposite direction
		if(startEdge.opposite!=null && startEdge.opposite.allows(usedMode)) {
			DBEdge e = startEdge.opposite;
			tt = e.getTravelTime(usedMode.vmax, time);
			nm = new DijkstraEntry(measure, null, e.getToNode(), e, availableModes, usedMode, e.getLength(), tt, null, tt, 0, true);
			// originally, "startPos" was used - currently the offset of the mappable object
			// is not regarded in the distance limit computation
			next.add(nm);
			ret.addNodeInfo(e.getToNode(), availableModes, nm);
			if(ret.addEdgeInfo(measure, e, nm)) {
				if(!hadExtension&&!ret.allFound()) {
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
					availableModes = availableModes & oe.modes;
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
				DijkstraEntry oldValue = ret.getPriorNodeInfo(n, availableModes);
				DijkstraEntry newValue = new DijkstraEntry(measure, nns, n, oe, availableModes, usedMode, distance, tt, ptConnection, ttt, interchangeTT, false);
				if(oldValue==null) {
					next.add(newValue);
					ret.addNodeInfo(n, availableModes, newValue);
				} else if(measure.compare(oldValue, newValue)>0) {
					next.remove(oldValue);
					next.add(newValue);
					ret.addNodeInfo(n, availableModes, newValue);
				}
				if(ret.addEdgeInfo(measure, oe, newValue)) {
					if(!hadExtension&&!ret.allFound()) {
						boundTT = Math.max(boundTT, tt+newValue.first.ttt+ttt);
						hadExtension = true;
					}
				}
				
				// check opposite direction
				if(oe.opposite!=null && oe.opposite.getAttachedObjectsNumber()!=0) {
					DijkstraEntry newOppositeValue = new DijkstraEntry(measure, nns, n, oe.opposite, availableModes, usedMode, distance, tt, ptConnection, ttt, interchangeTT, true);
					if(ret.addEdgeInfo(measure, oe.opposite, newOppositeValue)) {
						if(!hadExtension&&!ret.allFound()) {
							boundTT = Math.max(boundTT, tt+newOppositeValue.first.ttt+ttt);
							hadExtension = true;
						}
					}
				}
				
			}
		}
		return ret;
	}
	
	

}

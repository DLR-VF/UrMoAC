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

import de.dlr.ivf.urmo.router.gtfs.GTFSEdge;
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
	 * @param usedMode The first used mode
	 * @param modes Bitset of usable transport modes
	 * @param ends A set of all destinations
	 * @param startPos The starting position at the starting road
	 * @param boundNumber If >0 then the router will stop after this number of ends has been found
	 * @param boundTT If >0 then the router will stop after this travel time has been reached
	 * @param boundDist If >0 then the router will stop after this distance has been reached
	 * @param boundVar If >0 then the router will stop after the bound variable reaches this limit
	 * @param shortestOnly Whether the router shall end as soon as a sink was seen
	 * @return A results container
	 * @see DijkstraResult
	 */
	public static DijkstraResult run(AbstractRouteWeightFunction measure, int time, DBEdge startEdge, long _usedMode, long modes, Set<DBEdge> ends, 
			int boundNumber, double boundTT, double boundDist, double boundVar, boolean shortestOnly) {
		
		boolean hadExtension = false;
		long availableModes = modes;
		Mode usedMode = Modes.getMode(_usedMode);
		double tt = startEdge.getTravelTime("", usedMode.vmax, time);
		DijkstraResult ret = new DijkstraResult(new HashSet<>(ends), boundNumber, boundTT, boundDist, boundVar, shortestOnly, time);
		PriorityQueue<DijkstraEntry> next = new PriorityQueue<DijkstraEntry>(1000, measure);
		DijkstraEntry nm = new DijkstraEntry(measure, null, startEdge.getToNode(), startEdge, availableModes, usedMode,
				startEdge.getLength(), tt, "", tt, 0, false);
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
			tt = e.getTravelTime("", usedMode.vmax, time);
			nm = new DijkstraEntry(measure, null, e.getToNode(), e, availableModes, usedMode, e.getLength(), tt, "", tt, 0, true);
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
					interchangeTT = 0;
				}
				String line = oe.isGTFSEdge() ? ((GTFSEdge) oe).route.nameHack : "";
				if(!line.equals(nns.line)) { // slow string comparison
					interchangeTT = nns.n.getInterchangeTime(line, nns.line, 0);
				}
				DBNode n = oe.getToNode();
				double distance = nns.distance + oe.getLength();
				double ttt = oe.getTravelTime("", usedMode.vmax, time + nns.tt) + interchangeTT;
				tt = nns.tt + ttt;
				DijkstraEntry oldValue = ret.getPriorNodeInfo(n, availableModes);
				DijkstraEntry newValue = new DijkstraEntry(measure, nns, n, oe, availableModes, usedMode, distance, tt, line, ttt, interchangeTT, false);
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
					DijkstraEntry newOppositeValue = new DijkstraEntry(measure, nns, n, oe.opposite, availableModes, usedMode, distance, tt, line, ttt, interchangeTT, true);
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

/*
 * Copyright (c) 2017-2024
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
package de.dlr.ivf.urmo.router.output.odext;

import java.util.HashSet;
import java.util.Set;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraEntry;
import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.output.MeasurementGenerator;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class ODExtendedMeasuresGenerator
 * @brief Interprets a path to build an ODSingleExtendedResult
 * @author Daniel Krajzewicz
 */
public class ODExtendedMeasuresGenerator extends MeasurementGenerator<ODSingleExtendedResult> {
	/**
	 * @brief Interprets the path to build an ODSingleExtendedResult
	 * @param beginTime The start time of the path
	 * @param result The processed path between the origin and the destination
	 * @return An ODSingleExtendedResult computed using the given path
	 */
	public ODSingleExtendedResult buildResult(int beginTime, SingleODResult result) {
		DijkstraEntry toEdgeEntry = result.path;//.getPath(to);//dr.getEdgeInfo(to.edge);
		ODSingleExtendedResult e = new ODSingleExtendedResult(result);
		MapResult from = result.origin;
		MapResult to = result.destination;
		e.weightedDistance = e.dist * e.val;
		e.weightedTravelTime = e.tt * e.val;
		e.weightedValue = ((LayerObject) result.destination.em).getAttachedValue() * e.val;
		e.weightedWaitingTime = 0;
		e.weightedInitialWaitingTime = 0;
		e.weightedPTTravelTime = 0;
		e.weightedInterchangeTime = 0;
		e.connectionsWeightSum = e.val;

		HashSet<String> trips = new HashSet<>();
		double factor = 1.;
		boolean single = false;
		if(from.edge==to.edge) {
			if(from.pos>to.pos) {
				factor = (from.pos - to.pos) / from.edge.getLength();
			} else {
				factor = (to.pos - from.pos) / from.edge.getLength();				
			}
			single = true;
		} else if(from.edge.getOppositeEdge()==to.edge) {
			if(from.pos>(from.edge.getLength() - to.pos)) {
				factor = (from.pos - (from.edge.getLength() - to.pos)) / from.edge.getLength();
			} else {
				factor = ((from.edge.getLength() - to.pos) - from.pos) / from.edge.getLength();				
			}
			single = true;
		} else {
			if(toEdgeEntry.wasOpposite) {
				factor = (to.edge.getLength() - to.pos) / to.edge.getLength();
			} else {
				factor = to.pos / to.edge.getLength();
			}
		}		
		
		DijkstraEntry current = toEdgeEntry;
		String lastPT = null; 
		Set<String> seenLines = new HashSet<>();
		// we go backwards through the list
		do {
			double ttt = current.e.getTravelTime(current.usedMode.vmax, current.tt+beginTime);//.prev==null ? current.tt : current.tt - current.prev.tt;
			if(current.prev==null&&!single) {
				// compute offset to edge's begin / end if it's the first edge
				if(current.e==from.edge.getOppositeEdge()) {
					factor = from.pos / from.edge.getLength();
				} else {
					factor = 1. - from.pos / from.edge.getLength();
				}
			}
			//
			DBEdge edge = current.e;
			e.weightedKCal += edge.getKKC(current.usedMode, ttt) * factor;
			e.weightedPrice += edge.getPrice(current.usedMode, seenLines) * factor;
			e.weightedCO2 += edge.getCO2(current.usedMode) * factor;
			if(current.ptConnection==null) {
				if(lastPT==null) {
					e.weightedEgress += ttt * factor;
				}
				e.weightedAccess += ttt * factor;
			} else {
				if(lastPT!=null&&!current.ptConnection.trip.tripID.equals(lastPT)) {
					e.weightedInterchanges += 1.;
					e.weightedInterchangeTime += e.weightedAccess;
				}
				e.weightedInitialWaitingTime = 0;
				e.weightedInterchangeTime += current.interchangeTT;
				e.weightedAccess = 0;
				e.weightedPTTravelTime += current.ttt;
				if(current.prev==null || current.prev.ptConnection==null || !current.ptConnection.trip.equals(current.prev.ptConnection.trip)) {
					double waitingTime = current.ptConnection.getWaitingTime(beginTime + current.prev.tt);
					e.weightedWaitingTime += waitingTime;
					e.weightedInitialWaitingTime = waitingTime;
					e.weightedPTTravelTime -= waitingTime;
				}
			}
			if(current.ptConnection!=null) {
				lastPT = current.ptConnection.trip.tripID;
				e.lines.add(current.ptConnection.trip.route.id);
				trips.add(current.ptConnection.trip.tripID);
			} else {
				e.lines.add(current.usedMode.mml);
			}
			current = current.prev;
			factor = 1.; // reset factor for normal (not first / last) edges
		} while(current!=null);
		if(trips.size()<1) {
			e.weightedAccess = 0;
			e.weightedEgress = 0;
		}
		
		e.weightedKCal *= e.val;
		e.weightedPrice *= e.val; 
		e.weightedCO2 *= e.val;
		e.weightedInterchanges *= e.val;
		e.weightedWaitingTime *= e.val;
		e.weightedPTTravelTime *= e.val;
		return e;
	}	
	
	
	/**
	 * @brief Builds an empty entry of type ODSingleExtendedResult
	 * @param srcID The id of the origin the path started at
	 * @param destID The id of the destination accessed by this path
	 * @return An empty entry type ODSingleExtendedResult
	 */
	public ODSingleExtendedResult buildEmptyEntry(long srcID, long destID) {
		return new ODSingleExtendedResult(srcID, destID);
	}

	
}

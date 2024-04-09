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
package de.dlr.ivf.urmo.router.output.ptod;

import java.util.HashSet;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraEntry;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.output.MeasurementGenerator;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class PTODMeasuresGenerator
 * @brief Interprets a path to build an PTODSingleResult
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class PTODMeasuresGenerator extends MeasurementGenerator<PTODSingleResult> {
	/**
	 * @brief Interprets the path to build an PTODSingleResult
	 * @param beginTime The start time of the path
	 * @param from The origin the path started at
	 * @param to The destination accessed by this path
	 * @param dr The routing result
	 * @return An PTODSingleResult computed using the given path
	 */
	public PTODSingleResult buildResult(int beginTime, MapResult from, MapResult to, DijkstraResult dr) {
		PTODSingleResult e = new PTODSingleResult(from.em.getOuterID(), to.em.getOuterID(), from, to, dr);
		int step = 0;
		DijkstraEntry current = dr.getEdgeInfo(to.edge);
		double tt = 0;
		double dist = 0;
		HashSet<String> trips = new HashSet<>();
		if(current.line!=null) {
			e.lines.add(current.line.trip.route.id);
			trips.add(current.line.trip.tripID);
		} else {
			e.lines.add(current.usedMode.mml);
		}
		if(from.edge==to.edge) {
			tt = current.first.ttt;
			if(from.pos>to.pos) {
				dist = from.pos - to.pos;
			} else {
				dist = to.pos - from.pos;				
			}
			tt = tt / to.edge.length * dist;
			step = 4;
		} else if(from.edge.opposite==to.edge) {
			tt = current.first.ttt;
			if(from.pos>(to.edge.length - to.pos)) {
				dist = from.pos - (to.edge.length - to.pos);
			} else {
				dist = (to.edge.length - to.pos) - from.pos;				
			}
			tt = tt / to.edge.length * dist;
			step = 4;
		} else {
			if(current.wasOpposite) {
				dist -= to.pos;
				tt -= current.ttt * to.pos / to.edge.length;
			} else {
				dist -= (to.edge.length - to.pos);
				tt -= (current.ttt - current.ttt * (to.pos / to.edge.length));
			}
			step = 0;
			do {
				e.weightedInterchangeTravelTime += current.interchangeTT;
				dist += current.e.length;
				tt += current.ttt;
				DijkstraEntry prev = current.prev;
				if(prev==null) {
					current = prev;
					continue;
				}
				if(current.line!=null) {
					e.lines.add(current.line.trip.route.id);
					trips.add(current.line.trip.tripID);
				} else {
					e.lines.add(current.usedMode.mml);
				}
				if( (prev.line==null&&current.line==null) || (prev.line!=null && current.line!=null && prev.line.trip.equals(current.line.trip)) ) {
					current = prev;
					continue;
				}
				addStep(e, step, dist, tt);
				tt = 0;
				dist = 0;
				if(prev.line==null) {
					// foot->pt; 
					step = 2; // either interchange or access 
					double waitingTime = current.line.getWaitingTime(beginTime + prev.tt);
					e.weightedWaitingTime += waitingTime;
					e.weightedInitialWaitingTime = waitingTime;
				} else {
					// pt->foot|pt
					if(current.line!=null) {
						// change from pt to pt
						double waitingTime = current.line.getWaitingTime(beginTime + prev.tt);
						e.weightedWaitingTime += waitingTime;
						e.weightedInitialWaitingTime = waitingTime;
					}
					step = 1;
				}
				current = prev;
			} while(current!=null);
			current = dr.getEdgeInfo(to.edge);
			double firstTT = current.first.ttt;
			if(current.first.wasOpposite) {
				dist -= (from.edge.length - from.pos);
				tt -= (firstTT - firstTT * (from.pos / from.edge.length));
			} else {
				dist -= from.pos;
				tt -= (firstTT * (from.pos / from.edge.length));
			}
		}
		if(step!=4 && dist>0 && tt>0) {
			if(step==2) {
				step = 3;
			}
			addStep(e, step, dist, tt);
		}
		// no access / egress when no pt
		if(trips.size()<1) {
			e.weightedAccessDistance = 0;
			e.weightedAccessTravelTime = 0;
			e.weightedEgressDistance = 0;
			e.weightedEgressTravelTime = 0;
		}
		// apply length
		e.weightedInterchangesNum = Math.max(0, (double) trips.size() - 1.);
		e.weightedDistance = e.dist * e.val;
		e.weightedTravelTime = e.tt * e.val;
		e.weightedAccessDistance *= e.val;
		e.weightedAccessTravelTime *= e.val;
		e.weightedEgressDistance *= e.val;
		e.weightedEgressTravelTime *= e.val;
		e.weightedPTDistance *= e.val;
		e.weightedPTTravelTime = (e.weightedPTTravelTime-e.weightedWaitingTime) * e.val;
		e.weightedInterchangeDistance *= e.val;
		e.weightedInterchangeTravelTime *= e.val;
		e.weightedInterchangesNum *= e.val;
		e.weightedWaitingTime *= e.val;
		e.weightedInitialWaitingTime *= e.val;
		e.weightedValue = ((LayerObject) to.em).getAttachedValue() * e.val;
		e.connectionsWeightSum = e.val;
		return e;
	}
	
	
	/**
	 * @brief Builds an empty entry of type ODSingleStatsResult
	 * @param srcID The id of the origin the path started at
	 * @param destID The id of the destination accessed by this path
	 * @return An empty entry type ODSingleStatsResult
	 */
	public PTODSingleResult buildEmptyEntry(long srcID, long destID) {
		return new PTODSingleResult(srcID, destID);
	}

	
	/**
	 * @brief Adds the information about the travel time / distance to the proper field
	 * @param e The result to add the information to
	 * @param step The current trip step (egress, pt, interchange, access)
	 * @param dist The distance collected
	 * @param tt The travel time collected
	 */
	private void addStep(PTODSingleResult e, int step, double dist, double tt) {
		switch(step) {
		case 0: // egress
			e.weightedEgressDistance = dist;
			e.weightedEgressTravelTime = tt;
			break;
		case 1: // pt
			e.weightedPTDistance += dist;
			e.weightedPTTravelTime += tt;
			break;
		case 2: // interchange
			e.weightedInterchangeDistance += dist;
			e.weightedInterchangeTravelTime += tt;
			break;
		case 3: // access
			e.weightedAccessDistance = dist;
			e.weightedAccessTravelTime = tt;
			break;
		case 4: // single edge
			break;
		}
	}
	
}

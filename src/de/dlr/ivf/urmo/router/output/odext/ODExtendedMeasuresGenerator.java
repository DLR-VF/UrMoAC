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
package de.dlr.ivf.urmo.router.output.odext;

import java.util.HashSet;
import java.util.Set;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraEntry;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.gtfs.GTFSEdge;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.output.MeasurementGenerator;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class ODExtendedMeasuresGenerator
 * @brief Interprets a path to build an ODSingleExtendedResult
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 * @param <T>
 */
public class ODExtendedMeasuresGenerator extends MeasurementGenerator<ODSingleExtendedResult> {
	/**
	 * @brief Interprets the path to build an ODSingleExtendedResult
	 * @param beginTime The start time of the path
	 * @param from The origin the path started at
	 * @param to The destination accessed by this path
	 * @param dr The routing result
	 * @return An ODSingleExtendedResult computed using the given path
	 */
	public ODSingleExtendedResult buildResult(int beginTime, MapResult from, MapResult to, DijkstraResult dr) {
		DijkstraEntry toEdgeEntry = dr.getEdgeInfo(to.edge);
		ODSingleExtendedResult e = new ODSingleExtendedResult(from.em.getOuterID(), to.em.getOuterID(), from, to, dr);
		e.weightedDistance = e.dist * e.val;
		e.weightedTravelTime = e.tt * e.val;
		e.weightedSpeed = e.tt!=0 ? (e.dist/e.tt) * e.val : -1; /// TODO: document
		e.weightedValue = ((LayerObject) to.em).getAttachedValue() * e.val;
		e.weightedWaitingTime = 0;
		e.weightedInitialWaitingTime = 0;
		e.weightedPTTravelTime = 0;
		e.weightedInterchangeTime = 0;
		e.connectionsWeightSum = e.val;

		double factor = 1.;
		boolean single = false;
		if(from.edge==to.edge) {
			if(from.pos>to.pos) {
				factor = (from.pos - to.pos) / from.edge.length;
			} else {
				factor = (to.pos - from.pos) / from.edge.length;				
			}
			single = true;
		} else if(from.edge.opposite==to.edge) {
			if(from.pos>(from.edge.length - to.pos)) {
				factor = (from.pos - (from.edge.length - to.pos)) / from.edge.length;
			} else {
				factor = ((from.edge.length - to.pos) - from.pos) / from.edge.length;				
			}
			single = true;
		} else {
			if(toEdgeEntry.wasOpposite) {
				factor = (to.edge.length - to.pos) / to.edge.length;
			} else {
				factor = to.pos / to.edge.length;
			}
		}		
		
		DijkstraEntry current = toEdgeEntry;
		String lastPT = null; 
		Set<String> seenLines = new HashSet<>();
		do {
			double ttt = current.prev==null ? current.tt : current.tt - current.prev.tt;
			if(current.prev==null&&!single) {
				if(current.e==from.edge.opposite) {
					factor = from.pos / from.edge.length;
				} else {
					factor = 1. - from.pos / from.edge.length;
				}
			}
			DBEdge edge = current.e;
			if(lastPT!=null&&current.line.length()!=0&&!lastPT.equals(current.line)) {
				e.weightedInterchanges += 1.;
			}
			e.weightedKCal += edge.getKKC(current.usedMode, ttt) * factor;
			e.weightedPrice += edge.getPrice(current.usedMode, seenLines) * factor;
			e.weightedCO2 += edge.getCO2(current.usedMode) * factor;
			if(current.line.length()==0) {
				if(lastPT==null) {
					e.weightedEgress += ttt * factor;
				}
				e.weightedAccess += ttt * factor;
			} else {
				e.weightedAccess = 0;
				e.weightedInitialWaitingTime = 0;
				e.weightedInterchangeTime += current.interchangeTT;
				e.weightedPTTravelTime += ((GTFSEdge) edge).getTravelTime(current.line, 1000, beginTime + current.prev.tt) - ((GTFSEdge) edge).getWaitingTime(beginTime + current.prev.tt);
				if( (current.prev==null) || !current.prev.line.equals(current.line)) {
					e.weightedWaitingTime += ((GTFSEdge) edge).getWaitingTime(beginTime + current.prev.tt) * factor;
					e.weightedInitialWaitingTime = ((GTFSEdge) edge).getWaitingTime(beginTime + current.prev.tt) * factor;
				}
			}
			factor = 1.;
			if(current.line.length()!=0) {
				lastPT = current.line;
				e.lines.add(current.line);
			} else {
				e.lines.add(current.usedMode.mml);
			}
			current = current.prev;
		} while(current!=null);
		// TODO: recheck
		if(e.lines.size()<2) {
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

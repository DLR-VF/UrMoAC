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
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.output.odstats;

import java.util.HashSet;
import java.util.Set;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraEntry;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.output.MeasurementGenerator;
import de.dlr.ivf.urmo.router.shapes.DBEdge;

/**
 * @class ODStatsMeasuresGenerator
 * @brief Interprets a path to build an ODSingleStatsResult
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 * @param <T>
 */
public class ODStatsMeasuresGenerator extends MeasurementGenerator<ODSingleStatsResult> {
	/**
	 * @brief Interprets the path to build an ODSingleStatsResult
	 * @param beginTime The start time of the path
	 * @param from The origin the path started at
	 * @param to The destination accessed by this path
	 * @param dr The routing result
	 * @return An ODSingleStatsResult computed using the given path
	 */
	public ODSingleStatsResult buildResult(int beginTime, MapResult from, MapResult to, DijkstraResult dr) {
		DijkstraEntry toEdgeEntry = dr.getEdgeInfo(to.edge);
		ODSingleStatsResult e = new ODSingleStatsResult(from.em.getOuterID(), to.em.getOuterID(), from, to, dr);
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
		
		double kCal = 0;
		double price = 0;
		double CO2 = 0;
		Set<String> seenLines = new HashSet<>();
		DijkstraEntry current = toEdgeEntry;
		do {
			double ttt = current.prev==null ? current.tt : current.tt - current.prev.tt;
			if(current.prev==null&&!single) {
				factor = 1. - from.pos / from.edge.length;
			}
			DBEdge edge = current.e;
			kCal += edge.getKKC(current.usedMode, ttt) * factor;
			price += edge.getPrice(current.usedMode, seenLines) * factor;
			CO2 += edge.getCO2(current.usedMode) * factor;
			factor = 1.;
			current = current.prev;
		} while(current!=null);
		
		e.addSingle(e.dist, e.tt, e.val, kCal, price, CO2);
		return e;
	}	
	
	
	/**
	 * @brief Builds an empty entry of type ODSingleStatsResult
	 * @param srcID The id of the origin the path started at
	 * @param destID The id of the destination accessed by this path
	 * @return An empty entry type ODSingleStatsResult
	 */
	public ODSingleStatsResult buildEmptyEntry(long srcID, long destID) {
		return new ODSingleStatsResult(srcID, destID);
	}

	
}

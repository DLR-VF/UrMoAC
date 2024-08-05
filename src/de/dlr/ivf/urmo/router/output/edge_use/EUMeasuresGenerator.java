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
package de.dlr.ivf.urmo.router.output.edge_use;

import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraEntry;
import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.output.MeasurementGenerator;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class EUMeasuresGenerator
 * @brief Interprets a path to build an EUSingleResult
 * @author Daniel Krajzewicz
 */
public class EUMeasuresGenerator extends MeasurementGenerator<EUSingleResult> {
	/**
	 * @brief Interprets the path to build an EUSingleResult
	 * @param beginTime The start time of the path
	 * @param result The processed path between the origin and the destination
	 * @return An EUSingleResult computed using the given path
	 */
	public EUSingleResult buildResult(int beginTime, SingleODResult result) {
		DijkstraEntry current = result.path;
		EUSingleResult e = new EUSingleResult(result);
		double value = ((LayerObject) result.destination.em).getAttachedValue() * e.val;
		do {
			DijkstraEntry next = current;
			e.addSingle(next.e, value, e.val);
			current = current.prev;
		} while(current!=null);
		return e;
	}	
	
	
	/**
	 * @brief Builds an empty entry of type EUSingleResult
	 * @param originID The id of the origin the path started at
	 * @param destID The id of the destination accessed by this path
	 * @return An empty entry type EUSingleResult
	 */
	public EUSingleResult buildEmptyEntry(long originID, long destID) {
		return new EUSingleResult(originID, destID);
	}

	
}

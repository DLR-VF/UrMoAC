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
package de.dlr.ivf.urmo.router.output.od;

import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.output.MeasurementGenerator;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class ODMeasuresGenerator
 * @brief Interprets a path to build an ODSingleResult
 * @author Daniel Krajzewicz
 */
public class ODMeasuresGenerator extends MeasurementGenerator<ODSingleResult> {
	/**
	 * @brief Interprets the path to build an ODSingleResult
	 * @param beginTime The start time of the path
	 * @param result The processed path between the origin and the destination
	 * @return An ODSingleResult computed using the given path
	 */
	public ODSingleResult buildResult(int beginTime, SingleODResult result) {
		ODSingleResult e = new ODSingleResult(result);
		e.weightedDistance = e.dist * e.val;
		e.weightedTravelTime = e.tt * e.val;
		e.weightedValue = ((LayerObject) result.destination.em).getAttachedValue() * e.val;
		e.connectionsWeightSum = e.val;
		return e;
	}
	
	
	/**
	 * @brief Builds an empty entry of type ODSingleResult
	 * @param srcID The id of the origin the path started at
	 * @param destID The id of the destination accessed by this path
	 * @return An empty entry type ODSingleResult
	 */
	public ODSingleResult buildEmptyEntry(long srcID, long destID) {
		return new ODSingleResult(srcID, destID);
	}

	
}

/*
 * Copyright (c) 2017-2025
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
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;

/**
 * @class ODSingleResult
 * @brief A simple origin-destination result containing the travel time and the distance
 * @author Daniel Krajzewicz
 */
public class ODSingleResult extends AbstractSingleResult {
	/// @brief The weighted distance
	public double weightedDistance = 0;
	/// @brief The weighted travel times
	public double weightedTravelTime = 0;
	/// @brief The weighted value
	public double weightedValue = 0;
	/// @brief The sum of the connection weights
	public double connectionsWeightSum = 0;
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Generates an empty entry.
	 * @param originID The id of the origin the represented trip starts at
	 * @param destID The id of the destination the represented trip ends at
	 */
	public ODSingleResult(long originID, long destID) {
		super(originID, destID);
	}
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Computes the distance and the travel time
	 * @param result The processed path between the origin and the destination
	 */
	public ODSingleResult(SingleODResult result) {
		super(result);
	}
	
	
	/**
	 * @brief Adds the measures from the given result
	 * @param asr The result to add
	 */
	@Override
	public void addCounting(AbstractSingleResult asr) {
		ODSingleResult srnm = (ODSingleResult) asr;
		weightedDistance += srnm.weightedDistance;
		weightedTravelTime += srnm.weightedTravelTime;
		weightedValue += srnm.weightedValue;
		connectionsWeightSum += srnm.connectionsWeightSum;
	}
	
	
	/**
	 * @brief Norms the computed measures
	 * @param numOrigins The number of origins
	 * @param originsWeight The sum of the origins' weights
	 * @return The normed result
	 */
	@Override
	public AbstractSingleResult getNormed(int numOrigins, double originsWeight) {
		ODSingleResult srnm = new ODSingleResult(originID, destID);
		srnm.weightedDistance = connectionsWeightSum!=0 ? weightedDistance / connectionsWeightSum : 0;
		srnm.weightedTravelTime = connectionsWeightSum!=0 ? weightedTravelTime / connectionsWeightSum : 0;
		srnm.connectionsWeightSum = originsWeight!=0 ? connectionsWeightSum / originsWeight : 0;
		srnm.weightedValue = originsWeight!=0 ? weightedValue / originsWeight : 0;
		return srnm;
	}
	
	
}

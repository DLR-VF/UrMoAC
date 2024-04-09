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
package de.dlr.ivf.urmo.router.output.od;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;

/**
 * @class ODSingleResult
 * @brief A simple origin-destination result containing the travel time and the distance
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
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
	 * @param srcID The id of the origin the represented trip starts at
	 * @param destID The id of the destination the represented trip ends at
	 */
	public ODSingleResult(long srcID, long destID) {
		super(srcID, destID);
	}
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Computes the distance and the travel time
	 * @param srcID The id of the origin the represented trip starts at
	 * @param destID The id of the destination the represented trip ends at
	 * @param from The mapped source
	 * @param to The mapped destination
	 * @param dr The path between the source and the destination
	 */
	public ODSingleResult(long srcID, long destID, MapResult from, MapResult to, DijkstraResult dr) {
		super(srcID, destID, from, to, dr);
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
	 * @param numSources The number of sources
	 * @param sourcesWeight The sum of the sources' weights
	 * @return The normed result
	 */
	@Override
	public AbstractSingleResult getNormed(int numSources, double sourcesWeight) {
		ODSingleResult srnm = new ODSingleResult(srcID, destID);
		srnm.weightedDistance = connectionsWeightSum!=0 ? weightedDistance / connectionsWeightSum : 0;
		srnm.weightedTravelTime = connectionsWeightSum!=0 ? weightedTravelTime / connectionsWeightSum : 0;
		srnm.connectionsWeightSum = sourcesWeight!=0 ? connectionsWeightSum / sourcesWeight : 0;
		srnm.weightedValue = sourcesWeight!=0 ? weightedValue / sourcesWeight : 0;
		return srnm;
	}
	
	
}

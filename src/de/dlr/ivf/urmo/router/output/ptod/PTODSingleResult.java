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
package de.dlr.ivf.urmo.router.output.ptod;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;

/**
 * @class PTODSingleResult
 * @brief A public transport - oriented result
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class PTODSingleResult extends AbstractSingleResult {
	/// @brief The weighted access distance
	public double weightedAccessDistance = 0;
	/// @brief The weighted access travel time
	public double weightedAccessTravelTime = 0;
	/// @brief The weighted egress distance
	public double weightedEgressDistance = 0;
	/// @brief The weighted egress travel time
	public double weightedEgressTravelTime = 0;
	/// @brief The weighted sum of interchange distances
	public double weightedInterchangeDistance = 0;
	/// @brief The weighted sum of interchange travel times
	public double weightedInterchangeTravelTime = 0;
	/// @brief The weighted distance spent in public transport
	public double weightedPTDistance = 0;
	/// @brief The weighted travel time spent in public transport
	public double weightedPTTravelTime = 0;
	/// @brief The weighted waiting time
	public double weightedWaitingTime = 0;
	/// @brief The weighted initial waiting time
	public double weightedInitialWaitingTime = 0;
	/// @brief The weighted interchanges number
	public double weightedInterchangesNum = 0;
	/// @brief The weighted value
	public double weightedValue = 0;
	/// @brief The sum of connection weights
	public double connectionsWeightSum = 0;
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Generates an empty entry.
	 * @param _srcID The id of the origin the represented trip starts at
	 * @param _destID The id of the destination the represented trip ends at
	 */
	public PTODSingleResult(long srcID, long destID) {
		super(srcID, destID);
	}
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Computes the distance and the travel time
	 * @param _srcID The id of the origin the represented trip starts at
	 * @param _destID The id of the destination the represented trip ends at
	 */
	public PTODSingleResult(long srcID, long destID, MapResult from, MapResult to, DijkstraResult dr) {
		super(srcID, destID, from, to, dr);
	}

	
	/**
	 * @brief Adds the measures from the given result
	 * @param asr The result to add
	 */
	@Override
	public void addCounting(AbstractSingleResult asr) {
		PTODSingleResult srnm = (PTODSingleResult) asr;
		weightedAccessDistance += srnm.weightedAccessDistance;
		weightedAccessTravelTime += srnm.weightedAccessTravelTime;
		weightedEgressDistance += srnm.weightedEgressDistance;
		weightedEgressTravelTime += srnm.weightedEgressTravelTime;
		weightedInterchangeDistance += srnm.weightedInterchangeDistance;
		weightedInterchangeTravelTime += srnm.weightedInterchangeTravelTime;
		weightedPTDistance += srnm.weightedPTDistance;
		weightedPTTravelTime += srnm.weightedPTTravelTime;
		weightedInterchangesNum += srnm.weightedInterchangesNum;
		weightedWaitingTime += srnm.weightedWaitingTime;
		weightedInitialWaitingTime += srnm.weightedInitialWaitingTime;
		weightedValue += srnm.weightedValue;
		connectionsWeightSum += srnm.connectionsWeightSum;
	}


	/**
	 * @brief Norms the computed measures
	 * @param numSources The number of sources
	 * @param sourcesWeight The sum of the sources' weights
	 */
	@Override
	public AbstractSingleResult getNormed(int numSources, double sourcesWeight) {
		PTODSingleResult srnm = new PTODSingleResult(srcID, destID);
		srnm.weightedAccessDistance = connectionsWeightSum!=0 ? weightedAccessDistance / connectionsWeightSum : 0;
		srnm.weightedAccessTravelTime = connectionsWeightSum!=0 ? weightedAccessTravelTime / connectionsWeightSum : 0;
		srnm.weightedEgressDistance = connectionsWeightSum!=0 ? weightedEgressDistance / connectionsWeightSum : 0;
		srnm.weightedEgressTravelTime = connectionsWeightSum!=0 ? weightedEgressTravelTime / connectionsWeightSum : 0;
		srnm.weightedInterchangeDistance = connectionsWeightSum!=0 ? weightedInterchangeDistance / connectionsWeightSum : 0;
		srnm.weightedInterchangeTravelTime = connectionsWeightSum!=0 ? weightedInterchangeTravelTime / connectionsWeightSum : 0;
		srnm.weightedPTDistance = connectionsWeightSum!=0 ? weightedPTDistance / connectionsWeightSum : 0;
		srnm.weightedPTTravelTime = connectionsWeightSum!=0 ? weightedPTTravelTime / connectionsWeightSum : 0;
		srnm.weightedInterchangesNum = connectionsWeightSum!=0 ? weightedInterchangesNum / connectionsWeightSum : 0;
		srnm.weightedWaitingTime = connectionsWeightSum!=0 ? weightedWaitingTime / connectionsWeightSum : 0;
		srnm.weightedInitialWaitingTime = connectionsWeightSum!=0 ? weightedInitialWaitingTime / connectionsWeightSum : 0;
		srnm.connectionsWeightSum = sourcesWeight!=0 ? connectionsWeightSum / sourcesWeight : 0;
		srnm.weightedValue = sourcesWeight!=0 ? weightedValue / sourcesWeight : 0;
		return srnm;
	}
	
	
}

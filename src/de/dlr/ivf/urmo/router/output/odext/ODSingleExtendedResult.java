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
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;

/**
 * @class ODSingleExtendedResult
 * @brief A more complex origin-destination result containing different measures
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class ODSingleExtendedResult extends AbstractSingleResult {
	/// @brief The weighted distance
	public double weightedDistance = 0;
	/// @brief The weighted travel time
	public double weightedTravelTime = 0;
	/// @brief The weighted speed
	public double weightedSpeed = 0;
	/// @brief The weighted value
	public double weightedValue = 0;
	/// @brief The weighted number of consumed kilocalories
	public double weightedKCal = 0;
	/// @brief The weighted price
	public double weightedPrice = 0;
	/// @brief The weighted amount of emitted CO2
	public double weightedCO2 = 0;
	/// @brief The sum of the connection weights
	public double connectionsWeightSum = 0;
	/// @brief The weighted number of interchanges
	public double weightedInterchanges = 0;
	/// @brief The weighted access
	public double weightedAccess = 0; // @todo reckeck: note that this is as well included in the travel time
	/// @brief The weighted egress
	public double weightedEgress = 0; // @todo reckeck: note that this is as well included in the travel time
	/// @brief The weighted waiting time
	public double weightedWaitingTime = 0;
	/// @brief The weighted initial weighting time
	public double weightedInitialWaitingTime = 0;
	/// @brief Thewe weighted travel time in a public transport carrier
	public double weightedPTTravelTime = 0;
	/// @brief The weighted interchanging time
	public double weightedInterchangeTime = 0;
	/// @brief The used lines
	public Set<String> lines = new HashSet<>();
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Generates an empty entry.
	 * @param _srcID The id of the origin the represented trip starts at
	 * @param _destID The id of the destination the represented trip ends at
	 */
	public ODSingleExtendedResult(long srcID, long destID) {
		super(srcID, destID);
	}
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Computes the distance and the travel time
	 * @param _srcID The id of the origin the represented trip starts at
	 * @param _destID The id of the destination the represented trip ends at
	 */
	public ODSingleExtendedResult(long srcID, long destID, MapResult from, MapResult to, DijkstraResult dr) {
		super(srcID, destID, from, to, dr);
	}

	
	/**
	 * @brief Adds the measures from the given result
	 * @param asr The result to add
	 */
	@Override
	public void addCounting(AbstractSingleResult asr) {
		ODSingleExtendedResult srnm = (ODSingleExtendedResult) asr;
		weightedDistance += srnm.weightedDistance;
		weightedTravelTime += srnm.weightedTravelTime;
		weightedSpeed += srnm.weightedSpeed;
		weightedValue += srnm.weightedValue;
		weightedKCal += srnm.weightedKCal;
		weightedPrice += srnm.weightedPrice;
		weightedCO2 += srnm.weightedCO2;
		weightedInterchanges += srnm.weightedInterchanges;
		weightedAccess += srnm.weightedAccess;
		weightedEgress += srnm.weightedEgress;
		weightedWaitingTime += srnm.weightedWaitingTime;
		weightedInitialWaitingTime += srnm.weightedInitialWaitingTime;
		weightedPTTravelTime += srnm.weightedPTTravelTime;
		weightedInterchangeTime += srnm.weightedInterchangeTime;
		connectionsWeightSum += srnm.connectionsWeightSum;
		lines.addAll(srnm.lines);
	}


	/**
	 * @brief Norms the computed measures
	 * @param numSources The number of sources
	 * @param sourcesWeight The sum of the sources' weights
	 */
	@Override
	public AbstractSingleResult getNormed(int numSources, double sourcesWeight) {
		ODSingleExtendedResult srnm = new ODSingleExtendedResult(srcID, destID);
		srnm.weightedDistance = connectionsWeightSum!=0 ? weightedDistance / connectionsWeightSum : 0;
		srnm.weightedTravelTime = connectionsWeightSum!=0 ? weightedTravelTime / connectionsWeightSum : 0;
		srnm.weightedSpeed = connectionsWeightSum!=0 ? weightedSpeed / connectionsWeightSum : 0;
		srnm.weightedKCal = connectionsWeightSum!=0 ? weightedKCal / connectionsWeightSum : 0;
		srnm.weightedPrice = connectionsWeightSum!=0 ? weightedPrice / connectionsWeightSum : 0;
		srnm.weightedCO2 = connectionsWeightSum!=0 ? weightedCO2 / connectionsWeightSum : 0;
		srnm.weightedInterchanges = connectionsWeightSum!=0 ? weightedInterchanges / connectionsWeightSum : 0;
		srnm.weightedAccess = connectionsWeightSum!=0 ? weightedAccess / connectionsWeightSum : 0;
		srnm.weightedEgress = connectionsWeightSum!=0 ? weightedEgress / connectionsWeightSum : 0;
		srnm.weightedWaitingTime = connectionsWeightSum!=0 ? weightedWaitingTime / connectionsWeightSum : 0;
		srnm.weightedInitialWaitingTime = connectionsWeightSum!=0 ? weightedInitialWaitingTime / connectionsWeightSum : 0;
		srnm.weightedPTTravelTime = connectionsWeightSum!=0 ? weightedPTTravelTime / connectionsWeightSum : 0;
		srnm.weightedInterchangeTime = connectionsWeightSum!=0 ? weightedInterchangeTime / connectionsWeightSum : 0;
		srnm.connectionsWeightSum = sourcesWeight!=0 ? connectionsWeightSum / sourcesWeight : 0;
		srnm.weightedValue = sourcesWeight!=0 ? weightedValue / sourcesWeight : 0;
		srnm.lines.addAll(lines);
		return srnm;
	}

	public static String asCSV(ODSingleExtendedResult result){

		return result.srcID + ";" + result.destID + ";"
				+ result.weightedDistance + ";" + result.weightedTravelTime + ";" + result.weightedSpeed + ";"
				+ result.connectionsWeightSum + ";" + result.weightedValue + ";"
				+ result.weightedKCal + ";" + result.weightedPrice + ";" + result.weightedCO2 + ";"
				+ result.weightedInterchanges + ";" + result.weightedAccess + ";" + result.weightedEgress + ";"
				+ result.weightedWaitingTime + ";" + result.weightedInitialWaitingTime + ";"
				+ result.weightedPTTravelTime + ";" + result.weightedInterchangeTime + ";" + result.lines.toString() + "\n";
	}
}

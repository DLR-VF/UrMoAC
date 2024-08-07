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
package de.dlr.ivf.urmo.router.output.odstats;

import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;

/**
 * @class ODSingleStatsResult
 * @brief A origin-destination result showing percentiles 
 * @author Daniel Krajzewicz
 */
public class ODSingleStatsResult extends AbstractSingleResult {
	/// @brief A vector of all collected distances
	protected Vector<Double> allDistances = new Vector<>();
	/// @brief A vector of all collected travel times
	protected Vector<Double> allTravelTimes = new Vector<>();
	/// @brief A vector of all collected values
	protected Vector<Double> allValues = new Vector<>();
	/// @brief A vector of all collected kilocalories
	protected Vector<Double> allKCals = new Vector<>();
	/// @brief A vector of all collected prices
	protected Vector<Double> allPrices = new Vector<>();
	/// @brief A vector of all collected CO2
	protected Vector<Double> allCO2s = new Vector<>();
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Generates an empty entry.
	 * @param originID The id of the origin the represented trip starts at
	 * @param destID The id of the destination the represented trip ends at
	 */
	public ODSingleStatsResult(long originID, long destID) {
		super(originID, destID);
	}
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Computes the distance and the travel time
	 * @param result The processed path between the origin and the destination
	 */
	public ODSingleStatsResult(SingleODResult result) {
		super(result);
	}

	
	/**
	 * @brief Adds the measures from the given result
	 * @param asr The result to add
	 */
	@Override
	public void addCounting(AbstractSingleResult asr) {
		ODSingleStatsResult ossr = (ODSingleStatsResult) asr;
		allDistances.addAll(ossr.allDistances);
		allTravelTimes.addAll(ossr.allTravelTimes);
		allValues.addAll(ossr.allValues);
		allKCals.addAll(ossr.allKCals);
		allPrices.addAll(ossr.allPrices);
		allCO2s.addAll(ossr.allCO2s);
	}


	/**
	 * @brief Norms the computed measures
	 * @param numOrigins The number of origins
	 * @param originsWeight The sum of the origins' weights
	 * @return The normed result
	 */
	@Override
	public AbstractSingleResult getNormed(int numOrigins, double originsWeight) {
		ODSingleStatsResult srnm = new ODSingleStatsResult(originID, destID);
		srnm.allDistances = new Vector<Double>(allDistances);
		srnm.allTravelTimes = new Vector<Double>(allTravelTimes);
		srnm.allValues = new Vector<Double>(allValues);
		srnm.allKCals = new Vector<Double>(allKCals);
		srnm.allPrices = new Vector<Double>(allPrices);
		srnm.allCO2s = new Vector<Double>(allCO2s);
		return srnm;
	}


	/**
	 * @brief Adds a single result to the maps
	 * @param dist The distance to add
	 * @param tt The travel time to add
	 * @param value The value to add
	 * @param kCal The kilocalories to add
	 * @param price The price to add
	 * @param CO2 The CO2 emissions to add
	 */
	public void addSingle(double dist, double tt, double value, double kCal, double price, double CO2) {
		allDistances.add(dist);
		allTravelTimes.add(tt);
		allValues.add(value);
		allKCals.add(kCal);
		allPrices.add(price);
		allCO2s.add(CO2);
	}
}

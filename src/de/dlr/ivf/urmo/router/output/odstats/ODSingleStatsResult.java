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

import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;

/**
 * @class ODSingleStatsResult
 * @brief A origin-destination result showing percentiles 
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
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
	 * @param srcID The id of the origin the represented trip starts at
	 * @param destID The id of the destination the represented trip ends at
	 */
	public ODSingleStatsResult(long srcID, long destID) {
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
	public ODSingleStatsResult(long srcID, long destID, MapResult from, MapResult to, DijkstraResult dr) {
		super(srcID, destID, from, to, dr);
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
	 * @param numSources The number of sources
	 * @param sourcesWeight The sum of the sources' weights
	 * @return The normed result
	 */
	@Override
	public AbstractSingleResult getNormed(int numSources, double sourcesWeight) {
		ODSingleStatsResult srnm = new ODSingleStatsResult(srcID, destID);
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

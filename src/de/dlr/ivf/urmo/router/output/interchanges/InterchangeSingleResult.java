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
package de.dlr.ivf.urmo.router.output.interchanges;

import java.util.HashMap;
import java.util.Map;

import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;

/**
 * @class InterchangeSingleResult
 * @brief Interchanges usage interpretation of a route 
 * @author Daniel Krajzewicz
 */
public class InterchangeSingleResult extends AbstractSingleResult {
	/**
	 * @class InterchangeParam
	 * @brief Statistics about using an interchange
	 */
	class InterchangeParam {
		/// @brief The number of interchanges of this type
		long number = 0;
		/// @brief The weighted travel time needed to perform the interchanges of that type
		double weightedTT = 0;
	}
	
	/// @brief A map from halt ID to (from, to, number, tt)
	public Map<String, Map<String, InterchangeParam>> stats = new HashMap<>(); 
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Generates an empty entry.
	 * @param srcID The id of the origin the represented trip starts at
	 * @param destID The id of the destination the represented trip ends at
	 */
	public InterchangeSingleResult(long srcID, long destID) {
		super(srcID, destID);
	}
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Computes the distance and the travel time
	 * @param result The processed path between the origin and the destination
	 */
	public InterchangeSingleResult(SingleODResult result) {
		super(result);
	}
	
	
	/**
	 * @brief Adds the measures from the given result
	 * @param asr The result to add
	 */
	@Override
	public synchronized void addCounting(AbstractSingleResult asr) {
		InterchangeSingleResult srnm = (InterchangeSingleResult) asr;
		for(String id : srnm.stats.keySet()) {
			if(!stats.containsKey(id)) {
				stats.put(id, new HashMap<String, InterchangeParam>());
			}
			Map<String, InterchangeParam> ssstats = srnm.stats.get(id);
			Map<String, InterchangeParam> dsstats = stats.get(id);
			for(String id2 : ssstats.keySet()) {
				if(!dsstats.containsKey(id2)) {
					dsstats.put(id2, new InterchangeParam());
				}
				dsstats.get(id2).number += ssstats.get(id2).number;
				dsstats.get(id2).weightedTT += ssstats.get(id2).weightedTT;
			}
		}
	}
	
	
	/**
	 * @brief Norms the computed measures
	 * @param numOrigins The number of origins
	 * @param originsWeight The sum of the origins' weights
	 * @return The normed result
	 */
	@Override
	public AbstractSingleResult getNormed(int numOrigins, double originsWeight) {
		InterchangeSingleResult srnm = new InterchangeSingleResult(srcID, destID);
		for(String id : stats.keySet()) {
			srnm.stats.put(id, new HashMap<String, InterchangeParam>());
			Map<String, InterchangeParam> ssstats = stats.get(id);
			Map<String, InterchangeParam> dsstats = srnm.stats.get(id);
			for(String id2 : ssstats.keySet()) {
				InterchangeParam pars = new InterchangeParam();
				pars.number = ssstats.get(id2).number;
				pars.weightedTT = ssstats.get(id2).weightedTT / (double) pars.number;
				dsstats.put(id2, pars);
			}
		}
		return srnm;
	}
	
	
	/**
	 * @brief Adds the information about a single interchange between two distinct lines
	 * @param haltID The id of the halt
	 * @param linesKey The name of the lines interchange ("<FROM_LINE><-><TO_LINE>")
	 * @param number The number of performed interchanges of this type
	 * @param tt The travel time needed to perform an interchange of this type
	 * @return The computed InterchangeParam
	 */
	public InterchangeParam addSingle(String haltID, String linesKey, int number, double tt) {
		if(!stats.containsKey(haltID)) {
			stats.put(haltID, new HashMap<String, InterchangeParam>());
		}
		Map<String, InterchangeParam> ssstats = stats.get(haltID);
		if(!ssstats.containsKey(linesKey)) {
			InterchangeParam pars = new InterchangeParam();
			pars.number = number;
			pars.weightedTT = tt * (double) number;
			ssstats.put(linesKey, pars);
			return pars;
		} else {
			InterchangeParam pars = ssstats.get(linesKey);
			pars.number += number;
			pars.weightedTT += tt * (double) number;
			return pars;
		}
	}
	
	
	/**
	 * @brief Builds a name of the lines interchange ("<FROM_LINE><-><TO_LINE>")
	 * @param fromLine The line to interchange from
	 * @param toLine The line to interchange to
	 * @return The build name
	 */
	public static String buildLinesKey(String fromLine, String toLine) {
		return fromLine+"<->"+toLine;
	}
	
	
	/**
	 * @brief Split the interchange name for obtaining the lines
	 * @param linesKey The name of the lines interchange ("<FROM_LINE><-><TO_LINE>")
	 * @return Both lines
	 */
	public static String[] splitLinesKey(String linesKey) {
		int i = linesKey.indexOf("<->");
		String[] ret = new String[2];
		ret[0] = linesKey.substring(0, i);
		ret[1] = linesKey.substring(i+3);
		return ret;
	}
	
}

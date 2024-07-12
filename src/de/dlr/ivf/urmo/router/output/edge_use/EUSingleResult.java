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
package de.dlr.ivf.urmo.router.output.edge_use;

import java.util.HashMap;
import java.util.Map;

import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;
import de.dlr.ivf.urmo.router.shapes.DBEdge;

/**
 * @class EUSingleResult
 * @brief Edge usage interpretation of a route 
 * @author Daniel Krajzewicz
 */
public class EUSingleResult extends AbstractSingleResult {
	/**
	 * @class EdgeParam
	 * @brief The collected values about an edge usage 
	 */
	class EdgeParam {
		/// @brief The weight number of routes over this edge
		public double num = 0;
		/// @brief The sum of origin weights
		public double originsWeight = 0;

	}
	
	/// @brief Map of edges to numbers
	public Map<String, EdgeParam> stats = new HashMap<>(); 
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Generates an empty entry.
	 * @param originID The id of the origin the represented trip starts at
	 * @param destID The id of the destination the represented trip ends at
	 */
	public EUSingleResult(long originID, long destID) {
		super(originID, destID);
	}
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Computes the distance and the travel time
	 * @param result The processed path between the origin and the destination
	 */
	public EUSingleResult(SingleODResult result) {
		super(result);
	}
	
	
	/**
	 * @brief Adds the measures from the given result
	 * @param asr The result to add
	 */
	@Override
	public synchronized void addCounting(AbstractSingleResult asr) {
		EUSingleResult srnm = (EUSingleResult) asr;
		for(String id : srnm.stats.keySet()) {
			if(!stats.containsKey(id)) {
				stats.put(id, new EdgeParam());
			}
			EdgeParam ssstats = srnm.stats.get(id);
			EdgeParam dsstats = stats.get(id);
			dsstats.num += ssstats.num;
			dsstats.originsWeight += ssstats.originsWeight;
		}
	}
	
	
	/**
	 * @brief Norms the computed measures
	 * @param numSources The number of sources
	 * @param sourcesWeight The sum of the sources' weights
	 * @return The normed result
	 */
	@Override
	public synchronized AbstractSingleResult getNormed(int numSources, double sourcesWeight) {
		EUSingleResult srnm = new EUSingleResult(originID, destID);
		for(String id : stats.keySet()) {
			srnm.stats.put(id, new EdgeParam());
			EdgeParam ssstats = stats.get(id);
			EdgeParam dsstats = srnm.stats.get(id);
			dsstats.num += ssstats.num;
			dsstats.originsWeight += ssstats.originsWeight;
		}
		return srnm;
	}
	

	/**
	 * @brief Adds the information about a single edge
	 * @param e The edge to add the information about
	 * @param value The (variable) value of the destination
	 * @param originWeight The weight of the origin
	 */
	public synchronized void addSingle(DBEdge e, double value, double originWeight) {
		if(!stats.containsKey(e.getID())) {
			stats.put(e.getID(), new EdgeParam());
		}
		EdgeParam curr = stats.get(e.getID());
		curr.num += value;
		curr.originsWeight = originWeight;
	}
}

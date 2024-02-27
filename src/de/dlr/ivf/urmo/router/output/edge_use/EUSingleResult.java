/*
 * Copyright (c) 2016-2024 DLR Institute of Transport Research
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

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;
import de.dlr.ivf.urmo.router.shapes.DBEdge;

/**
 * @class EUSingleResult
 * @brief Edge usage interpretation of a route 
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class EUSingleResult extends AbstractSingleResult {
	/**
	 * @class EdgeParam
	 * @brief The collected values about an edge usage 
	 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
	 */
	class EdgeParam {
		/// @brief The weight number of routes over this edge
		public double num = 0;
		/// @brief The sum of sources weights
		public double sourcesWeight = 0;

	}
	
	/// @brief Map of edges to numbers
	public Map<String, EdgeParam> stats = new HashMap<>(); 
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Generates an empty entry.
	 * @param srcID The id of the origin the represented trip starts at
	 * @param destID The id of the destination the represented trip ends at
	 */
	public EUSingleResult(long srcID, long destID) {
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
	public EUSingleResult(long srcID, long destID, MapResult from, MapResult to, DijkstraResult dr) {
		super(srcID, destID, from, to, dr);
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
			dsstats.sourcesWeight += ssstats.sourcesWeight;
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
		EUSingleResult srnm = new EUSingleResult(srcID, destID);
		for(String id : stats.keySet()) {
			srnm.stats.put(id, new EdgeParam());
			EdgeParam ssstats = stats.get(id);
			EdgeParam dsstats = srnm.stats.get(id);
			dsstats.num += ssstats.num;
			dsstats.sourcesWeight += ssstats.sourcesWeight;
		}
		return srnm;
	}
	

	/**
	 * @brief Adds the information about a single edge
	 * @param e The edge to add the information about
	 * @param value The (variable) value of the destination
	 * @param sourcesWeight The weight of the source
	 */
	public synchronized void addSingle(DBEdge e, double value, double sourcesWeight) {
		if(!stats.containsKey(e.id)) {
			stats.put(e.id, new EdgeParam());
		}
		EdgeParam curr = stats.get(e.id);
		curr.num += value;
		curr.sourcesWeight = sourcesWeight;
	}
}

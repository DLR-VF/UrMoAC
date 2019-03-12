/**
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * 
 * Copyright © 2016-2019 German Aerospace Center 
 * All rights reserved
 */
package de.dlr.ivf.urmo.router.output.edge_use;

import java.util.HashMap;
import java.util.Map;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.modes.Mode;
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
		/// @brief The number of walking routes over this edge 
		public long numWalk = 0;
		/// @brief The number of bicycle routes over this edge 
		public long numBike = 0;
		/// @brief The number of public transport routes over this edge 
		public long numPT = 0;
		/// @brief The number of passenger car routes over this edge 
		public long numCar = 0;
	}
	
	/// @brief Map of edges to numbers
	public Map<String, EdgeParam> stats = new HashMap<>(); 
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Generates an empty entry.
	 * @param _srcID The id of the origin the represented trip starts at
	 * @param _destID The id of the destination the represented trip ends at
	 */
	public EUSingleResult(long srcID, long destID) {
		super(srcID, destID);
	}
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Computes the distance and the travel time
	 * @param _srcID The id of the origin the represented trip starts at
	 * @param _destID The id of the destination the represented trip ends at
	 */
	public EUSingleResult(long srcID, long destID, MapResult from, MapResult to, DijkstraResult dr) {
		super(srcID, destID, from, to, dr);
	}
	
	
	/**
	 * @brief Adds the measures from the given result
	 * @param asr The result to add
	 */
	@Override
	public void addCounting(AbstractSingleResult asr) {
		EUSingleResult srnm = (EUSingleResult) asr;
		for(String id : srnm.stats.keySet()) {
			if(!stats.containsKey(id)) {
				stats.put(id, new EdgeParam());
			}
			EdgeParam ssstats = srnm.stats.get(id);
			EdgeParam dsstats = stats.get(id);
			dsstats.numWalk += ssstats.numWalk;
			dsstats.numBike += ssstats.numBike;
			dsstats.numPT += ssstats.numPT;
			dsstats.numCar += ssstats.numCar;
		}
	}
	
	
	/**
	 * @brief Norms the computed measures
	 * @param numSources The number of sources
	 * @param sourcesWeight The sum of the sources' weights
	 */
	@Override
	public AbstractSingleResult getNormed(int numSources, double sourcesWeight) {
		EUSingleResult srnm = new EUSingleResult(srcID, destID);
		for(String id : stats.keySet()) {
			srnm.stats.put(id, new EdgeParam());
			EdgeParam ssstats = stats.get(id);
			EdgeParam dsstats = srnm.stats.get(id);
			dsstats.numWalk += ssstats.numWalk;
			dsstats.numBike += ssstats.numBike;
			dsstats.numPT += ssstats.numPT;
			dsstats.numCar += ssstats.numCar;
		}
		return srnm;
	}
	

	/**
	 * @brief Adds the information about a single edge
	 * @param e The edge to add the information about
	 * @param usedMode The mode used to pass this edge
	 */
	public void addSingle(DBEdge e, Mode usedMode) {
		if(!stats.containsKey(e.id)) {
			stats.put(e.id, new EdgeParam());
		}
		EdgeParam curr = stats.get(e.id);
		String modeID = usedMode.mml;
		if(modeID=="passenger") {
			curr.numCar += 1;
		} else if(modeID=="bus") {
			curr.numPT += 1;
		} else if(modeID=="bicycle") {
			curr.numBike += 1;
		} else if(modeID=="foot") {
			curr.numWalk += 1;
		} else {
			throw new RuntimeException("Unsupported mode '" + modeID + "' occured in edge output");
		}
	}
}

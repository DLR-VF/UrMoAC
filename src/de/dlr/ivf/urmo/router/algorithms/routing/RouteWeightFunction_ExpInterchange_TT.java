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
package de.dlr.ivf.urmo.router.algorithms.routing;

import java.util.HashMap;

/**
 * @class RouteWeightFunction_ExpInterchange_TT
 * @brief Weights routes by counting interchanges exponential and addin the travel time
 */
public class RouteWeightFunction_ExpInterchange_TT extends AbstractRouteWeightFunction {
	/// @brief Weights for interchanges
	double scale1, scale2;
	
	
	/** 
	 * @brief Constructor
	 * @param _scale1 Exponential weight
	 * @param _scale2 Factor
	 */
	public RouteWeightFunction_ExpInterchange_TT(double _scale1, double _scale2) {
		scale1 = _scale1;
		scale2 = _scale2;
	}
	
	
	/**
	 * @brief Comparing function
	 * @param c1 First entry 
	 * @param c2 Second entry 
	 * @return Comparison 
	 */
	@Override
	public int compare(DijkstraEntry c1, DijkstraEntry c2) {
		double tt1 = computeWeight(c1);
		double tt2 = computeWeight(c2);
		if(tt1<tt2) {
			return -1;
		} else if(tt1>tt2) {
			return 1;
		}
		return 0;
	}
		
	
	/**
	 * @brief Builds the measures used for weighting
	 * @param prev The prior path element
	 * @param current The current path element
	 * @return A map with build measures
	 */
	public HashMap<String, Object> buildMeasures(DijkstraEntry prev, DijkstraEntry current) {
		HashMap<String, Object> ret = new HashMap<>();
		addInterchangeCount2Measures(ret, prev, current);
		return ret;
	}

	
	/** Computes the costs of the route
	 * @param c The route so far
	 * @return The route's costs
	 */
	public double computeWeight(DijkstraEntry c) {
		int pc = (Integer) c.measures.get("interchanges");
		double pcE = (Math.exp((double) pc*scale1)-1.) * scale2;
		return c.tt + pcE;
	}
	
};


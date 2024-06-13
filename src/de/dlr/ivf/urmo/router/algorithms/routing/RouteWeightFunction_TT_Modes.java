/*
 * Copyright (c) 2016-2024
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
package de.dlr.ivf.urmo.router.algorithms.routing;

import java.util.HashMap;

/**
 * @class RouteWeightFunction_TT_Modes
 * @brief Compares paths by the travel time, then by the number of remaining modes that can be used
 * @author Daniel Krajzewicz
 */
public class RouteWeightFunction_TT_Modes extends AbstractRouteWeightFunction {
	/** @brief Returns the number of required parameters
	 * @return The number of required parameters
	 */
	public int getParameterNumber() {
		return 0;
	}
	
	
	/**
	 * @brief Comparing function
	 * @param c1 First entry 
	 * @param c2 Second entry 
	 * @return Comparison 
	 */
	@Override
	public int compare(DijkstraEntry c1, DijkstraEntry c2) {
		if(c1.tt<c2.tt) {
			return -1;
		} else if(c1.tt>c2.tt) {
			return 1;
		}
		int bc1 = Long.bitCount(c1.availableModes);
		int bc2 = Long.bitCount(c2.availableModes);
		if(bc1>bc2) {
			return -1;
		} else if(bc1<bc2) {
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
		return null;
	}
	
};

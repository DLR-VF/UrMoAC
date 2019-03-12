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
package de.dlr.ivf.urmo.router.algorithms.routing;

import java.util.HashMap;

/**
 * @class RoutingMeasure_MaxInterchange_TT
 * @brief Limits the number of interchanges - a path with more than a given number of interchanges is always the slower one
 */
public class RoutingMeasure_MaxInterchange_TT extends AbstractRoutingMeasure {
	/// @brief The maximum number of interchanges
	int scale1;
	

	/** 
	 * @brief Constructor
	 * @param _scale1 The maximum number of interchanges
	 */
	public RoutingMeasure_MaxInterchange_TT(int _scale1) {
		scale1 = _scale1;
	}
	
	
	/**
	 * @brief Comparing function
	 * @param c1 First entry 
	 * @param c2 Second entry 
	 * @return Comparison 
	 */
	@Override
	public int compare(DijkstraEntry c1, DijkstraEntry c2) {
		int pc1 = (Integer) c1.measures.get("interchanges");
		int pc2 = (Integer) c2.measures.get("interchanges");
		if(pc1<=scale1 && pc2>scale1) {
			return -1;
		} else if(pc1>scale1 && pc2<=scale1) {
			return 1;
		} else {
			/*
			if(pc1<pc2) {
				return -1;
			} else if(pc1>pc2) {
				return 1;
			}
			*/
			if(c1.tt<c2.tt) {
				return -1;
			} else if(c1.tt>c2.tt) {
				return 1;
			}
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
	
};


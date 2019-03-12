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
import java.util.HashSet;

/**
 * @class RoutingMeasure_Price_TT
 * @brief Compares paths by price, then by the travel time
 */
public class RoutingMeasure_Price_TT extends AbstractRoutingMeasure {
	/**
	 * @brief Comparing function
	 * @param c1 First entry 
	 * @param c2 Second entry 
	 * @return Comparison 
	 */
	@Override
	public int compare(DijkstraEntry c1, DijkstraEntry c2) {
		double pc1 = (Double) c1.measures.get("price");
		double pc2 = (Double) c2.measures.get("price");
		if(pc1<pc2) {
			return -1;
		} else if(pc1>pc2) {
			return 1;
		}
		if(c1.tt<c2.tt) {
			return -1;
		} else if(c1.tt>c2.tt) {
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
		HashSet<String> lines = new HashSet<String>();
		if(prev!=null) {
			lines.addAll((HashSet<String>) prev.measures.get("lines"));
		}
		ret.put("lines", lines);
		ret.put("price", current.e.getPrice(current.usedMode, lines));
		return ret;
	}
	
};

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

import java.util.Comparator;
import java.util.HashMap;

/**
 * @class AbstractRoutingMeasure
 * @brief Base class for methods that weight paths.
 * @author Daniel Krajzewicz (c) 2018 German Aerospace Center, Institute of
 *         Transport Research
 */
public abstract class AbstractRoutingMeasure implements Comparator<DijkstraEntry> {
	/**
	 * @brief Builds the measures used for weighting the path given a new path element
	 * @param prev The prior path element
	 * @param current The current path element
	 * @return A map with build measures
	 */
	public abstract HashMap<String, Object> buildMeasures(DijkstraEntry prev, DijkstraEntry current);


	/**
	 * @brief Computes the information about interchanges performed at the trip
	 * @param ret The map to insert the measure into
	 * @param prev The prior path element
	 * @param current The current path element
	 */
	protected void addInterchangeCount2Measures(HashMap<String, Object> ret, DijkstraEntry prev, DijkstraEntry current) {
		int numInterchanges = 0;
		if(prev!=null) {
			numInterchanges = (Integer) prev.measures.get("interchanges");
			if(current.line.length()!=0) {
				String prevLastPT = (String) prev.measures.get("lastPT");
				if(!prevLastPT.equals(current.line)) {
					numInterchanges = numInterchanges + 1;
				}
			}
		}
		ret.put("interchanges", numInterchanges);
		if(current.line.length()!=0) {
			ret.put("lastPT", current.line);
		} else if(prev!=null) {
			ret.put("lastPT", (String) prev.measures.get("lastPT"));
		} else {
			ret.put("lastPT", "");
		}
	}


}

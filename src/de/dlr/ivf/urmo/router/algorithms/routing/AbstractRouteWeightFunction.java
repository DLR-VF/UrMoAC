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

import java.util.Comparator;
import java.util.HashMap;

import de.dlr.ivf.urmo.router.gtfs.GTFSTrip;

/**
 * @class AbstractRouteWeightFunction
 * @brief Base class for methods that weight paths.
 * @author Daniel Krajzewicz
 */
public abstract class AbstractRouteWeightFunction implements Comparator<DijkstraEntry> {
	/** @brief Returns the number of required parameters
	 * @return The number of required parameters
	 */
	public abstract int getParameterNumber();

	
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
			if(current.line!=null) {
				GTFSTrip prevLastPT = (GTFSTrip) prev.measures.get("lastPT");
				if(!current.line.trip.equals(prevLastPT)) {
					numInterchanges = numInterchanges + 1;
				}
			}
		}
		ret.put("interchanges", numInterchanges);
		if(current.line!=null) {
			ret.put("lastPT", current.line.trip);
		} else if(prev!=null) {
			ret.put("lastPT", (GTFSTrip) prev.measures.get("lastPT"));
		} else {
			ret.put("lastPT", null);
		}
	}


}

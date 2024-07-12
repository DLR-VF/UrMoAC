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
package de.dlr.ivf.urmo.router.output;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraEntry;
import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class AbstractSingleResult
 * @brief The base class for routing results' interpretations
 * @author Daniel Krajzewicz
 */
public abstract class AbstractSingleResult {
	/// @brief The id of the origin the represented trip starts at
	public long srcID;
	/// @brief The id of the destination the represented trip ends at
	public long destID;
	/// @brief The overall distance of this trip
	public double dist = 0;
	/// @brief The overall travel time of this trip
	public double tt = 0;
	/// @brief The value collected at this trip (at the destination)
	public double val = 0;
	/// @brief The path between the origin and the destination
	public DijkstraEntry toEdgeEntry = null;
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Generates an empty entry.
	 * @param _srcID The id of the origin the represented trip starts at
	 * @param _destID The id of the destination the represented trip ends at
	 */
	public AbstractSingleResult(long _srcID, long _destID) {
		srcID = _srcID;
		destID = _destID;
	}
	
	
	/**
	 * @brief Constructor 
	 * 
	 * Computes the distance and the travel time
	 * @param dr The path between the origin and the destination
	 */
	public AbstractSingleResult(SingleODResult result) {
		MapResult from = result.origin;
		MapResult to = result.destination;
		srcID = ((LayerObject) from.em).getOuterID();
		destID = ((LayerObject) to.em).getOuterID();
		dist = result.dist;
		tt = result.tt;
		val = ((LayerObject) from.em).getAttachedValue();
		toEdgeEntry = result.path;//.getPath(to);//.getEdgeInfo(to.edge);
	}
	
	
	/**
	 * @brief Adds the measures from the given result
	 * @param asr The result to add
	 */
	public abstract void addCounting(AbstractSingleResult asr);
	
	
	/**
	 * @brief Norms the computed measures
	 * @param numOrigins The number of origins
	 * @param originsWeight The sum of the origins' weights
	 * @return The normed result
	 */
	public abstract AbstractSingleResult getNormed(int numOrigins, double originsWeight);
	
}

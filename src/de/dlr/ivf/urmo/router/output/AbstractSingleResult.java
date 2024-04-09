/*
 * Copyright (c) 2016-2022 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * Licensed under the GNU General Public License v3.0
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
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class AbstractSingleResult
 * @brief The base class for routing results' interpretations
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
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
	 * @param _srcID The id of the origin the represented trip starts at
	 * @param _destID The id of the destination the represented trip ends at
	 * @param from The mapped source
	 * @param to The mapped destination
	 * @param dr The path between the source and the destination
	 */
	public AbstractSingleResult(long _srcID, long _destID, MapResult from, MapResult to, DijkstraResult dr) {
		srcID = _srcID;
		destID = _destID;

		DijkstraEntry toEdgeEntry = dr.getEdgeInfo(to.edge);
		val = ((LayerObject) from.em).getAttachedValue();
		double firstTT = toEdgeEntry.first.ttt;
		if(from.edge==to.edge) {
			tt = firstTT;
			if(from.pos>to.pos) {
				dist = from.pos - to.pos;
			} else {
				dist = to.pos - from.pos;				
			}
			tt = tt / to.edge.length * dist;
		} else if(from.edge.opposite==to.edge) {
			tt = firstTT;
			if(from.pos>(to.edge.length - to.pos)) {
				dist = from.pos - (to.edge.length - to.pos);
			} else {
				dist = (to.edge.length - to.pos) - from.pos;				
			}
			tt = tt / to.edge.length * dist;
		} else {
			dist = toEdgeEntry.distance;
			tt = toEdgeEntry.tt;
			if(toEdgeEntry.first.e==from.edge.opposite) {
				dist -= (from.edge.length - from.pos);
				tt -= (firstTT - firstTT * from.pos / from.edge.length);
			} else {
				dist -= from.pos;
				tt -= (firstTT * from.pos / from.edge.length);
			}
			if(toEdgeEntry.wasOpposite) {
				dist -= to.pos;
				tt -= toEdgeEntry.ttt * to.pos / to.edge.length;
			} else {
				dist -= (to.edge.length - to.pos);
				tt -= (toEdgeEntry.ttt - toEdgeEntry.ttt * (to.pos / to.edge.length));
			}
		}
		if(dist<0&&dist>-.1) {
			dist = 0;
		}
		if(tt<0&&tt>-.1) {
			tt = 0;
		}
		
		if(dist<0||tt<0) {
			System.err.println("Negative distance or travel time occured between '" + from.em.getOuterID() + "' to '" + to.em.getOuterID() + "'.");
		}
	}
	
	
	/**
	 * @brief Adds the measures from the given result
	 * @param asr The result to add
	 */
	public abstract void addCounting(AbstractSingleResult asr);
	
	
	/**
	 * @brief Norms the computed measures
	 * @param numSources The number of sources
	 * @param sourcesWeight The sum of the sources' weights
	 * @return The normed result
	 */
	public abstract AbstractSingleResult getNormed(int numSources, double sourcesWeight);
	
}

/*
 * Copyright (c) 2016-2025
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

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;

/**
 * @class SingleODResult
 * @brief A single result (connection between an origin and a destination)
 */
public class SingleODResult {
	/// @brief The id of the origin the represented trip starts at
	public MapResult origin;
	/// @brief The id of the destination the represented trip ends at
	public MapResult destination;
	/// @brief The overall distance of this trip
	public double dist = 0;
	/// @brief The overall travel time of this trip
	public double tt = 0;
	/// @brief The path that connects the origin and the destination
	public DijkstraEntry path;
	
	
	/** @brief Constructor
	 * 
	 * @param _origin The origin of the route
	 * @param _destination The destination of the route
	 * @param _path The path that connects the origin and the destination
	 * @param time The time for which the routing was performed 
	 */
	protected SingleODResult(MapResult _origin, MapResult _destination, DijkstraEntry _path, double time) {
		origin = _origin;
		destination = _destination;
		path = _path;
		if(path.prev==null) {
			// first edge
			if(origin.edge==destination.edge) {
				dist = Math.abs(destination.pos - origin.pos);
			} else {
				dist = Math.abs(destination.pos - (origin.edge.getLength() - origin.pos));
			}
			tt = destination.edge.getTravelTime(path.first.usedMode.vmax, time) / destination.edge.getLength() * dist;
		} else {
			double distOff = 0;
			if(!path.wasOpposite) {
				distOff = (destination.edge.getLength() - destination.pos);
			} else {
				distOff = destination.pos;
			}
			dist = path.distance - distOff;
			tt = path.tt - (path.ttt * distOff / destination.edge.getLength());
		}
		if(dist<0&&dist>-.1) {
			dist = 0;
		}
		if(tt<0&&tt>-.1) {
			tt = 0;
		}
		
		if(dist<0||tt<0) {
			System.err.println("Negative distance or travel time occurred between '" + origin.em.getOuterID() + "' to '" + destination.em.getOuterID() + "'.");
		}
	}


	/**
	 * @brief Returns whether the given requirements are fulfilled
	 * @param needsPT Whether the path must contain a PT element
	 * @return Whether the given requirements are fulfilled
	 */
	public boolean matchesRequirements(boolean needsPT) {
		if(!needsPT) {
			return true;
		}
		DijkstraEntry current = path;
		do {
			if(current.e.isGTFSEdge()) {
				return true;
			}
			current = current.prev;
		} while(current!=null);
		return false;
	}
	
	
}

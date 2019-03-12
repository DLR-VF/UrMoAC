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
package de.dlr.ivf.urmo.router.algorithms.edgemapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.dlr.ivf.urmo.router.shapes.DBEdge;

/**
 * @interface MapResult
 * @brief The result of mapping an EdgeMappable onto an edge
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class MapResult {
	/// @brief The located thing
	public EdgeMappable em;
	/// @brief The edge this thing is located at
	public DBEdge edge;
	/// @brief The distance to the edge
	public double dist;
	/// @brief The position along the edge
	public double pos;


	/**
	 * @brief Constructor
	 * @param _e The edge the thing is located at
	 * @param _dist The distance to the edge
	 * @param _pos The position along the edge
	 */
	public MapResult(EdgeMappable _em, DBEdge _e, double _dist, double _pos) {
		em = _em;
		edge = _e;
		dist = _dist;
		pos = _pos;
	}


	// -----------------------------------------------------------------------
	// helper methods
	// -----------------------------------------------------------------------
	/**
	 * @brief Returns the edges objects in the given map are mapped to
	 * @param mapping A map of objects to edges
	 * @return The list of the edges referenced by the mapped objects
	 */
	public static Set<DBEdge> results2edgeSet(HashMap<EdgeMappable, MapResult> mapping) {
		Set<DBEdge> r = new HashSet<>();
		for (MapResult res : mapping.values()) {
			r.add(res.edge);
		}
		return r;
	}
}

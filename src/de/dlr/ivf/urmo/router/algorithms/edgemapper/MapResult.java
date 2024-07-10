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
package de.dlr.ivf.urmo.router.algorithms.edgemapper;

import de.dlr.ivf.urmo.router.shapes.DBEdge;

/**
 * @class MapResult
 * @brief The result of mapping an EdgeMappable onto the road network
 * 
 * The MapResult describes the mapping of a origin or destination (or a public transport halt)
 * to the road network. It consists of
 * - a reference to the mapped object
 * - a reference to the edge the object is mapped onto
 * - the distance of the object and the edge
 * - the position of the mapped object along the edge
 * - whether this MapResult represents the assignment to the opposite edge
 * 
 * The information about being assigned to the opposite edge is used to dismiss these entries from the according outputs. 
 * 
 * @author Daniel Krajzewicz
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
	/// @brief Whether this is an assignment to the opposite edge
	public boolean onOpposite;


	/**
	 * @brief Constructor
	 * @param _em The mapped object
	 * @param _e The edge the thing is located at
	 * @param _dist The distance to the edge
	 * @param _pos The position along the edge
	 * @param _onOpposite Whether this MapResults represents an additional assignment to the opposite edge
	 * @see de.dlr.ivf.urmo.router.output.EdgeMappingWriter
	 */
	public MapResult(EdgeMappable _em, DBEdge _e, double _dist, double _pos, boolean _onOpposite) {
		em = _em;
		edge = _e;
		dist = _dist;
		pos = _pos;
		onOpposite = _onOpposite;
	}


}

/*
 * Copyright (c) 2016-2024 DLR Institute of Transport Research
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
package de.dlr.ivf.urmo.router.shapes;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;

/** @class DBODRelationExt
 * @brief A single origin/destination relation with a weight, extended by information about mapped object(s) 
 * @see DBODRelation
 */
public class DBODRelationExt extends DBODRelation {
	/** @brief Constructor
	 * @param o The origin
	 * @param d The destination
	 * @param w The weight
	 */
	public DBODRelationExt(long o, long d, double w) {
		super(o, d, w);
	}
	
	
	/// @brief The edge the source is located at
	public DBEdge fromEdge;
	
	/// @brief Information about the mapped source
	public MapResult fromMR;
	
	/// @brief The edge the destination is located at
	public DBEdge toEdge;
	
	/// @brief Information about the mapped destination
	public MapResult toMR;
	

}

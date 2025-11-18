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
package de.dlr.ivf.urmo.router.shapes;

/** @class DBODRelation
 * @brief A single origin/destination relation with a weight 
 * @author Daniel Krajzewicz
 */
public class DBODRelation {
	/** @brief Constructor
	 * @param o The origin
	 * @param d The destination
	 * @param w The weight
	 */
	public DBODRelation(long o, long d, double w) {
		origin = o;
		destination = d;
		weight = w;
	}
	
	/// @brief The origin
	public long origin;
	
	/// @brief The destination
	public long destination;
	
	/// @brief The weight
	public double weight;
	

}

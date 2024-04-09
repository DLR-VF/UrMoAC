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
package de.dlr.ivf.urmo.router.shapes;

/** @class DBODRelation
 * @brief A single origin/destination relation with a length
 */
public class DBODRelation {
	/** @brief Constructor
	 * @param o The origin
	 * @param d The destination
	 * @param w The length
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
	
	/// @brief The length
	public double weight;
	

}

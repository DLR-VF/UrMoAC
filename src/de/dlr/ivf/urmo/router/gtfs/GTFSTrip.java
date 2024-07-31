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
package de.dlr.ivf.urmo.router.gtfs;

/**
 * @class GTFSTrip
 * @brief A trip as stored in GTFS
 * @author Daniel Krajzewicz
 */
public class GTFSTrip {
	/// @brief The id of the trip
	public String tripID;
	/// @brief The route
	public GTFSRoute route;


	/**
	 * @brief Constructor
	 * @param _tripID The id of the trip
	 * @param _route The route
	 */
	public GTFSTrip(String _tripID, GTFSRoute _route) {
		route = _route;
		tripID = _tripID;
	}

}

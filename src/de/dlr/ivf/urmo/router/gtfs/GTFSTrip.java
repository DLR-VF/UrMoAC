/*
 * Copyright (c) 2016-2023 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * https://github.com/DLR-VF/UrMoAC
 * Licensed under the Eclipse Public License 2.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.gtfs;

/**
 * @class GTFSTrip
 * @brief A trip as stored in GTFS
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
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

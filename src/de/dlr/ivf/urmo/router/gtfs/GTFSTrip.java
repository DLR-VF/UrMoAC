/**
 * Copyright (c) 2016-2021 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * @author: Daniel.Krajzewicz@dlr.de
 * Licensed under the GNU General Public License v3.0
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
	/// @brief The id of the route
	public String routeID;
	/// @brief The id of the service
	public String serviceID;
	/// @brief The id of the trip
	public String tripID;


	/**
	 * @brief Constructor
	 * @param _routeID The id of the route
	 * @param _serviceID The id of the service
	 * @param _tripID The id of the trip
	 */
	public GTFSTrip(String _routeID, String _serviceID, String _tripID) {
		routeID = _routeID;
		serviceID = _serviceID;
		tripID = _tripID;
	}

}

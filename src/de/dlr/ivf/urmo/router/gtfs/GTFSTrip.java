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
	public int tripID;
	/// @brief The headsign at the pt vehicle
	public String headSign;
	/// @brief The name of this trip
	public String name;


	/**
	 * @brief Constructor
	 * @param _routeID The id of the route
	 * @param _serviceID The id of the service
	 * @param _tripID The id of the trip
	 * @param _headSign The headsign at the pt vehicle
	 * @param _name The name of this trip
	 * @param _blockID The id of the block (parent station)
	 */
	public GTFSTrip(String _routeID, String _serviceID, int _tripID, String _headSign, String _name) {
		routeID = _routeID;
		serviceID = _serviceID;
		tripID = _tripID;
		headSign = _headSign;
		name = _name;
	}

}

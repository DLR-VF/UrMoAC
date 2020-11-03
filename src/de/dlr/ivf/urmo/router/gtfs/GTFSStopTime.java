/**
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraße 2
 * 12489 Berlin
 * Germany
 * 
 * Copyright © 2016-2019 German Aerospace Center 
 * All rights reserved
 */
package de.dlr.ivf.urmo.router.gtfs;

/**
 * @class GTFSStopTime
 * @brief A stop time as stored in GTFS
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class GTFSStopTime {
	/// @brief This halt's id
	public String id;
	/// @brief The arrival time at the station
	public int arrivalTime;
	/// @brief The departure time from the station
	public int departureTime;
	/// @brief The id of the station
	public String stopID;


	/**
	 * @brief Constructor
	 * @param tripID This halt's id
	 * @param _arrivalTime The arrival time at the station
	 * @param _departureTime The departure time from the station
	 * @param _stopID The id of the station
	 */
	public GTFSStopTime(String tripID, int _arrivalTime, int _departureTime, String _stopID) {
		id = tripID;
		arrivalTime = _arrivalTime;
		departureTime = _departureTime;
		stopID = _stopID;
	}


}

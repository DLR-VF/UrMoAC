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
 * @class GTFSStopTime
 * @brief A stop time as stored in GTFS
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class GTFSStopTime {
	/// @brief This halt's id
	public int id;
	/// @brief The arrival time at the station
	public int arrivalTime;
	/// @brief The departure time from the station
	public int departureTime;
	/// @brief The pickup type at this station
	public int pickupType;
	/// @brief The drop-off type at this station
	public int dropOffType;
	/// @brief The id of the station
	public String stopID;


	/**
	 * @brief Constructor
	 * @param _id This halt's id
	 * @param _arrivalTime The arrival time at the station
	 * @param _departureTime The departure time from the station
	 * @param _stopID The id of the station
	 * @param _pickupType The pickup type at this station
	 * @param _dropOffType The drop-off type at this station
	 */
	public GTFSStopTime(int _id, int _arrivalTime, int _departureTime, String _stopID, int _pickupType,
			int _dropOffType) {
		id = _id;
		arrivalTime = _arrivalTime;
		departureTime = _departureTime;
		pickupType = _pickupType;
		dropOffType = _dropOffType;
		stopID = _stopID;
	}

}

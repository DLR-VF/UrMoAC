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
 * @class GTFSStopTime
 * @brief A stop time as stored in GTFS
 * @author Daniel Krajzewicz
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
	 * @param _id This halt's id
	 * @param _arrivalTime The arrival time at the station
	 * @param _departureTime The departure time from the station
	 * @param _stopID The id of the station
	 */
	public GTFSStopTime(String _id, int _arrivalTime, int _departureTime, String _stopID) {
		id = _id;
		arrivalTime = _arrivalTime;
		departureTime = _departureTime;
		stopID = _stopID;
	}

}

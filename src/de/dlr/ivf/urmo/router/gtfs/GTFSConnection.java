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
 * @class GTFSConnection
 * @brief A connection between two pt stations (a single ride between both)
 * @author Daniel Krajzewicz
 */
public class GTFSConnection {
	/// @brief The edge this connection runs over
	public GTFSEdge edge;
	/// @brief The pt line that realises this connection
	public GTFSTrip trip;
	/// @brief The departure time of the pt vehicle from the starting station
	public int departureTime;
	/// @brief The arrival time of the pt vehicle at the ending station
	public int arrivalTime;


	/**
	 * @brief Constructor
	 * @param e The edge representation of this connection
	 * @param _trip The id of the pt trip that realises this connection
	 * @param _departureTime The departure time of the pt vehicle from the starting station
	 * @param _arrivalTime The arrival time of the pt vehicle at the ending station
	 */
	public GTFSConnection(GTFSEdge e, GTFSTrip _trip, int _departureTime, int _arrivalTime) {
		edge = e;
		trip = _trip;
		departureTime = _departureTime;
		arrivalTime = _arrivalTime;
	}


	/** @brief Returns the travel time
	 * @param time The current time
	 * @return The travel time
	 */
	public double getTravelTime(double time) {
		return arrivalTime - time;
	}

	
	/** @brief Returns the waiting time
	 * @param time The current time
	 * @return The waiting time
	 */
	public double getWaitingTime(double time) {
		return departureTime - time;
	}

}

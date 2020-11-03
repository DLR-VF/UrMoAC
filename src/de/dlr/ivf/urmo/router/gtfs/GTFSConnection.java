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
 * @class GTFSConnnection
 * @brief A connection between two pt stations (a single ride between both)
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class GTFSConnection {
	/// @brief The edge this connection runs over
	public GTFSEdge edge;
	/// @brief The pt line that realises this connection
	public String line;
	/// @brief The trip id this connection represents
	public String tripID;
	/// @brief The departure time of the pt vehicle from the starting station
	public int departureTime;
	/// @brief The arrival time of the pt vehicle at the ending station
	public int arrivalTime;


	/**
	 * @brief Constructor
	 * @param _line The pt line that realises this connection
	 * @param _departureTime The departure time of the pt vehicle from the starting station
	 * @param _arrivalTime The arrival time of the pt vehicle at the ending station
	 */
	public GTFSConnection(GTFSEdge e, String _line, String tripID2, int _departureTime, int _arrivalTime) {
		edge = e;
		line = _line;
		tripID = tripID2;
		departureTime = _departureTime;
		arrivalTime = _arrivalTime;
	}

}

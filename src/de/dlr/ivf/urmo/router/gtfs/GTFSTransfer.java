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
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.gtfs;

/**
 * @class GTFSTransfer
 * @brief A transfer as stored in GTFS
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class GTFSTransfer {
	/// @brief The id of the starting stop
	public String fromStopID;
	/// @brief The id of the destination stop
	public String toStopID;
	/// @brief The id of the starting trip
	public String fromTripID;
	/// @brief The id of the destination trip
	public String toTripID;
	/// @brief The transfer type
	public int transferType;
	/// @brief minimum transfer time
	public int minTransferTime;


	/**
	 * @brief Constructor
	 * @param _fromStopID The id of the starting stop
	 * @param _toStopID The id of the destination stop
	 * @param _fromTripID The id of the starting trip
	 * @param _toTripID The id of the destination trip
	 * @param _transferType The transfer type
	 * @param _minTransferTime minimum transfer time
	 */
	public GTFSTransfer(String _fromStopID, String _toStopID, String _fromTripID, String _toTripID, int _transferType,
			int _minTransferTime) {
		fromStopID = _fromStopID;
		toStopID = _toStopID;
		fromTripID = _fromTripID;
		toTripID = _toTripID;
		transferType = _transferType;
		minTransferTime = _minTransferTime;
	}

}

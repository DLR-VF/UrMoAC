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
package de.dlr.ivf.urmo.router.modes;

/**
 * @class Mode
 * @brief A single mode of transport
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of Transport Research
 */
public class Mode {
	/// @brief This mode's id (must be a power of two result, bitset)
	public long id;
	/// @brief A human readable name of the mode
	public String mml;
	/// @brief This mode's vmax
	public double vmax;
	/// @brief The maximum distance traveled using this mode (unused!!!)
	public double maxDist;
	/// @brief kilocalories per hour
	public double kkcPerHour;
	/// @brief CO2 per km
	public double co2PerKm;
	/// @brief price per km
	public double pricePerKm;


	/**
	 * @brief Constructor
	 * @param _id This mode's id (must be a power of two result, bitset)
	 * @param _mml A human readable name of the mode
	 * @param _vmax This mode's vmax
	 * @param _maxDist The maximum distance traveled using this mode (unused!!!)
	 * @param _kkcPerHour The kilocalories consumed per hour when using this mode
	 * @param _co2PerKm The CO2 emitted per kilometer when using this mode
	 * @param _pricePerKm The price of this mode per kilometer
	 */
	public Mode(long _id, String _mml, double _vmax, double _maxDist, double _kkcPerHour, double _co2PerKm, double _pricePerKm) {
		id = _id;
		mml = _mml;
		vmax = _vmax / 3.6;
		maxDist = _maxDist * 1000;
		kkcPerHour = _kkcPerHour;
		co2PerKm = _co2PerKm;
		pricePerKm = _pricePerKm;
	}
}

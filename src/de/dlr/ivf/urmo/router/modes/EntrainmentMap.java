/*
 * Copyright (c) 2016-2025
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
package de.dlr.ivf.urmo.router.modes;

import java.util.HashMap;

/**
 * @class EntrainmentMap
 * @brief Represents which carrier may carry which vehicle
 * @author Daniel Krajzewicz
 */
public class EntrainmentMap {
	/// @brief Map from carrier to carried modes
	public HashMap<String, Long> carrier2carried = new HashMap<>();
	
	
	/**
	 * @brief Adds a mode to carry to a given carrier
	 * @param carrier_full The name of the carrier
	 * @param carried The mode that may be carried
	 */
	public void add(String carrier_full, long carried) {
		if(carrier2carried.containsKey(carrier_full)) {
			carrier2carried.put(carrier_full, carrier2carried.get(carrier_full).longValue()|carried);
		} else {
			carrier2carried.put(carrier_full, carried);
		}
	}
}

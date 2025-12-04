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
package de.dlr.ivf.urmo.router.mivspeeds;

import de.dlr.ivf.urmo.router.shapes.DBEdge;

/** @class SpeedModel
 * @brief A simple model for reducing MIV speeds based on given max. allowed speed
 * @author Daniel Krajzewicz
 */
public class SpeedModel {
	/** @brief Constructor
	 */
	public SpeedModel() {
	}

	
	/** @brief Computes the speed under load
	 * @param e The edge to compute the speed for
	 * @param t The time of the day to compute the speed for
	 * @return The computed maximum speed
	 */
	public double compute(DBEdge e, double t) {
		double vmax = e.getVMax();
		if(vmax<30./3.6) {
			return vmax / 1.5;
		} else if(vmax<=60./3.6) {
			return vmax / 2.;
		} else if(vmax<=80./3.6) {
			return vmax / 1.5;
		} else if(vmax<=120./3.6) {
			return vmax / 1.2;
		} else {
			return 140./3.6;
		}
	}
	
}

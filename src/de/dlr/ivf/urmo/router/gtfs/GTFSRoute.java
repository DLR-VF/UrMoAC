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
package de.dlr.ivf.urmo.router.gtfs;

/**
 * @class GTFSRoute
 * @brief A route as stored in GTFS
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class GTFSRoute {
	/// @brief The route's id
	public String id;
	/// @brief The route's short name
	public String nameS;
	/// @brief The route's type
	public int type;
	/// @brief A compound name for a single mode / line representation
	public String nameHack;


	/**
	 * @brief Constructor
	 * @param _id The route's id
	 * @param _nameS The route's short name
	 * @param _type The route's type
	 */
	public GTFSRoute(String _id, String _nameS, int _type) {
		id = _id;
		nameS = _nameS;
		type = _type;
		nameHack = _nameHack();
	}
	
	
	/**
	 * @brief Returns the name of the connection
	 * @return The name of the connection
	 */
	private String _nameHack() {
		String name = "";
		switch(type) {
		case 100:
			name = "re";
			break;
		case 102:
			name = "fern";
			break;
		case 109:
			name = "sbahn";
			break;
		case 400:
			name = "ubahn";
			break;
		case 700:
			name = "bus";
			break;
		case 900:
			name = "tram";
			break;
		case 1000:
			name = "ferry";
			break;
		}
		return name + "(" + nameS + ")/"+id;
	}

}

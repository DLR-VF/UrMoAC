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
	/// @brief The route's long name
	public String nameL;
	/// @brief The route's type
	public int type;
	/// @brief The route's color (as RGB-int)
	public int color;
	/// @brief A compound name for a single mode / line representation
	public String nameHack;


	/**
	 * @brief Constructor
	 * @param _id The route's id
	 * @param _nameS The route's short name
	 * @param _nameL The route's long name
	 * @param _type The route's type
	 * @param _color The route's color (as RGB-int)
	 */
	public GTFSRoute(String _id, String _nameS, String _nameL, int _type, int _color) {
		id = _id;
		nameS = _nameS;
		nameL = _nameL;
		type = _type;
		color = _color;
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
		return name + "(" + nameS + ")";
	}

}

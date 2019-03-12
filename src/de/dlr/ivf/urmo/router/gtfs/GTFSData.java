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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @class GTFSData
 * @brief A container for read GTFS data
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class GTFSData {
	/// @brief A map of ids to the respective stop
	public HashMap<Long, GTFSStop> stops;
	/// @brief A map of ids to the respective route
	public HashMap<String, GTFSRoute> routes;
	/// @brief A map of ids to the respective trip
	public HashMap<Integer, GTFSTrip> trips;
	/// @brief A set of edges (!!! unused?)
	public Set<GTFSEdge> connections;

	/// @brief TODO: some kind of an intermediate storage for dealing with a
	/// line names; to be replaced by something honest
	HashMap<String, String> namemap = new HashMap<>();


	/**
	 * @brief Constructor
	 * @param _stops A map of ids to the respective stop
	 * @param _routes A map of ids to the respective route
	 * @param _trips A list of trips
	 * @param _connections A set of edges (!!! unused?)
	 */
	public GTFSData(HashMap<Long, GTFSStop> _stops, HashMap<String, GTFSRoute> _routes, HashMap<Integer, GTFSTrip> _trips,
			Set<GTFSEdge> _connections) {
		stops = _stops;
		routes = _routes;
		trips = _trips;
		connections = _connections;

		namemap.put("100", "RE");
		namemap.put("109", "S-Bahn");
		namemap.put("400", "U-Bahn");
		namemap.put("700", "Bus");
		namemap.put("900", "Tram");
		namemap.put("1000", "Ferry");

	}


	/**
	 * @brief TODO: a hack for obtaining the used mode
	 * @param lines The used lines
	 * @return The used modes
	 */
	public String getModesString(Set<String> lines) {
		Set<String> modes = new HashSet<>();
		for (String line : lines) {
			if (line == null) {
				continue;
			}
			if ("foot".equals(line)) {
				modes.add("foot");
				continue;
			}
			if ("bicycle".equals(line)) {
				modes.add("bicycle");
				continue;
			}
			if ("passenger".equals(line)) {
				modes.add("passenger");
				continue;
			}
			/* !!!!
			String mode = line;
			GTFSRoute route = routes.get(Long.decode(line));
			if(route!=null) {
				String mode2 = namemap.get("" + routes.get(Long.decode(line)).type);
				if(mode2!=null) {
					mode = mode2;
				}
			}
			modes.add(mode);
			*/
		}
		return modes.toString();
	}

}

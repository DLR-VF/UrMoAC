/**
 * Copyright (c) 2016-2021 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * @author: Daniel.Krajzewicz@dlr.de
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

import java.util.HashMap;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.modes.EntrainmentMap;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBNode;

/**
 * @class GTFSStop
 * @brief A stop as stored in GTFS
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of Transport Research
 */
public class GTFSStop extends DBNode implements EdgeMappable {
	/// @brief The stop's id
	public String mid;
	/// @brief The location of this stop
	public Point point;
	/// @brief A map of one-hop destinations to routes to pt edges that start at this node
	public HashMap<GTFSStop, HashMap<GTFSRoute, GTFSEdge>> connections = new HashMap<>();
	/// @brief The map of interchange times between different lines
	public HashMap<String, HashMap<String, Double>> myInterchangeTimes = new HashMap<>();


	/**
	 * @brief Constructor
	 * @param _id The stop's id
	 * @param _mid The stop's id !!!clarify why twice
	 * @param _pos The location of this stop
	 * @param _point The location of this stop!!!clarify why twice
	 */
	public GTFSStop(long _id, String _mid, Coordinate _pos, Point _point) {
		super(_id, _pos);
		mid = _mid;
		point = _point;
	}


	/**
	 * @brief Returns this stop's outer id
	 * @return This stop's id
	 */
	@Override
	public long getOuterID() {
		return id;
	}


	/**
	 * @brief Returns this stop's position
	 * @return This stop's position
	 */
	@Override
	public Point getPoint() {
		return point;
	}


	/**
	 * @brief Returns the stop's geometry
	 * @return The stop's geometry (position only)
	 */
	@Override
	public Geometry getGeometry() {
		return point;
	}


	/**
	 * @brief Returns the connection to the given stop (building it if not yet given)
	 * !!! TODO: it may happen that one mode of transport operating between two subsequent halts allows entrainment while other does not!!!
	 * @param to The destination stop
	 * @param nextID A running id
	 * @param route The route that realises this connection
	 * @param em The enttrainment map
	 * @param pm The used preicision model
	 * @param srid The used projection
	 * @return The built or already available edge
	 */
	public GTFSEdge getEdgeTo(GTFSStop to, long nextID, GTFSRoute route, EntrainmentMap em, PrecisionModel pm, int srid) {
		if (!connections.containsKey(to)) {
			connections.put(to, new HashMap<GTFSRoute, GTFSEdge>());
		}
		HashMap<GTFSRoute, GTFSEdge> m = connections.get(to);
		if (!m.containsKey(route)) {
			long modes = Modes.getMode("foot").id;
			if(em.carrier2carried.containsKey("pt"+route.type)) {
				modes |= Modes.getMode(em.carrier2carried.get("pt"+route.type)).id;
			}
			double length = this.pos.distance(to.pos);
			Coordinate coord[] = new Coordinate[2];
			coord[0] = point.getCoordinate();
			coord[1] = to.point.getCoordinate();
			LineString ls = new LineString(coord, pm, srid);
			GTFSEdge e = new GTFSEdge(nextID, this.mid + "_to_" + to.mid + "_using_" + route.nameS, this, to, modes, 80, ls, length, route);
			m.put(route, e);
			return e;
		}
		return m.get(route);
	}
	
	
	
	/**
	 * @brief Sets the interchange time at this stop between two lines
	 * @param line First line
	 * @param line2 Second line
	 * @param time The interchange time
	 */
	public void setInterchangeTime(String line, String line2, double time) {
		if(!myInterchangeTimes.containsKey(line)) {
			myInterchangeTimes.put(line, new HashMap<String, Double>());
		}
		HashMap<String, Double> it2 = myInterchangeTimes.get(line);
		it2.put(line2, time);
	}


	/**
	 * @brief Returns the set interchange time - or no time if no time is given
	 * @param line First line
	 * @param line2 Second line
	 * @param defaultTime The default interchange time
	 * @return The interchange time
	 */
	@Override
	public double getInterchangeTime(String line, String line2, double defaultTime) {
		if(line.equals(line2)) {
			return 0;
		}
		if(myInterchangeTimes.containsKey(line)) {
			HashMap<String, Double> it2 = myInterchangeTimes.get(line);
			if(it2.containsKey(line2)) {
				return it2.get(line2);
			}
		}
		return defaultTime;
	}

	
}

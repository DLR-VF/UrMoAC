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
package de.dlr.ivf.urmo.router.gtfs;

import java.util.HashMap;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.modes.EntrainmentMap;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.DBNode;

/**
 * @class GTFSStop
 * @brief A stop as stored in GTFS
 * @author Daniel Krajzewicz
 */
public class GTFSStop extends DBNode implements EdgeMappable {
	/// @brief The stop's id
	public String mid;
	/// @brief The location of this stop
	public Point point;
	/// @brief A map of one-hop destinations to routes to pt edges that start at this node
	public HashMap<GTFSStop, HashMap<GTFSRoute, GTFSEdge>> connections = new HashMap<>();
	/// @brief The map of interchange times between different lines
	public HashMap<GTFSTrip, HashMap<GTFSTrip, Double>> myInterchangeTimes = new HashMap<>();


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
		return getID();
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
	 * @brief Returns the envelope of this thing
	 * @return The bounding box
	 */
	public Envelope getEnvelope() {
		return point.getEnvelopeInternal();
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
	 * @param route The route that realises this connection
	 * @param em The enttrainment map
	 * @param net The network, needed for obtaining the geometry factory
	 * @return The built or already available edge
	 */
	public GTFSEdge getEdgeTo(GTFSStop to, GTFSRoute route, EntrainmentMap em, DBNet net) {
		if (!connections.containsKey(to)) {
			connections.put(to, new HashMap<GTFSRoute, GTFSEdge>());
		}
		HashMap<GTFSRoute, GTFSEdge> m = connections.get(to);
		if (!m.containsKey(route)) {
			long modes = Modes.getMode("foot").id;
			if(em.carrier2carried.containsKey("pt"+route.type)) {
				modes |= em.carrier2carried.get("pt"+route.type);
			}
			double length = this.getCoordinate().distance(to.getCoordinate());
			Coordinate coord[] = new Coordinate[2];
			coord[0] = getCoordinate();
			coord[1] = to.getCoordinate();
			LineString ls = net.getGeometryFactory().createLineString(coord);
			GTFSEdge e = new GTFSEdge(this.mid + "_to_" + to.mid + "_using_" + route.nameS, this, to, modes, 80, ls, length, 0, route);
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
	public void setInterchangeTime(GTFSTrip line, GTFSTrip line2, double time) {
		if(!myInterchangeTimes.containsKey(line)) {
			myInterchangeTimes.put(line, new HashMap<GTFSTrip, Double>());
		}
		HashMap<GTFSTrip, Double> it2 = myInterchangeTimes.get(line);
		it2.put(line2, time);
	}


	/**
	 * @brief Returns the set interchange time - or no time if no time is given
	 * @param line First line
	 * @param line2 Second line
	 * @param defaultTime The default interchange time
	 * @return The interchange time
	 * @todo play with this
	 */
	public double getInterchangeTime(GTFSTrip line, GTFSTrip line2, double defaultTime) {
		if(line.equals(line2)) {
			return 0;
		}
		if(myInterchangeTimes.containsKey(line)) {
			HashMap<GTFSTrip, Double> it2 = myInterchangeTimes.get(line);
			if(it2.containsKey(line2)) {
				return it2.get(line2);
			}
		}
		return defaultTime;
	}

	
}

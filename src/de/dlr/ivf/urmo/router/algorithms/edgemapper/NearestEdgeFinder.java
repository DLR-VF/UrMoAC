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
 * Rutherfordstraße 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.algorithms.edgemapper;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;

import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.GeomHelper;

/**
 * @class NearestEdgeFinder
 * @brief For a given set of objects and a given road network, this class
 *        computes the nearest road for each object.
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class NearestEdgeFinder {
	/// @brief The list of objects to allocate in the network
	private Vector<EdgeMappable> source;
	/// @brief The transport modes to use
	private long modes;
	/// @brief A spatial index
	private STRtree tree = null;
	/// @brief The resulting geometry factory
	GeometryFactory geometryFactory = null;

	// @brief Enum for points being on the right side of the road
	private static final int DIRECTION_RIGHT = 1;
	// @brief Enum for points being on the left side of the road
	private static final int DIRECTION_LEFT = -1;
	


	/**
	 * @brief Constructor
	 * @param _source The list of objects to allocate in the network
	 * @param _net The network to use
	 * @param _modes Bitset of usable transport modes
	 */
	public NearestEdgeFinder(Vector<EdgeMappable> _source, DBNet _net, long _modes) {
		source = _source;
		modes = _modes;
		tree = _net.getModedSpatialIndex(modes);
		geometryFactory = _net.getGeometryFactory();
	}


	/**
	 * @brief Builds and returns the mapping of objects to edges
	 * @param addToEdge if set, the objects will be added to the respectively found edges
	 * @return The map of objects to edges
	 */
	public HashMap<DBEdge, Vector<MapResult>> getNearestEdges(boolean addToEdge) {
		ItemDistance itemDist = new ItemDistance() {
		    @Override
		    public double distance(ItemBoundable i1, ItemBoundable i2) {
		        DBEdge e = (DBEdge) i1.getItem();
		        EdgeMappable em = (EdgeMappable) i2.getItem();
		        return e.getGeometry().distance(em.getPoint());
		    }
		};
		HashMap<DBEdge, Vector<MapResult>> ret = new HashMap<>();
		// go through the locations
		for (EdgeMappable c : source) {
			// get the next edge envelope
			DBEdge found = (DBEdge) tree.nearestNeighbour(c.getEnvelope(), c, itemDist);
			if(found==null) {
				continue; // add error message
			}
			Point p = c.getPoint();
			double minDist = p.distance(found.geom);
			int minDir = getDirectionToPoint(found, p);
			// we now have to check all edges within an envelope that covers
			// the circle with the found distance
			double cdist = minDist * 1.415; // sqrt(2)
			Envelope env = new Envelope(new Coordinate(p.getX()-cdist, p.getY()-cdist), new Coordinate(p.getX()+cdist, p.getY()+cdist));
			@SuppressWarnings("rawtypes")
			List objs = tree.query(env);
			for(Object o : objs) {
				DBEdge e = (DBEdge) o;
				// get the distance
				double dist = p.distance(e.geom);
				if(dist>minDist+.1) {
					// currently seen is further away than the initial
					continue;
				}
				// get the current edge's direction (at minimum distance)
				int dir = getDirectionToPoint(e, p);
				if(dir==DIRECTION_RIGHT) {
					// ok, the point is on the right side of this one
					if(minDir!=DIRECTION_RIGHT || dist<minDist || e.id.compareTo(found.id) > 0) {
						// use this one either if the point was on the false side of the initially found 
						// one or sort by distance or id
						minDist = dist;
						minDir = dir;
						found = e;
					}
				} else if(minDir==DIRECTION_LEFT && (dist<minDist || e.id.compareTo(found.id) > 0)) {
					// the point is on the left and the previous one, too;
					// sort by distance or id
					minDist = dist;
					minDir = dir;
					found = e;
				}
			}
			//
			if(found!=null) {
				Coordinate coord = new Coordinate(p.getX(), p.getY(), 0);
				double posAtEdge = GeomHelper.getDistanceOnLineString(found, coord, p);
				if (addToEdge) {
					found.addMappedObject(c);
				}
				// properly computed, yet
				if (!ret.containsKey(found)) {
					ret.put(found, new Vector<>());
				}
				Vector<MapResult> ress = ret.get(found);
				ress.add(new MapResult(c, found, minDist, posAtEdge));
			}
		}
		return ret;
	}


	// -----------------------------------------------------------------------
	// helper methods
	// -----------------------------------------------------------------------
	/** 
	 * @brief Returns the direction (left/right) into which the opivot oint lies in respect to the edge
	 * @param e The edge
	 * @return The direction of the opivot point
	 */
	private int getDirectionToPoint(DBEdge e, Point p) {
		double minDist = -1;
		double minDir = 0;
		int numPoints = e.geom.getNumPoints();
		Coordinate tcoord[] = ((LineString) e.geom).getCoordinates();
		Coordinate coord[] = new Coordinate[2];
		for(int i=0; i<numPoints-1; ++i) {
			coord[0] = tcoord[i];
			coord[1] = tcoord[i+1];
			LineString ls = geometryFactory.createLineString(coord);
			double dist = p.distance(ls);
			if(minDist<0 || minDist>dist) {
				minDist = dist;
				minDir = (coord[1].x - coord[0].x) * (p.getY() - coord[0].y) - (p.getX() - coord[0].x) * (coord[1].y - coord[0].y);
			}
		}
		return minDir<0 ? DIRECTION_RIGHT : DIRECTION_LEFT;
	}

}

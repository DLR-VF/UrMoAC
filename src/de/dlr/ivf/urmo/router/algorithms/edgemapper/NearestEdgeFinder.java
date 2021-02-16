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
package de.dlr.ivf.urmo.router.algorithms.edgemapper;

import java.util.HashMap;
import java.util.Vector;

import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.SpatialIndex;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.GeomHelper;
import gnu.trove.TIntProcedure;

/**
 * @class NearestEdgeFinder
 * @brief For a given set of objects and a given road network, this class
 *        computes the nearest road for each object.
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class NearestEdgeFinder {
	/// @brief The network to use
	private DBNet net;
	/// @brief The list of objects to allocate in the network
	private Vector<EdgeMappable> source;
	/// @brief The transport modes to use
	private long modes;
	/// @brief A spatial index
	private SpatialIndex rtree = null;
	/// @brief The (resulting after being built) map of object ids to edges
	private HashMap<Integer, DBEdge> id2edge = new HashMap<>();

	/// @brief Temporary found edge
	private DBEdge found;
	/// @brief Temporary minimum distance
	private double minDist;
	/// @brief Temporary minimum direction
	private int minDir;
	/// @brief The centroid of the current object
	private com.vividsolutions.jts.geom.Point opivot;
	
	// @brief Enum for points being on the right side of the road
	private static final int DIRECTION_RIGHT = 1;
	
	// @brief Enum for points being on the left side of the road
	private static final int DIRECTION_LEFT = -1;
	


	/**
	 * @Constructor
	 * @param _source The list of objects to allocate in the network
	 * @param _net The network to use
	 * @param _modes Bitset of usable transport modes
	 */
	public NearestEdgeFinder(Vector<EdgeMappable> _source, DBNet _net, long _modes) {
		source = _source;
		net = _net;
		modes = _modes;
		found = null;
		minDist = -1;
		rtree = _net.getModedSpatialIndex(modes);
		id2edge = _net.getID2EdgeForMode(modes);
	}


	/**
	 * @brief Builds and returns the mapping of objects to edges
	 * @param addToEdge if set, the objects will be added to the respectively found edges
	 * @return The map of objects to edges
	 */
	public HashMap<DBEdge, Vector<MapResult>> getNearestEdges(boolean addToEdge) {
		HashMap<DBEdge, Vector<MapResult>> ret = new HashMap<>();
		for (EdgeMappable c : source) {
			opivot = c.getPoint();
			Point pivot = new Point((float) opivot.getX(), (float) opivot.getY());
			minDist = -1;
			minDir = 0;
			found = null;
			float viewDist = 10;
			while ((found == null && viewDist < Math.max(net.size.x, net.size.y)) || viewDist / 2. < minDist) {
				rtree.nearestN(pivot, new TIntProcedure() {
					@Override
					public boolean execute(int i) {
						DBEdge e = id2edge.get(i);
						if (!e.allowsAny(modes)) {
							return true;
						}
						double dist = opivot.distance(e.geom);
						if (minDist < 0 || (minDist > dist && Math.abs(minDist-dist)>=.1)) {
							minDist = dist;
							minDir = getDirectionToPoint(e);
							found = e;
						} else if (Math.abs(minDist-dist)<.1) {
							// get the current edge's direction (at minimum distance)
							int dir = getDirectionToPoint(e);
							if(dir==DIRECTION_RIGHT) {
								// ok, the point is on the right side of this one
								if(minDir!=DIRECTION_RIGHT || e.id.compareTo(found.id) > 0) {
									// use this one either if the point was on the false side of the initially found one
									// or sort by id
									minDist = dist;
									minDir = dir;
									found = e;
								}
							} else if(minDir==DIRECTION_LEFT && e.id.compareTo(found.id) > 0) {
								// the point is on the left; sort by id if the initially found was on the left side, too
								minDist = dist;
								minDir = dir;
								found = e;
							}
						}
						return true;
					}
				}, 100, viewDist);
				viewDist = viewDist * 2;
			}
			//
			if(found!=null) {
				Coordinate coord = new Coordinate(pivot.x, pivot.y, 0);
				double posAtEdge = GeomHelper.getDistanceOnLineString(found, coord, opivot);
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
	 * @brief Returns a hashmap of object information
	 * @param mapping A map of edges to mapped object vectors
	 * @return A map of objects to mapping information
	 */
	public static HashMap<EdgeMappable, MapResult> results2edgeSet(HashMap<DBEdge, Vector<MapResult>> mapping) {
		HashMap<EdgeMappable, MapResult> r = new HashMap<>();
		for (DBEdge e : mapping.keySet()) {
			Vector<MapResult> objects = mapping.get(e);
			for (MapResult mr : objects) {
				r.put(mr.em, mr);
			}
		}
		return r;
	}


	/** 
	 * @brief Returns the direction (left/right) into which the opivot oint lies in respect to the edge
	 * @param e The edge
	 * @return The direction of the opivot point
	 */
	private int getDirectionToPoint(DBEdge e) {
		double minDist = -1;
		double minDir = 0;
		int numPoints = e.geom.getNumPoints();
		Coordinate tcoord[] = ((LineString) e.geom).getCoordinates();
		Coordinate coord[] = new Coordinate[2];
		for(int i=0; i<numPoints-1; ++i) {
			coord[0] = tcoord[i];
			coord[1] = tcoord[i+1];
			LineString ls = new LineString(coord, e.geom.getPrecisionModel(), e.geom.getSRID());
			double dist = opivot.distance(ls);
			if(minDist<0 || minDist>dist) {
				minDist = dist;
				minDir = (coord[1].x - coord[0].x) * (opivot.getY() - coord[0].y) - (opivot.getX() - coord[0].x) * (coord[1].y - coord[0].y);
			}
		}
		return minDir<0 ? DIRECTION_RIGHT : DIRECTION_LEFT;
	}

}

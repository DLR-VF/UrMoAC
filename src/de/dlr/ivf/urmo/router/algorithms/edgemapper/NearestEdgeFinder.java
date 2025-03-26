/*
 * Copyright (c) 2016-2024
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
package de.dlr.ivf.urmo.router.algorithms.edgemapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;

import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.GeomHelper;

/**
 * @class NearestEdgeFinder
 * @brief This class maps objects to the respectively nearest road 
 * 
 * The class returns a map from edges to the list of objects that were mapped on the (assigned to
 * them).
 * 
 * The class uses threads to compute the positions of a given set of object (positions) to the 
 * road network. Threads are implemented as an inner class. Each thread asks for the next object
 * to map using the @see getNextMappable method and returns the result using the @addResult method.
 * The search for the nearest edges uses an @see STRtree. 
 * 
 * Optionally (only in case of destination) the objects are as well assigned to the opposite edge.
 * 
 * Besides returning the information about which objects are mapped on which edges, the class
 * as well adds the objects to the edge they were assigned to. This is an optional action, only
 * performed for destinations.
 * 
 * @author Daniel Krajzewicz
 * @todo Recheck whether the objects should be added to the edges here
 */
public class NearestEdgeFinder {
	/// @brief The list of objects to allocate in the network
	private Vector<EdgeMappable> objects;
	/// @brief Pointer to the next mappable to process
	private Iterator<EdgeMappable> nextMappablePointer;
	/// @brief The results set
	private HashMap<DBEdge, Vector<MapResult>> ret = new HashMap<>();

	/// @brief The transport modes to use
	private long modes;
	/// @brief A spatial index
	private STRtree tree = null;
	/// @brief The resulting geometry factory
	private GeometryFactory geometryFactory = null;

	// @brief Constant for points being on the right side of the road
	private static final int DIRECTION_RIGHT = 1;
	// @brief Constant for points being on the left side of the road
	private static final int DIRECTION_LEFT = -1;
	

	
	// -----------------------------------------------------------------------
	// ComputingThread
	// -----------------------------------------------------------------------
	/** @class ComputingThread
	 * 
	 * A thread which polls for new origins, computes the accessibility and
	 * writes the results before asking for the next one
	 */
	private static class ComputingThread implements Runnable {
		/// @brief The parent to get information from
		private NearestEdgeFinder parent;
		/// @brief The spatial index of edges
		private STRtree tree;
		/// @brief A distance computation class
		private ItemDistance itemDist;
		/// @brief Whether the mappable should be added to the according edge
		private boolean addToEdge;
		/// @brief The transport modes to use
		private long modes;
		/// @brief Whether the objects should be assigned to the opposite direction of the road as well
		private boolean withOpposites;

		
		/**
		 * @brief Constructor
		 * @param _parent The parent to get information from
		 * @param _tree The spatial index to use to find edges
		 * @param _addToEdge Whether the mappables should be added to the according edges
		 * @param _withOpposites Whether the objects should be assigned to the opposite direction of the road as well
		 * @param _modes The transport modes to use
		 */
		public ComputingThread(NearestEdgeFinder _parent, STRtree _tree, boolean _addToEdge, boolean _withOpposites, long _modes) {
			super();
			parent = _parent;
			tree = _tree;
			addToEdge = _addToEdge;
			modes = _modes;
			withOpposites = _withOpposites;
			itemDist = new ItemDistance() {
			    @Override
			    public double distance(ItemBoundable i1, ItemBoundable i2) {
			        DBEdge e = (DBEdge) i1.getItem();
			        EdgeMappable em = (EdgeMappable) i2.getItem();
			        return e.getGeometry().distance(em.getPoint());
			    }
			};
		}
		
		
		/**
		 * @brief Performs the computation
		 * 
		 * Iterates over mappables, determines the nearest edge.
		 */
		public void run() {
			EdgeMappable c = null;
			do {
				c = parent.getNextMappable();
				if(c==null) {
					continue;
				}
				processMappable(c);
			} while(c!=null);
		}


		/// !!! Initial attempt to process areal destinations
		/*
		private void processMappable2(EdgeMappable mappable) {
			List<Object> results = tree.query(mappable.getEnvelope());
			boolean found = false; 
			for(Object e : results) {
				DBEdge edge = (DBEdge) e;
				LineString geom = edge.getGeometry();
				// completely included
				if(mappable.getGeometry().contains(geom)) {
					parent.addResult(new MapResult(mappable, edge, 0, -1));
					found = true;
				}
				// crosses
				if(mappable.getGeometry().crosses(geom)) {
					parent.addResult(new MapResult(mappable, edge, 0, -1));
					found = true;
				}
			}
		}
		*/
		
		/** 
		 * @brief Determines the edge the mappable is allocated at
		 * @param mappable The mappable to process
		 */
		private void processMappable(EdgeMappable mappable) {
			// get the next nearest edges
			DBEdge found = (DBEdge) tree.nearestNeighbour(mappable.getEnvelope(), mappable, itemDist);
			if(found==null) {
				return; // @todo add error message
			}
			// check opposite
			Point p = mappable.getPoint();
			double minDist = p.distance(found.getGeometry());
			if(found.getOppositeEdge()!=null&&found.getOppositeEdge().allowsAny(modes)) {
				double dist = p.distance(found.getOppositeEdge().getGeometry());
				if(dist-minDist<.1) {
					minDist = dist;
					int minDir = parent.getDirectionToPoint(found, p);
					// get the current edge's direction (at minimum distance)
					int dir = parent.getDirectionToPoint(found.getOppositeEdge(), p);
					if(dir==DIRECTION_RIGHT) {
						// ok, the point is on the right side of this one
						if(minDir!=DIRECTION_RIGHT || dist<minDist || found.getOppositeEdge().getID().compareTo(found.getID()) > 0) {
							// use this one either if the point was on the false side of the initially found 
							// one or sort by distance or id
							minDist = dist;
							minDir = dir;
							found = found.getOppositeEdge();
						}
					} else if(minDir==DIRECTION_LEFT && (dist<minDist || found.getOppositeEdge().getID().compareTo(found.getID()) > 0)) {
						// the point is on the left and the previous one, too;
						// sort by distance or id
						minDist = dist;
						minDir = dir;
						found = found.getOppositeEdge();
					}
				}
			}
			//
			if(found!=null) {
				if(minDist==0&&found.getID().startsWith("opp_")&&found.getOppositeEdge()!=null) {
					// just beautifying the results
					found = found.getOppositeEdge();
				}
				Coordinate coord = new Coordinate(p.getX(), p.getY(), 0);
				double posAtEdge = GeomHelper.getDistanceOnLineString(found, coord, p);
				if (addToEdge) {
					found.addMappedObject(mappable);
				}
				parent.addResult(new MapResult(mappable, found, minDist, posAtEdge, false));
				if(withOpposites&&found.getOppositeEdge()!=null) {
					posAtEdge = GeomHelper.getDistanceOnLineString(found.getOppositeEdge(), coord, p);
					if (addToEdge) {
						found.getOppositeEdge().addMappedObject(mappable);
					}
					parent.addResult(new MapResult(mappable, found.getOppositeEdge(), minDist, posAtEdge, true));
				}
			}
		}
	}
	
	
	
	// -----------------------------------------------------------------------
	// NearestEdgeFinder
	// -----------------------------------------------------------------------
	/**
	 * @brief Constructor
	 * @param _objects The list of objects to allocate in the network
	 * @param _net The network to use
	 * @param _modes Bitset of usable transport modes
	 */
	public NearestEdgeFinder(Vector<EdgeMappable> _objects, DBNet _net, Vector<Mode> _modes) {
		objects = _objects;
		modes = Modes.getCombinedModeIDs(_modes);
		tree = _net.getModedSpatialIndex(modes);
		geometryFactory = _net.getGeometryFactory();
	}


	/**
	 * @brief Adds the found mapping result to the results set
	 * @param mapResult The computed mapping result
	 */
	private synchronized void addResult(MapResult mapResult) {
		DBEdge found = mapResult.edge;
		if (!ret.containsKey(found)) {
			ret.put(found, new Vector<>());
		}
		Vector<MapResult> ress = ret.get(found);
		ress.add(mapResult);
	}


	/**
	 * @brief Returns the next mappable to process
	 * @return The next mappable to process
	 */
	private synchronized EdgeMappable getNextMappable() {
		EdgeMappable nextMappable = null;
		while(nextMappable==null) {
			if(!nextMappablePointer.hasNext()) {
				return null;
			}
			nextMappable = nextMappablePointer.next();
		}
		return nextMappable;
	}


	/**
	 * @brief Builds and returns the mapping of objects to edges
	 * @param addToEdge if set, the objects will be added to the respectively found edges
	 * @return The map of objects to edges
	 */
	public HashMap<DBEdge, Vector<MapResult>> getNearestEdges(boolean addToEdge, boolean withOpposites, int numThreads) {
		// start threads
		nextMappablePointer = objects.iterator();
		Vector<Thread> threads = new Vector<>();
		for (int i=0; i<numThreads; ++i) {
			Thread t = new Thread(new ComputingThread(this, tree, addToEdge, withOpposites, modes));
			threads.add(t);
	        t.start();
		}
		// close threads
		for(Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}


	// -----------------------------------------------------------------------
	// helper methods
	// -----------------------------------------------------------------------
	/** 
	 * @brief Returns the direction (left/right) into which the point lies in respect to the edge
	 * @param e The edge
	 * @param p The point
	 * @return The direction of the pivot point
	 */
	private int getDirectionToPoint(DBEdge e, Point p) {
		double minDist = -1;
		double minDir = 0;
		int numPoints = e.getGeometry().getNumPoints();
		Coordinate tcoord[] = ((LineString) e.getGeometry()).getCoordinates();
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

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
package de.dlr.ivf.urmo.router.algorithms.edgemapper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
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
	private Queue<EdgeMappable> source = new LinkedList<>();
	HashMap<DBEdge, Vector<MapResult>> nearestEdges = new HashMap<>();
	/// @brief The transport modes to use
	private long modes;
	/// @brief The (resulting after being built) map of object ids to edges
	private HashMap<Integer, DBEdge> id2edge = new HashMap<>();
	
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
		//convert vector to Queue
		for(EdgeMappable e: _source) {
			source.add(e);
		}
		net = _net;
		modes = _modes;
		id2edge = _net.getID2EdgeForMode(modes);
	}


	/**
	 * @brief Builds and returns the mapping of objects to edges
	 * @param addToEdge if set, the objects will be added to the respectively found edges
	 * @return The map of objects to edges
	 */
	public HashMap<DBEdge, Vector<MapResult>> getNearestEdges(boolean addToEdge) {
		return this.getNearestEdges(addToEdge, 1);
	}
	

	/**
	 * @brief Builds and returns the mapping of objects to edges
	 * @param addToEdge if set, the objects will be added to the respectively found edges
	 * @param numOfThreads number of threads to be used
	 * @return The map of objects to edges
	 */
	public HashMap<DBEdge, Vector<MapResult>> getNearestEdges(boolean addToEdge, int numOfThreads) {
		SpatialIndex[] rtrees = new SpatialIndex[numOfThreads];
		
		for(int i=0; i< numOfThreads; i++) {
			rtrees[i]=  net.getModedSpatialIndex(modes); //every thread needs its own rtree, because rtree is not thread-save :( 
		}
		return this.getNearestEdges(addToEdge, numOfThreads, rtrees);
	}
	
	/**
	 * @brief Builds and returns the mapping of objects to edges
	 * @param addToEdge if set, the objects will be added to the respectively found edges
	 * @param numOfThreads number of threads to be used
	 * @param rtrees the rtrees to be used. They might be recycled from the last run to save computing time!
	 * @return The map of objects to edges
	 */
	public HashMap<DBEdge, Vector<MapResult>> getNearestEdges(boolean addToEdge, int numOfThreads, SpatialIndex[] rtrees) {		

		Vector<Thread> threads = new Vector<>();
		
		//start edge finder
		for (int t=0; t<numOfThreads ; t++) {
			Thread thread = new Thread(new ComputingThread(addToEdge,rtrees[t]));
			threads.add(thread);
			thread.start();
		}
		//wait for completion
		for(Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}		
		return this.nearestEdges;
	}

	private class ComputingThread implements Runnable {
		
		//must be a function member due to function "rtree.nearestN"
		DBEdge foundEdgeThread = null;
		double minDistThread = -1;
		int minDirThread = 0;
		boolean addToEdge =false;
		SpatialIndex rtree;

		
		public ComputingThread(boolean addToEdge,SpatialIndex rtree) {
			this.addToEdge=addToEdge;
			this.rtree=rtree;
		}
		
		public void run() {
			if(nearestEdges ==null) {
				System.err.println("Wrong initialized thread!");
			}
			EdgeMappable c =null;
			do {
				//get a new point
				synchronized(source) {
					c=source.poll();
				}
				if (c != null) {
					
					foundEdgeThread = null;
					minDistThread = -1;
					minDirThread = 0;
					com.vividsolutions.jts.geom.Point opivot = c.getPoint();
					Point pivot = new Point((float) opivot.getX(), (float) opivot.getY());

					float viewDist = 10;
					
					//find the closest rectangle with an edge within the given viewDist
					while ((foundEdgeThread == null && viewDist < Math.max(net.size.x, net.size.y))) {
						rtree.nearest(pivot, new TIntProcedure() {
							@Override
							public boolean execute(int i) {
								DBEdge e = id2edge.get(i);
								if (!e.allowsAny(modes)) {
									return true;
								}
								double dist = opivot.distance(e.geom);
								if (minDistThread < 0
										|| (minDistThread > dist && Math.abs(minDistThread - dist) >= .1)) {
									minDistThread = dist;
									minDirThread = getDirectionToPoint(e, opivot);
									foundEdgeThread = e;
								} else if (Math.abs(minDistThread - dist) < .1) {
									// get the current edge's direction (at minimum distance)
									int dir = getDirectionToPoint(e, opivot);
									if (dir == DIRECTION_RIGHT) {
										// ok, the point is on the right side of this one
										if (minDirThread != DIRECTION_RIGHT || e.id.compareTo(foundEdgeThread.id) > 0) {
											// use this one either if the point was on the false side of the initially
											// found one
											// or sort by id
											minDistThread = dist;
											minDirThread = dir;
											foundEdgeThread = e;
										}
									} else if (minDirThread == DIRECTION_LEFT
											&& e.id.compareTo(foundEdgeThread.id) > 0) {
										// the point is on the left; sort by id if the initially found was on the left
										// side, too
										minDistThread = dist;
										minDirThread = dir;
										foundEdgeThread = e;
									}
								}
								return true;
							}
						}, viewDist);
						viewDist = viewDist * 10;
					}
					
					//OK, the found edge lies in a rectangle around opivot with a side-length of minDistThread. 
					//Due to some diagonal problem a edge in a neighbouring rectangle might be closer. 
					//Therefore search one time again with minDistThread.
					rtree.nearest(pivot, new TIntProcedure() {
						@Override
						public boolean execute(int i) {
							DBEdge e = id2edge.get(i);
							if (!e.allowsAny(modes)) {
								return true;
							}
							double dist = opivot.distance(e.geom);
							if (minDistThread < 0
									|| (minDistThread > dist && Math.abs(minDistThread - dist) >= .1)) {
								minDistThread = dist;
								minDirThread = getDirectionToPoint(e, opivot);
								foundEdgeThread = e;
							} else if (Math.abs(minDistThread - dist) < .1) {
								// get the current edge's direction (at minimum distance)
								int dir = getDirectionToPoint(e, opivot);
								if (dir == DIRECTION_RIGHT) {
									// ok, the point is on the right side of this one
									if (minDirThread != DIRECTION_RIGHT || e.id.compareTo(foundEdgeThread.id) > 0) {
										// use this one either if the point was on the false side of the initially
										// found one
										// or sort by id
										minDistThread = dist;
										minDirThread = dir;
										foundEdgeThread = e;
									}
								} else if (minDirThread == DIRECTION_LEFT
										&& e.id.compareTo(foundEdgeThread.id) > 0) {
									// the point is on the left; sort by id if the initially found was on the left
									// side, too
									minDistThread = dist;
									minDirThread = dir;
									foundEdgeThread = e;
								}
							}
							return true;
						}
					}, (float)minDistThread);
					
					Coordinate coord = new Coordinate(pivot.x, pivot.y, 0);
					double posAtEdge = GeomHelper.getDistanceOnLineString(foundEdgeThread, coord, opivot);
					// alter the edge synchronized
					if (addToEdge) {
						synchronized (foundEdgeThread) {
							foundEdgeThread.addMappedObject(c);
						}
					}
					MapResult result = new MapResult(c, foundEdgeThread, minDistThread, posAtEdge);
					// store values synchronized
					synchronized (nearestEdges) {
						Vector<MapResult> ress = nearestEdges.get(foundEdgeThread);
						// properly computed, yet
						if (ress ==null ) {
							ress = new Vector<>();
							nearestEdges.put(foundEdgeThread, ress);
						}
						ress.add(result);
					}
				}
			} while (c != null);
		}
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
	 * @brief Returns the direction (left/right) into which the opivot point lies in respect to the edge
	 * @param e The edge
	 * @param opivot The reference opivot point
	 * @return The direction of the opivot point
	 */
	private int getDirectionToPoint(DBEdge e, com.vividsolutions.jts.geom.Point opivot) {
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

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
package de.dlr.ivf.urmo.router.algorithms.routing;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.gtfs.GTFSConnection;
import de.dlr.ivf.urmo.router.gtfs.GTFSEdge;
import de.dlr.ivf.urmo.router.gtfs.GTFSStop;
import de.dlr.ivf.urmo.router.gtfs.GTFSTrip;
import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNode;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @brief A 1-to-many Dijkstra that may be bound by some values
 * @author Daniel Krajzewicz
 * @todo Check which parameter should be included in the constructor and which in the run method
 */
public class BoundDijkstra {
	/// @brief The origin of routing
	private MapResult origin;
	/// @brief Sum of seen destination weights
	private double seenVar = 0;
	/// @brief Number of destinations to find (-1 if not used)
	private int boundNumber;
	/// @brief Maximum travel time (-1 if not used)
	private double boundTT;
	/// @brief Maximum distance (-1 if not used)
	private double boundDist;
	/// @brief Maximum weight sum to find (-1 if not used)
	private double boundVar;
	/// @brief Whether only the next item shall be found
	private boolean shortestOnly;
	/// @brief Starting time
	private int time;
	/// @brief Map of seen destinations to the paths to them
	private Map<EdgeMappable, SingleODResult> seen = new HashMap<>();
	/// @brief The route weighting function to use
	private AbstractRouteWeightFunction measure = null;
	/// @brief The priority queue holding the next elements to process
	private PriorityQueue<DijkstraEntry> next = null;
	/// @brief The information about node access
	private HashMap<DBNode, HashMap<Long, DijkstraEntry> > nodeMap = new HashMap<DBNode, HashMap<Long, DijkstraEntry>>();
	/// @brief Infomration about visited edges
	private Map<DBEdge, DijkstraEntry> edgeMap = new HashMap<>();
	
	
	/** @brief Constructor
	 * @param _measure The route weighting function to use
	 * @param _origin The origin of routing
	 * @param _boundNumber Number of destinations to find (-1 if not used)
	 * @param _boundTT Maximum travel time (-1 if not used)
	 * @param _boundDist Maximum distance (-1 if not used)
	 * @param _boundVar Maximum weight sum to find (-1 if not used)
	 * @param _shortestOnly Whether only the next item shall be found
	 * @param _time Starting time
	 */
	public BoundDijkstra(AbstractRouteWeightFunction _measure, MapResult _origin, int _boundNumber, double _boundTT, double _boundDist, 
			double _boundVar, boolean _shortestOnly, int _time) {
		origin = _origin;
		boundNumber = _boundNumber;
		boundTT = _boundTT;
		boundDist = _boundDist;
		boundVar = _boundVar;
		shortestOnly = _shortestOnly;
		time = _time;
		measure = _measure;
		next = new PriorityQueue<DijkstraEntry>(1000, _measure);
	}

	
	
	/**
	 * @brief Computes a bound 1-to-many shortest paths using the Dijkstra algorithm
	 * 
	 * @param usedModeID The first used mode
	 * @param modes Bitset of usable transport modes
	 * @param ends A set of all destinations
	 * @param nearestFromEdges The map of edges destinations are located at to the destinations
	 */
	public void run(long usedModeID, long modes,  Set<DBEdge> ends, HashMap<DBEdge, Vector<MapResult>> nearestFromEdges) {
		boolean hadExtension = false;
		long availableModes = modes;
		Mode usedMode = Modes.getMode(usedModeID);
		DBEdge startEdge = origin.edge;
		double tt = startEdge.getTravelTime(usedMode.vmax, time) * (startEdge.getLength()-origin.pos) / startEdge.getLength();
		DijkstraEntry nm = new DijkstraEntry(measure, null, startEdge.getToNode(), startEdge, availableModes, usedMode,
				(startEdge.getLength()-origin.pos), tt, null, tt, 0, false);
		next.add(nm);
		addNodeInfo(startEdge.getToNode(), availableModes, nm);
		if(visitFirstEdge(measure, startEdge, nm, nearestFromEdges, false)) {
			hadExtension = true; // there won't be a better way
		} 
		// consider starting in the opposite direction
		if(startEdge.getOppositeEdge()!=null && startEdge.getOppositeEdge().allows(usedMode)) {
			DBEdge e = startEdge.getOppositeEdge();
			tt = e.getTravelTime(usedMode.vmax, time) * (origin.pos) / e.getLength();
			nm = new DijkstraEntry(measure, null, e.getToNode(), e, availableModes, usedMode, (origin.pos), tt, null, tt, 0, true);
			next.add(nm);
			addNodeInfo(e.getToNode(), availableModes, nm);
			if(visitFirstEdge(measure, e, nm, nearestFromEdges, true)) {
				if(!hadExtension) {
					hadExtension = true; // there won't be a better way
				}
			}
		}
		
		while (!next.isEmpty()) {
			DijkstraEntry nns = next.poll();
			// check bounds
			if (boundTT > 0 && nns.tt >= boundTT) {
				continue;
			}
			if (boundDist > 0 && nns.distance >= boundDist) {
				continue;
			}
			Vector<DBEdge> oes = nns.n.getOutgoing();
			for (DBEdge oe : oes) {
				availableModes = nns.availableModes;
				usedMode = nns.usedMode;
				if(!oe.allowsAny(availableModes)) { 
					continue;
				}
				double interchangeTT = 0;
				if (!oe.allows(usedMode)) {
					// todo: pt should entrain bikes, foot, etc. be put on top of this
					// (check also computation of the current line in outputs)
					availableModes = availableModes & oe.getModes();
					if(availableModes==0) {
						continue; // no modes left
					}
					usedMode = Modes.selectModeFrom(availableModes);
					// @todo: what is an inter-mode interchange time? interchangeTT = 0;
				}
				GTFSConnection ptConnection = null;
				double ttt;
				if(oe.isGTFSEdge()) {
					GTFSEdge ge = (GTFSEdge) oe;
					// @todo: this is not correct, the interchange should be regarded here, not in the ttt computation below
					// @todo: how to select the trip continuation first?
					ptConnection = ge.getConnection(time + nns.tt);
					if(ptConnection==null) {
						// @todo: what about connections during the next day?
						continue; // no valid pt connection
					} else {
						GTFSTrip prevTrip = nns.ptConnection!=null ? nns.ptConnection.trip : null;
						if(!ptConnection.trip.equals(prevTrip)) {
							interchangeTT = ((GTFSStop) nns.n).getInterchangeTime(ptConnection.trip, prevTrip, 0);
						}
						ttt = ptConnection.arrivalTime - time - nns.tt + interchangeTT;
					}
				} else {
					ttt = oe.getTravelTime(usedMode.vmax, time + nns.tt) + interchangeTT;
					// @todo: interchange times at nodes
				}
				DBNode n = oe.getToNode();
				double distance = nns.distance + oe.getLength();
				tt = nns.tt + ttt;
				DijkstraEntry oldValue = getPriorNodeInfo(n, availableModes);
				DijkstraEntry newValue = new DijkstraEntry(measure, nns, n, oe, availableModes, usedMode, distance, tt, ptConnection, ttt, interchangeTT, false);
				if(oldValue==null) {
					next.add(newValue);
					addNodeInfo(n, availableModes, newValue);
				} else if(measure.compare(oldValue, newValue)>0) {
					next.remove(oldValue);
					next.add(newValue);
					addNodeInfo(n, availableModes, newValue);
				}
				if(visitEdge(measure, oe, newValue, nearestFromEdges)) {
					if(!hadExtension) {
						boundTT = Math.max(boundTT, tt+newValue.first.ttt+ttt); // !!! probably false, use topology in combination with maximum travel time (or measure)
						hadExtension = true;
					}
				}
				
				// check opposite direction
				if(oe.getOppositeEdge()!=null && oe.getOppositeEdge().getAttachedObjectsNumber()!=0) {
					DijkstraEntry newOppositeValue = new DijkstraEntry(measure, nns, n, oe.getOppositeEdge(), availableModes, usedMode, distance, tt, ptConnection, ttt, interchangeTT, true);
					if(visitEdge(measure, oe.getOppositeEdge(), newOppositeValue, nearestFromEdges)) {
						if(!hadExtension) {
							boundTT = Math.max(boundTT, tt+newOppositeValue.first.ttt+ttt); // !!! probably false, use topology in combination with maximum travel time (or measure)
							hadExtension = true;
						}
					}
				}
				
			}
		}
	}
	

	/** @brief Adds the information about the access to a node
	 * 
	 * @param nodeMap The information storage
	 * @param node The node to add the information about
	 * @param availableModes The still available modes
	 * @param m The path to the node
	 */
	public void addNodeInfo(DBNode node, long availableModes, DijkstraEntry m) {
		if(!nodeMap.containsKey(node)) {
			nodeMap.put(node, new HashMap<Long, DijkstraEntry>());
		}
		HashMap<Long, DijkstraEntry> nodeVals = nodeMap.get(node);
		nodeVals.put(availableModes, m);
	}
	
	
	/** @brief Returns the information about a previously visited node
	 * @param node The accessed node
	 * @param availableModes The still available modes
	 * @return The prior node used to access the given one using the given modes
	 */
	public DijkstraEntry getPriorNodeInfo(DBNode node, long availableModes) {
		if(!nodeMap.containsKey(node)) {
			return null;
		}
		HashMap<Long, DijkstraEntry> nodeVals = nodeMap.get(node);
		if(!nodeVals.containsKey(availableModes)) {
			return null;
		}
		return nodeVals.get(availableModes); 
	}


	/** @brief Adds the information about the first edge
	 * 
	 * @param measure The routing weight function to use
	 * @param oe The accessed edge
	 * @param newValue The routing element used to approach the edge
	 * @return Whether all needed destinations were found
	 */
	public boolean visitFirstEdge(AbstractRouteWeightFunction measure, DBEdge oe, DijkstraEntry newValue, HashMap<DBEdge, Vector<MapResult>> nearestToEdges, boolean isOpposite) {
		// check only edges that have attached destinations
		if(oe.getAttachedObjectsNumber()==0) {
			return false;
		}
		Vector<MapResult> toObjects = nearestToEdges.get(oe);
		for(MapResult mr : toObjects) {
			LayerObject lo = (LayerObject) mr.em;
			SingleODResult path = new SingleODResult(origin, mr, newValue, time);
			if(!seen.containsKey(lo)) {
				seen.put(lo, path);
				seenVar += lo.getAttachedValue();
			} else if(seen.get(lo).tt>path.tt) {
				seen.put(lo, path);
			}
		}
		if (shortestOnly&&seen.size()>0) {
			return true;
		}
		if (boundNumber > 0 && seen.size() >= boundNumber) {
			return true;
		}
		if (boundVar > 0 && seenVar >= boundVar) {
			return true;
		}
		return false;
	}



	/** @brief Adds the information about an accessed edge
	 * 
	 * For the first edge and its opposite edge, it performs a comparison for the positions --> visitFirstEdge
	 * 
	 * @param measure The routing weight function to use
	 * @param oe The accessed edge
	 * @param newValue The routing element used to approach the edge
	 * @return Whether all needed destinations were found
	 */
	public boolean visitEdge(AbstractRouteWeightFunction measure, DBEdge oe, DijkstraEntry newValue, HashMap<DBEdge, Vector<MapResult>> nearestToEdges) {
		// check only edges that have attached destinations
		if(oe.getAttachedObjectsNumber()==0) {
			return false;
		}
		// add the way to this edge if it's the first or the best one
		boolean isUpdate = edgeMap.containsKey(oe);
		if(!isUpdate || measure.compare(edgeMap.get(oe), newValue)>0) { // !!! on an edge base? 
			edgeMap.put(oe, newValue);
			Vector<MapResult> toObjects = nearestToEdges.get(oe);
			for(MapResult mr : toObjects) {
				LayerObject lo = (LayerObject) mr.em;
				SingleODResult path = new SingleODResult(origin, mr, newValue, time);
				if(!seen.containsKey(lo)) {
					seen.put(lo, path);
					seenVar += lo.getAttachedValue();
				} else if(seen.get(lo).tt>path.tt) {
					seen.put(lo, path);
				}
			}
		}
		if (shortestOnly&&seen.size()>0) {
			return true;
		}
		// nope, we have seen the wanted number of elements
		if (boundNumber > 0 && seen.size() >= boundNumber) {
			return true;
		}
		// nope, we have seen the number of values to find
		if (boundVar > 0 && seenVar >= boundVar) {
			return true;
		}
		return false;
	}

	
	/** @brief Returns the path to the given destination
	 * 
	 * @param to The destination to get the path to
	 * @return The path to the given destination
	 */
	public SingleODResult getResult(EdgeMappable to) {
		return seen.get(to);
	}
	

	/** @brief Returns the seen destinations
	 * 
	 * @return All seen destinations
	 * @todo Refactor - return ODResults
	 */
	public Vector<EdgeMappable> getSeenDestinations() {
		Vector<EdgeMappable> ret = new Vector<>();
		ret.addAll(seen.keySet());
		Collections.sort(ret, new Comparator<EdgeMappable>() {
			@Override
			public int compare(EdgeMappable o1, EdgeMappable o2) {
				long id1 = o1.getOuterID();
				long id2 = o2.getOuterID();
				return id1 < id2 ? -1 : id1 > id2 ? 1 : 0;
			}
		});
		return ret;
	}

}

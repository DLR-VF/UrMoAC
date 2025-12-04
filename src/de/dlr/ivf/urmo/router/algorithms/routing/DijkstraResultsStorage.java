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
package de.dlr.ivf.urmo.router.algorithms.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.output.SingleResultComparator_DestinationID;
import de.dlr.ivf.urmo.router.output.SingleResultComparator_TT;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/** @class DijkstraResultsStorage
 * @brief A storage for Dijkstra results.
 * 
 * This is a base class with two variants. @see DijkstraResultsStorage_SingleType assumes only
 * one type of destinations, while @see DijkstraResultsStorage_MultiType stores the results
 * assuming different types of destination exist.
 * @author Daniel Krajzewicz
 */
public abstract class DijkstraResultsStorage {
	/// @brief Number of destinations to find (-1 if not used)
	protected int boundNumber;
	/// @brief Maximum travel time (-1 if not used)
	protected double boundTT;
	/// @brief Maximum distance (-1 if not used)
	protected double boundDist;
	/// @brief Maximum weight sum to find (-1 if not used)
	protected double boundVar;
	/// @brief Whether only the next item shall be found
	protected boolean shortestOnly;

	
	/** @class ResultSet
	 * Stores the shortest path between an origin and the destination.
	 */
	public class ResultSet {
		/// @brief Sum of seen destination weights
		protected double seenVar = 0;
		/// @brief Map of seen destinations to the paths to them
		protected Map<EdgeMappable, SingleODResult> seen = new HashMap<>();
		
		
		/** @brief Constructor
		 */
		public ResultSet() {
		}
		
		
		/** @brief Adds the result for a single destination
		 * 
		 * @param lo The destination
		 * @param path The path to the destination
		 */
		public void _addResult(LayerObject lo, SingleODResult path) {
			if(!seen.containsKey(lo)) {
				seen.put(lo, path);
				seenVar += lo.getAttachedValue();
			} else if(seen.get(lo).tt>path.tt) {
				seen.put(lo, path);
			}
		}

		
		/** @brief Returns whether the search is completed
		 * 
		 * @return Whether the limits have been reached
		 */
		public boolean _finished() {
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
		
		
		/** @brief Returns found results
		 * 
		 * @param comparator The comparator used to sort the results
		 * @param needsPT Whether only paths that contain public transport shall be returned
		 * @param singleDestination The destination to find explicitly
		 * @return The list of found results
		 */
		public Vector<SingleODResult> getResults(SingleResultComparator_TT comparator, boolean needsPT, long singleDestination) {
			Vector<SingleODResult> results = new Vector<>();
			for(EdgeMappable destination : seen.keySet()) {
				SingleODResult destPath = seen.get(destination);
				if(destPath.origin.edge==destPath.destination.edge&&destPath.origin.edge.getOppositeEdge()==null&&destPath.origin.pos>destPath.destination.pos) {
					// !!! skip those that are on the same edge but behind the origin
					continue;
				}
				if(!destPath.matchesRequirements(needsPT)) {
					continue;
				}
				if(singleDestination<0||destination.getOuterID()==singleDestination) {
					results.add(destPath);
				}
			}
			return results;
		}
		
	}
	
	

	/** @brief Constructor
	 * 
	 * @param _boundNumber The maximum number of destinations to find
	 * @param _boundTT The maximum travel time bounds
	 * @param _boundDist The maximum distance bounds
	 * @param _boundVar The maximum value bounds
	 * @param _shortestOnly Whether only the nearest destination shall be found
	 */
	public DijkstraResultsStorage(int _boundNumber, double _boundTT, double _boundDist, 
			double _boundVar, boolean _shortestOnly) {
		boundNumber = _boundNumber;
		boundTT = _boundTT;
		boundDist = _boundDist;
		boundVar = _boundVar;
		shortestOnly = _shortestOnly;
	}


	/** @brief Revisits results and returns only valid ones
	 *  
	 * @param results The results to recheck
	 * @return List of valid results
	 */
	protected Vector<SingleODResult> filterResults(Vector<SingleODResult> results) {
		Vector<SingleODResult> nresults = new Vector<>();
		double var = 0;
		int num = 0;
		for(SingleODResult result : results) {
			if(boundTT>0&&result.tt>boundTT) {
				continue;
			}
			if(boundDist>0&&result.dist>boundDist) {
				continue;
			}
			nresults.add(result);
			if(shortestOnly) {
				break;
			}
			num += 1;
			var += ((LayerObject) result.destination.em).getAttachedValue();
			if(boundNumber>0&&num>=boundNumber) {
				break;
			}
			if(boundVar>0&&var>=boundVar) {
				break;
			}
		}
		return nresults;				
	}
	
	
	/** @brief Adds a result
	 * 
	 * @param lo The destination to add
	 * @param path The path to the destination
	 */
	public abstract void addResult(LayerObject lo, SingleODResult path);
	

	/** @brief Returns whether the search can be finished
	 * @return Whether all destinations were found
	 */
	public abstract boolean finished();
	
	
	/** @brief Collects the results
	 * 
	 * @param comparator The comparator used to sort the results
	 * @param sorter The sorting comparator
	 * @param needsPT Whether only paths that contain public transport shall be returned
	 * @param singleDestination The destination to find explicitly
	 * @return The sorted list of found destinations
	 */
	public abstract Vector<SingleODResult> collectResults(SingleResultComparator_TT comparator, SingleResultComparator_DestinationID sorter, boolean needsPT, long singleDestination);


	
}

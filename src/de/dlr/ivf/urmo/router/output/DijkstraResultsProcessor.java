/*
 * Copyright (c) 2017-2024
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
package de.dlr.ivf.urmo.router.output;

import java.io.IOException;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.IBoundDijkstra;
import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class DijkstraResultsProcessor
 * @brief Processes computed paths by generating measures and writing them
 * @author Daniel Krajzewicz
 * @todo What about the sorter - why static, what about other weighting functions?
 */
public class DijkstraResultsProcessor {
	/// @brief The aggregators to use
	@SuppressWarnings("rawtypes")
	private Vector<Aggregator> aggs;
	/// @brief The results comparator to use
	private SingleResultComparator_TT comparator = new SingleResultComparator_TT();
	/// @brief Sorts results by the ID of the destination
	private SingleResultComparator_DestinationID sorter = new SingleResultComparator_DestinationID();
	/// @brief An optional direct output
	private DirectWriter directWriter;
	/// @brief The begin time of route computation
	private int beginTime;
	/// @brief The maximum number of destinations to find
	private int maxNumber;
	/// @brief The maximum travel time to use
	private double maxTT;
	/// @brief The maximum distance to pass
	private double maxDistance;
	/// @brief The maximum value to collect
	private double maxVar;
	/// @brief Whether only the shortest connection shall be found 
	private boolean shortestOnly;
	/// @brief Whether only paths with an public transport line shall be reported 
	private boolean needsPT;
	
		
	/**
	 * @brief Constructor
	 * @param _beginTime The begin time of route computation
	 * @param _dw An optional direct output
	 * @param _aggs The aggregators to use
	 * @param _maxNumber The maximum number of destinations to find
	 * @param _maxTT The maximum travel time to use
	 * @param _maxDistance The maximum distance to pass
	 * @param _maxVar The maximum value to collect
	 * @param _shortestOnly Whether only the shortest connection shall be found 
	 * @param _needsPT Whether only paths with an public transport line shall be reported 
	 */
	public DijkstraResultsProcessor(int _beginTime, DirectWriter _dw, @SuppressWarnings("rawtypes") Vector<Aggregator> _aggs,
			int _maxNumber, double _maxTT, double _maxDistance, double _maxVar, boolean _shortestOnly, boolean _needsPT) {
		aggs = _aggs;
		directWriter = _dw;
		beginTime = _beginTime;
		maxNumber = _maxNumber;
		maxTT = _maxTT;
		maxDistance = _maxDistance;
		maxVar = _maxVar;
		shortestOnly = _shortestOnly;
		needsPT = _needsPT;
	}
	
	
	/**
	 * @brief Processes a single result
	 * @param mr The origin result
	 * @param dr The path to process
	 * @param singleDestination If >0 only this destination shall be regarded
	 * @throws IOException When something fails
	 */
	public void process(MapResult mr, IBoundDijkstra dr, long singleDestination) throws IOException {
		Vector<SingleODResult> results = new Vector<>();
		for(EdgeMappable destination : dr.getSeenDestinations()) {
			SingleODResult destPath = dr.getResult(destination);
			if(destPath.origin.edge==destPath.destination.edge&&destPath.origin.edge.getOppositeEdge()==null&&destPath.origin.pos>destPath.destination.pos) {
				continue;
			}
			if(!destPath.matchesRequirements(needsPT)) {
				continue;
			}
			if(singleDestination<0||destination.getOuterID()==singleDestination) {
				results.add(destPath);
			}
		}
		results.sort(comparator);
		Vector<SingleODResult> nresults = new Vector<>();
		double var = 0;
		int num = 0;
		for(SingleODResult result : results) {
			if(maxTT>0&&result.tt>maxTT) {
				continue;
			}
			if(maxDistance>0&&result.dist>maxDistance) {
				continue;
			}
			nresults.add(result);
			if(shortestOnly) {
				break;
			}
			num += 1;
			var += ((LayerObject) result.destination.em).getAttachedValue();
			if(maxNumber>0&&num>=maxNumber) {
				break;
			}
			if(maxVar>0&&var>=maxVar) {
				break;
			}
		}
		nresults.sort(sorter);
		
		// multiple origins and multiple destinations
		for(SingleODResult result : nresults) {
			for(@SuppressWarnings("rawtypes") Aggregator agg : aggs) {
				agg.add(beginTime, result);
			}
			if(directWriter!=null) {
				directWriter.writeResult(result, beginTime);
			}
		}
		for(@SuppressWarnings("rawtypes") Aggregator agg : aggs) {
			agg.endOrigin(mr.em.getOuterID());
		}
	}


	/**
	 * @brief Finishes the processing
	 */
	public void finish() throws IOException {
		if(directWriter!=null) {
			directWriter.close();
		}
		for(@SuppressWarnings("rawtypes") Aggregator agg : aggs) {
			agg.finish();
		}
	}
	
	
}

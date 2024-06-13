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
import java.util.HashMap;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class DijkstraResultsProcessor
 * @brief Processes computed paths by generating measures and writing them
 * @author Daniel Krajzewicz
 */
public class DijkstraResultsProcessor {
	/// @brief A mapping from an edge to allocated sources
	HashMap<DBEdge, Vector<MapResult>> nearestFromEdges;
	/// @brief A mapping from an edge to allocated destinations
	HashMap<DBEdge, Vector<MapResult>> nearestToEdges;
	/// @brief The aggregators to use
	@SuppressWarnings("rawtypes")
	public Vector<Aggregator> aggs;
	/// @brief The results comparator to use
	public SingleResultComparator_TT comparator = new SingleResultComparator_TT();
	/// 
	public SingleResultComparator_DestinationID sorter = new SingleResultComparator_DestinationID();
	/// @brief An optional direct output
	DirectWriter directWriter;
	/// @brief The begin time of route computation
	int beginTime;
	

	/**
	 * @brief Constructor
	 * @param _beginTime The begin time of route computation
	 * @param dw An optional direct output
	 * @param _aggs The aggregators to use
	 * @param _nearestFromEdges A mapping from an edge to allocated sources
	 * @param _nearestToEdges A mapping from an edge to allocated destinations
	 */
	public DijkstraResultsProcessor(int _beginTime, DirectWriter dw, @SuppressWarnings("rawtypes") Vector<Aggregator> _aggs,
			HashMap<DBEdge, Vector<MapResult>> _nearestFromEdges, HashMap<DBEdge, Vector<MapResult>> _nearestToEdges) {
		aggs = _aggs;
		nearestFromEdges = _nearestFromEdges;
		nearestToEdges = _nearestToEdges;
		directWriter = dw;
		beginTime = _beginTime;
	}
	
	
	/**
	 * @brief Processes a single result
	 * @param mr The origin result
	 * @param dr The path to process
	 * @param needsPT Whether only entries that contain a public transport path shall be processed
	 * @param singleDestination If >0 only this destination shall be regarded
	 * @throws IOException When something fails
	 */
	public void process(MapResult mr, DijkstraResult dr, boolean needsPT, long singleDestination) throws IOException {
		Vector<SingleODResult> results = new Vector<>();
		for(MapResult destination : dr.getSeenDestinations()) {
			SingleODResult destPath = dr.getResult(destination);
			if(destPath.origin.edge==destPath.destination.edge&&destPath.origin.edge.getOppositeEdge()==null&&destPath.origin.pos>destPath.destination.pos) {
				continue;
			}
			if(!destPath.matchesRequirements(needsPT)) {
				continue;
			}
			if(singleDestination<0||destination.em.getOuterID()==singleDestination) {
				results.add(destPath);
			}
		}
		results.sort(comparator);
		Vector<SingleODResult> nresults = new Vector<>();
		double var = 0;
		int num = 0;
		for(SingleODResult result : results) {
			if(dr.boundTT>0&&result.tt>dr.boundTT) {
				continue;
			}
			if(dr.boundDist>0&&result.dist>dr.boundDist) {
				continue;
			}
			nresults.add(result);
			if(dr.shortestOnly) {
				break;
			}
			num += 1;
			var += ((LayerObject) result.destination.em).getAttachedValue();
			if(dr.boundNumber>0&&num>=dr.boundNumber) {
				break;
			}
			if(dr.boundVar>0&&var>=dr.boundVar) {
				break;
			}
		}
		nresults.sort(sorter);
		
		// multiple sources and multiple destinations
		for(SingleODResult result : nresults) {
			for(@SuppressWarnings("rawtypes") Aggregator agg : aggs) {
				agg.add(beginTime, result);
			}
			if(directWriter!=null) {
				directWriter.writeResult(result.origin.em.getOuterID(), result.destination.em.getOuterID(), result.path);
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

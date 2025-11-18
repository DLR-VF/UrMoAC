/*
 * Copyright (c) 2017-2025
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

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResultsStorage;
import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;

/**
 * @class ResultsProcessor
 * @brief Processes computed paths by generating measures and writing them
 * @author Daniel Krajzewicz
 * @todo What about the sorter - why static, what about other weighting functions?
 */
public class ResultsProcessor {
	/// @brief The aggregators to use
	@SuppressWarnings("rawtypes")
	private Vector<AggregatorBase> aggs;
	/// @brief The results comparator to use
	private SingleResultComparator_TT comparator = new SingleResultComparator_TT();
	/// @brief Sorts results by the ID of the destination
	private SingleResultComparator_DestinationID sorter = new SingleResultComparator_DestinationID();
	/// @brief An optional direct output
	private DirectWriter directWriter;
	/// @brief An optional process writer
	private ProcessWriter processWriter;
	/// @brief The begin time of route computation
	private int beginTime;
	/// @brief Whether only paths with an public transport line shall be reported 
	private boolean needsPT;
	
		
	/**
	 * @brief Constructor
	 * @param _beginTime The begin time of route computation
	 * @param _directWriter An optional direct output
	 * @param _processWriter A writer for computation stats
	 * @param _aggs The aggregators to use
	 * @param _needsPT Whether only paths with an public transport line shall be reported 
	 */
	public ResultsProcessor(int _beginTime, DirectWriter _directWriter, ProcessWriter _processWriter, @SuppressWarnings("rawtypes") Vector<AggregatorBase> _aggs,
			boolean _needsPT) {
		aggs = _aggs;
		directWriter = _directWriter;
		processWriter = _processWriter;
		beginTime = _beginTime;
		needsPT = _needsPT;
	}
	
	
	/**
	 * @brief Processes a single result
	 * @param beg The begin time of the routing
	 * @param numSeenEdges The number of seen edges with destinations
	 * @param numSeenNodes The number of seen nodes
	 * @param mr The origin
	 * @param drs The results storage
	 * @param singleDestination If >0 only this destination shall be regarded
	 * @throws IOException When something fails
	 */
	public void process(long beg, long numSeenEdges, long numSeenNodes, MapResult mr, DijkstraResultsStorage drs, long singleDestination) throws IOException {
		Vector<SingleODResult> results = drs.collectResults(comparator, sorter, needsPT, singleDestination);
		// multiple origins and multiple destinations
		for(SingleODResult result : results) {
			for(@SuppressWarnings("rawtypes") AggregatorBase agg : aggs) {
				agg.add(beginTime, result);
			}
			if(directWriter!=null) {
				directWriter.writeResult(result, beginTime);
			}
			if(processWriter!=null) {
				processWriter.write(beg, numSeenEdges, numSeenNodes, mr, result);
			}
		}
		for(@SuppressWarnings("rawtypes") AggregatorBase agg : aggs) {
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
		if(processWriter!=null) {
			processWriter.close();
		}
		for(@SuppressWarnings("rawtypes") AggregatorBase agg : aggs) {
			agg.finish();
		}
	}
	
	
}

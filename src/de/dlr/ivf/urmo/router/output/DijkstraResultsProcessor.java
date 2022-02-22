/*
 * Copyright (c) 2016-2022 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * Licensed under the GNU General Public License v3.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.output;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraEntry;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.shapes.DBEdge;

/**
 * @class DijkstraResultsProcessor
 * @brief Processes computed paths by generating measures and writing them
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class DijkstraResultsProcessor {
	/// @brief A mapping from an edge to allocated sources
	HashMap<DBEdge, Vector<MapResult>> nearestFromEdges;
	/// @brief A mapping from an edge to allocated destinations
	HashMap<DBEdge, Vector<MapResult>> nearestToEdges;
	/// @brief The aggregators to use
	public Vector<Aggregator> aggs;
	/// @brief The results comparator to use
	public AbstractSingleResultComparator_TT comparator = new AbstractSingleResultComparator_TT();
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
	public DijkstraResultsProcessor(int _beginTime, DirectWriter dw, Vector<Aggregator> _aggs,
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
		if(directWriter!=null) {
			directWriter.writeResult(dr, mr, needsPT, singleDestination);
		}
		// multiple sources and multiple destinations
		for(Aggregator agg : aggs) {
			Vector<AbstractSingleResult> results = new Vector<>();
			for(DBEdge destEdge : dr.edgeMap.keySet()) {
				DijkstraEntry toEdgeEntry = dr.getEdgeInfo(destEdge);
				if(!toEdgeEntry.matchesRequirements(needsPT)) {
					continue;
				}
				Vector<MapResult> toObjects = nearestToEdges.get(destEdge);
				if(toObjects!=null) {
					for(MapResult toObject : toObjects) {
						if(singleDestination<0||toObject.em.getOuterID()==singleDestination) {
							AbstractSingleResult result = agg.parent.buildResult(beginTime, mr, toObject, dr);
							results.add(result);
						}
					}
				}
			}
			results.sort(comparator);
			double var = 0;
			int num = 0;
			for(AbstractSingleResult result : results) {
				if(dr.boundTT>0&&result.tt>dr.boundTT) {
					continue;
				}
				if(dr.boundDist>0&&result.dist>dr.boundDist) {
					continue;
				}
				agg.add(result);
				if(dr.shortestOnly) {
					break;
				}
				num += 1;
				var += result.val;
				if(dr.boundNumber>0&&num>=dr.boundNumber) {
					break;
				}
				if(dr.boundVar>0&&var>=dr.boundVar) {
					break;
				}
			}
		}
	}


	/**
	 * @brief Finishes the processing
	 */
	public void finish() throws IOException {
		if(directWriter!=null) {
			directWriter.close();
		}
		for(Aggregator agg : aggs) {
			agg.finish();
		}
	}
	
	
}

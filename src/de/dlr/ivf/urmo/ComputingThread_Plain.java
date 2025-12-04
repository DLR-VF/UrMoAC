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
package de.dlr.ivf.urmo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.AbstractRouteWeightFunction;
import de.dlr.ivf.urmo.router.algorithms.routing.BoundDijkstra_Full;
import de.dlr.ivf.urmo.router.algorithms.routing.BoundDijkstra_UniModal;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResultsStorage;
import de.dlr.ivf.urmo.router.algorithms.routing.IBoundDijkstra;
import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.output.ResultsProcessor;
import de.dlr.ivf.urmo.router.shapes.DBEdge;

/** @class ComputingThread_Plain
 * @brief A thread which polls for new origins, computes the accessibility and
 * writes the results before asking for the next one
 * @author Daniel Krajzewicz
 */
public class ComputingThread_Plain implements Runnable {
	/// @brief The parent to get information from
	private UrMoAccessibilityComputer parent;
	/// @brief The results processor to use
	private ResultsProcessor resultsProcessor;
	/// @brief The routing measure to use
	private AbstractRouteWeightFunction measure;
	/// @brief The start time of routing
	private int time; 
	/// @brief The available transport modes
	private Vector<Mode> modes;
	/// @brief The maximum number of destinations to find
	private int boundNumber;
	/// @brief The maximum travel time to use
	private double boundTT;
	/// @brief The maximum distance to pass
	private double boundDist;
	/// @brief The maximum value to collect
	private double boundVar;
	/// @brief Whether only the shortest connection shall be found 
	private boolean shortestOnly;
	/// @brief Whether public transport schedule is loaded
	private boolean hasPT;
	/// @brief The map of destination type IDs to their names 
	private HashMap<Long, Set<String>> destTypes;

	
	/**
	 * @brief Constructor
	 * @param _parent The parent to get information from
	 * @param _measure The routing measure to use
	 * @param _resultsProcessor The results processor to use
	 * @param _time The start time of routing
	 * @param _modes The available transport modes
	 * @param _boundNumber The maximum number of destinations to find
	 * @param _boundTT The maximum travel time to use
	 * @param _boundDist The maximum distance to pass
	 * @param _boundVar The maximum value to collect
	 * @param _shortestOnly Whether only the shortest connection shall be found 
	 * @param _hasPT Whether public transport schedule is loaded
	 * @param _destTypes The map of destination type IDs to their names 
	 */
	public ComputingThread_Plain(UrMoAccessibilityComputer _parent, 
			AbstractRouteWeightFunction _measure, ResultsProcessor _resultsProcessor,
			int _time, Vector<Mode> _modes, int _boundNumber, double _boundTT, 
			double _boundDist, double _boundVar, boolean _shortestOnly, boolean _hasPT, HashMap<Long, Set<String>> _destTypes) {
		super();
		parent = _parent;
		resultsProcessor = _resultsProcessor;
		measure = _measure;
		//needsPT = _needsPT;
		time = _time; 
		modes = _modes;
		boundNumber = _boundNumber;
		boundTT = _boundTT;
		boundDist = _boundDist;
		boundVar = _boundVar;
		shortestOnly = _shortestOnly;
		hasPT = _hasPT;
		destTypes = _destTypes;
	}
	
	
	
	/**
	 * @brief Performs the computation
	 * 
	 * Iterates over edges or od-connections.
	 * Builds the paths, first, then uses them to generate the results.
	 */
	public void run() {
		try {
			DBEdge e = null;
			do {
				e = parent.getNextStartingEdge();
				if(e==null) {
					continue;
				}
				Vector<MapResult> fromObjects = parent.nearestFromEdges.get(e);
				if(modes.size()==1 && hasPT==false) {
					runUniModal(fromObjects);
				} else {
					runFull(fromObjects);
				}
			} while(e!=null&&!parent.hadError);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	private void runFull(Vector<MapResult> fromObjects) throws IOException {
		for(MapResult mr : fromObjects) {
			try {
				long beg = System.nanoTime();
				IBoundDijkstra bd = new BoundDijkstra_Full(modes, measure, mr, boundNumber, boundTT, boundDist, boundVar, shortestOnly, time, destTypes);
				DijkstraResultsStorage drs = bd.run(parent.nearestToEdges.keySet(), parent.nearestToEdges);
				resultsProcessor.process(beg, bd.getSeenEdgesNum(), bd.getSeenNodesNum(), mr, drs, -1);
			} catch(java.lang.OutOfMemoryError e2) {
				System.out.println("Out of memory while processing '" + mr.em.getOuterID() + "'.");
			}
		}
	}


	private void runUniModal(Vector<MapResult> fromObjects) throws IOException {
		for(MapResult mr : fromObjects) {
			try {
				long beg = System.nanoTime();
				IBoundDijkstra bd = new BoundDijkstra_UniModal(modes.get(0), measure, mr, boundNumber, boundTT, boundDist, boundVar, shortestOnly, time, destTypes);
				DijkstraResultsStorage drs = bd.run(parent.nearestToEdges.keySet(), parent.nearestToEdges);
				resultsProcessor.process(beg, bd.getSeenEdgesNum(), bd.getSeenNodesNum(), mr, drs, -1);
			} catch(java.lang.OutOfMemoryError e2) {
				System.out.println("Out of memory while processing '" + mr.em.getOuterID() + "'.");
			}
		}
	}
	
	
	
	
	
}
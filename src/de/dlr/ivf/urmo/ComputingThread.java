package de.dlr.ivf.urmo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.AbstractRouteWeightFunction;
import de.dlr.ivf.urmo.router.algorithms.routing.BoundDijkstra;
import de.dlr.ivf.urmo.router.output.DijkstraResultsProcessor;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBODRelationExt;

/** @class ComputingThread
 * 
 * A thread which polls for new sources, computes the accessibility and
 * writes the results before asking for the next one
 * @author Daniel Krajzewicz
 * @todo refactor - od-connections and usual use cases should use own according computation classes
 */
public class ComputingThread implements Runnable {
	/// @brief The parent to get information from
	private UrMoAccessibilityComputer parent;
	/// @brief The results processor to use
	private DijkstraResultsProcessor resultsProcessor;
	/// @brief The routing measure to use
	private AbstractRouteWeightFunction measure;
	/// @brief The start time of routing
	private int time; 
	/// @brief The mode of transport to use at the begin
	private long initMode;
	/// @brief The available transport modes
	private long modes;
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
	
	
	/**
	 * @brief Constructor
	 * @param _parent The parent to get information from
	 * @param _measure The routing measure to use
	 * @param _resultsProcessor The results processor to use
	 * @param _time The start time of routing
	 * @param _initMode The mode of transport to use at the begin
	 * @param _modes The available transport modes
	 * @param _boundNumber The maximum number of destinations to find
	 * @param _boundTT The maximum travel time to use
	 * @param _boundDist The maximum distance to pass
	 * @param _boundVar The maximum value to collect
	 * @param _shortestOnly Whether only the shortest connection shall be found 
	 */
	public ComputingThread(UrMoAccessibilityComputer _parent, 
			AbstractRouteWeightFunction _measure, DijkstraResultsProcessor _resultsProcessor,
			int _time, long _initMode, 
			long _modes, int _boundNumber, double _boundTT, 
			double _boundDist, double _boundVar, boolean _shortestOnly) {
		super();
		parent = _parent;
		resultsProcessor = _resultsProcessor;
		measure = _measure;
		//needsPT = _needsPT;
		time = _time; 
		initMode = _initMode;
		modes = _modes;
		boundNumber = _boundNumber;
		boundTT = _boundTT;
		boundDist = _boundDist;
		boundVar = _boundVar;
		shortestOnly = _shortestOnly;
	}
	
	
	
	/**
	 * @brief Performs the computation
	 * 
	 * Iterates over edges or od-connections.
	 * Builds the paths, first, then uses them to generate the results.
	 */
	public void run() {
		try {
			if(parent.connections==null) {
				DBEdge e = null;
				do {
					e = parent.getNextStartingEdge();
					if(e==null) {
						continue;
					}
					Vector<MapResult> fromObjects = parent.nearestFromEdges.get(e);
					for(MapResult mr : fromObjects) {
						BoundDijkstra bd = new BoundDijkstra(measure, mr, boundNumber, boundTT, boundDist, boundVar, shortestOnly, time);
						bd.run(initMode, modes, parent.nearestToEdges.keySet(), parent.nearestToEdges);
						resultsProcessor.process(mr, bd, -1);
					}
				} while(e!=null&&!parent.hadError);
			} else {
				DBODRelationExt od = null;
				do {
					od = parent.getNextOD();
					if(od==null) {
						continue;
					}
					Set<DBEdge> destinations = new HashSet<>();
					destinations.add(od.toEdge);
					BoundDijkstra bd = new BoundDijkstra(measure, od.fromMR, boundNumber, boundTT, boundDist, boundVar, shortestOnly, time);
					bd.run(initMode, modes, parent.nearestToEdges.keySet(), parent.nearestToEdges);
					resultsProcessor.process(od.fromMR, bd, od.destination);
				} while(od!=null&&!parent.hadError);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
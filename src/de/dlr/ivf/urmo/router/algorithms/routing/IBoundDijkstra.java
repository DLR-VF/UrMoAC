package de.dlr.ivf.urmo.router.algorithms.routing;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.shapes.DBEdge;

public interface IBoundDijkstra {

	/**
	 * @brief Computes a bound 1-to-many shortest paths using the Dijkstra algorithm
	 * 
	 * @param usedModeID The first used mode
	 * @param modes Bitset of usable transport modes
	 * @param ends A set of all destinations
	 * @param nearestFromEdges The map of edges destinations are located at to the destinations
	 */
	void run(Set<DBEdge> ends, HashMap<DBEdge, Vector<MapResult>> nearestFromEdges);

	/** @brief Returns the path to the given destination
	 * 
	 * @param to The destination to get the path to
	 * @return The path to the given destination
	 */
	SingleODResult getResult(EdgeMappable to);

	/** @brief Returns the seen destinations
	 * 
	 * @return All seen destinations
	 * @todo Refactor - return ODResults
	 */
	Vector<EdgeMappable> getSeenDestinations();

}
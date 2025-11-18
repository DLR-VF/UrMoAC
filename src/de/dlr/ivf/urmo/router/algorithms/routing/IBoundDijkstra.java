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
import java.util.Set;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.shapes.DBEdge;

/** @class IBoundDijkstra
 * @brief The interface for threads that compute paths 
 * @see DijkstraResultsStorage
 * @author Daniel Krajzewicz
 */
public interface IBoundDijkstra {

	/**
	 * @brief Computes a bound 1-to-many shortest paths using the Dijkstra algorithm
	 * 
	 * @param ends A set of all destinations
	 * @param edges2dests The map of edges destinations are located at to the destinations
	 * @return The computed results
	 */
	DijkstraResultsStorage run(Set<DBEdge> ends, HashMap<DBEdge, Vector<MapResult>> edges2dests);


	/** @brief Returns the number of visited edges (with destinations)
	 * @return The number of seen edges with destinations
	 */
	public long getSeenEdgesNum();
	
	
	/** @brief Returns the number of visited nodes
	 * @return The number of visited nodes
	 */
	public long getSeenNodesNum();


}
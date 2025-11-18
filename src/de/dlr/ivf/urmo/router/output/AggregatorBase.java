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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.locationtech.jts.index.strtree.STRtree;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.shapes.Layer;

/**
 * @class AggregatorBase
 * @brief Aggregates results by origin / destination aggregation areas optionally
 * @author Daniel Krajzewicz
 * @param <T>
 */
public abstract class AggregatorBase<T extends AbstractSingleResult> {
	/// @brief The map from origins to respective aggregation area the lie within
	protected HashMap<Long, Long> origin2aggMap = null;
	/// @brief The map from destinations to respective aggregation area the lie within
	protected HashMap<Long, Long> dest2aggMap = null;
	/// @brief Whether only the shortestpath shall be computed (@todo: explain why it's here)
	protected boolean shortest = false;
	/// @brief Whether all origins shall be aggregated
	protected boolean sumOrigins = false;
	/// @brief Whether all destinations shall be aggregated
	protected boolean sumDestinations = false;
	/// @brief The writers to use
	protected Vector<AbstractResultsWriter<T>> writers = new Vector<>();
	/// @brief The measurements generator to use
	protected MeasurementGenerator<T> parent; 
	/// @brief The layer to retrieve all origin objects from
	protected Layer fromLayer;


	
	
	/**
	 * @brief Constructor
	 * @param _parent The measurements generator to use
	 * @param _fromLayer The layer to retrieve all origin objects from
	 * @param _shortest Whether only the shortest path shall be computed (@todo: explain why it's here)
	 */
	public AggregatorBase(MeasurementGenerator<T> _parent, Layer _fromLayer, boolean _shortest) {
		parent = _parent;
		shortest = _shortest;
		fromLayer = _fromLayer;
	}


	/**
	 * @brief Set aggregation of all origins to true
	 */
	public void sumOrigins() {
		sumOrigins = true;
	}


	/**
	 * @brief Set aggregation of all destinations to true
	 */
	public void sumDestinations() {
		sumDestinations = true;
	}


	/**
	 * @brief Builds a map of the origins aggregation
	 * @param orig The layer with origins
	 * @param origAgg The layer with origin aggregation objects
	 * @return The number of origins that could not be allocated in aggregation areas
	 */
	public int setOriginAggregation(Layer orig, Layer origAgg) {
		origin2aggMap = new HashMap<>();
		return buildAggregationMapping(orig, origAgg, origin2aggMap);
	}


	/**
	 * @brief Builds a map of the destination aggregation
	 * @param dest The layer with destinations
	 * @param destAgg The layer with destination aggregation objects
	 * @return The number of destinations that could not be allocated in aggregation areas
	 */
	public int setDestinationAggregation(Layer dest, Layer destAgg) {
		dest2aggMap = new HashMap<>();
		return buildAggregationMapping(dest, destAgg, dest2aggMap);
	}



	/**
	 * @brief Adds a writer to use
	 * @param b The writer to add
	 */
	public void addOutput(AbstractResultsWriter<T> b) {
		writers.add(b);
	}


	/**
	 * @brief Builds a map to collect aggregated measures within
	 * @param orig The layer with origins
	 * @param dest The layer with destinations
	 */
	public abstract void buildMeasurementsMap(Layer orig, Layer dest);


	/**
	 * @brief Builds an aggregation map
	 * @param orig The layer with unaggregated origins/destinations
	 * @param origAgg The aggregation geometries
	 * @param into The map to store the aggregation within
	 * @return The number of origins/destination that could not be assigned to an aggregation area
	 */
	private int buildAggregationMapping(Layer orig, Layer origAgg, HashMap<Long, Long> into) {
		// build temporary tree
		STRtree tree = new STRtree();
		for (EdgeMappable e : origAgg.getObjects()) {
			tree.insert(e.getGeometry().getEnvelopeInternal(), e);
		}
		// build look-up
		int missing = 0;
		for (EdgeMappable em : orig.getObjects()) {
			long destID = -1;
			@SuppressWarnings("rawtypes")
			List objs = tree.query(em.getEnvelope());
			for(Object o : objs) {
				EdgeMappable aggArea = (EdgeMappable) o;
				if (aggArea.getGeometry().contains(em.getPoint())) {
					destID = aggArea.getOuterID();
					break;
				}
			}
			if (destID >= 0) {
				into.put(em.getOuterID(), destID);
			} else {
				into.put(em.getOuterID(), -1l);
				++missing;
			}
		}
		return missing;
	}

	
	
	/**
	 * @brief Writes the result to the given writers
	 * @param entry The entry to write
	 * @param destType The type of the destination
	 * @throws IOException When something fails
	 */
	protected synchronized void write(T entry, String destType) throws IOException {
		for (AbstractResultsWriter<T> bw : writers) {
			bw.writeResult(entry, destType);
		}
	}
	

	/**
	 * @brief Flushes the writers
	 * @throws IOException When something fails
	 */
	public void flush() throws SQLException, IOException {
		for (AbstractResultsWriter<T> bw : writers) {
			bw.flush();
		}
	}
	
	
	/** @brief Adds a result to the aggregator
	 * 
	 * @param beginTime The begin time of routing
	 * @param od The result to add
	 * @throws IOException When something fails
	 */
	public abstract void add(int beginTime, SingleODResult od) throws IOException;
	
	
	/** @brief Ends the computation for a given origin
	 * 
	 * @param originID The ID of the origin
	 * @throws IOException When something fails
	 */
	public abstract void endOrigin(long originID) throws IOException;

	
	/**
	 * @brief Finishes writing, optionally generating normed collected measures and flushing outputs
	 * @throws IOException When something fails
	 */
	public abstract void finish() throws IOException;
	

	/**
	 * @brief Returns the ID of the aggregation area the given origin belongs to
	 * @param originID The id of the origin
	 * @return The post-aggregation id
	 */
	protected long getMappedOriginID(long originID) {
		if (sumOrigins) {
			return -1;
		} else if (origin2aggMap != null) {
			if(origin2aggMap.containsKey(originID)) {
				return origin2aggMap.get(originID);
			} else {
				return -1;
			}
		}
		return originID;
	}


	/**
	 * @brief Returns the ID of the aggregation area the given destination belongs to
	 * @param destID The id of the destination
	 * @return The post-aggregation id
	 */
	protected long getMappedDestinationID(long destID) {
		if (sumDestinations) {
			return -1;
		} else if (dest2aggMap != null) {
			return dest2aggMap.get(destID);
		}
		if (origin2aggMap != null && shortest) {
			return -1;
		}
		return destID;
	}


	
}

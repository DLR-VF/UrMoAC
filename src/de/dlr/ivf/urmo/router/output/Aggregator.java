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
 * Rutherfordstra�e 2
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
import de.dlr.ivf.urmo.router.shapes.Layer;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class Aggregator
 * @brief Aggregates results by origin / destination aggregation areas optionally
 * @author Daniel Krajzewicz
 * @param <T>
 */
public class Aggregator<T extends AbstractSingleResult> {
	/// @brief The map from origins to respective aggregation area the lie within
	private HashMap<Long, Long> origin2aggMap = null;
	/// @brief The map from destinations to respective aggregation area the lie within
	private HashMap<Long, Long> dest2aggMap = null;
	/// @brief Whether only the shortestpath shall be computed (@todo: explain why it's here)
	private boolean shortest = false;
	/// @brief Whether all origins shall be aggregated
	private boolean sumOrigins = false;
	/// @brief Whether all destinations shall be aggregated
	private boolean sumDestinations = false;
	/// @brief The map of measurements collected so far
	private HashMap<Long, HashMap<Long, T>> measurements = new HashMap<>();
	/// @brief The writers to use
	private Vector<AbstractResultsWriter<T>> writers = new Vector<>();
	/// @brief The measurements generator to use
	public MeasurementGenerator<T> parent; 
	/// @brief The layer to retrieve all source objects from
	private Layer fromLayer;


	/**
	 * @brief Constructor
	 * @param _parent The measurements generator to use
	 * @param _fromLayer The layer to retrieve all source objects from
	 * @param _shortest Whether only the shortestpath shall be computed (@todo: explain why it's here)
	 */
	public Aggregator(MeasurementGenerator<T> _parent, Layer _fromLayer, boolean _shortest) {
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
	public void buildMeasurementsMap(Layer orig, Layer dest) {
		if (origin2aggMap == null && dest2aggMap == null && !sumOrigins && !sumDestinations) {
			return;
		}
		HashMap<Long, T> destMap = new HashMap<>();
		for (EdgeMappable d : dest.getObjects()) {
			long destID = getMappedDestID(d.getOuterID());
			if(!destMap.containsKey(destID)) {
				destMap.put(destID, parent.buildEmptyEntry(-1, -1));
			}
		}
		for (EdgeMappable o : orig.getObjects()) {
			long srcID = getMappedSrcID(o.getOuterID());
			// add
			if (!measurements.containsKey(srcID)) {
				HashMap<Long, T> nDestMap = new HashMap<>();
				for(Long l : destMap.keySet()) {
					nDestMap.put(l, parent.buildEmptyEntry(srcID, l));
				}
				measurements.put(srcID, nDestMap);
			}
		}
	}


	/**
	 * @brief Builds an aggregation map
	 * @param orig The layer with unaggregated origins/destinations
	 * @param dest The aggregation geometries
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
	 * @brief Adds a result
	 * @param entry The entry to add
	 * @throws IOException When writing fails
	 */
	public void add(T entry) throws IOException {
		// no aggregation, write directly
		if (origin2aggMap == null && dest2aggMap == null && !sumOrigins && !sumDestinations) {
			write(entry);
			return;
		}
		// aggregation;
		entry.srcID = getMappedSrcID(entry.srcID);
		entry.destID = getMappedDestID(entry.destID);
		// TODO: check if we could write directly if no origin aggregation and destination=="all" add
		HashMap<Long, T> destMap = measurements.get(entry.srcID);
		destMap.get(entry.destID).addCounting(entry);
	}
	

	/**
	 * @brief Writes the result to the given writers
	 * @param entry The entry to write
	 * @throws IOException When something fails
	 */
	private synchronized void write(T entry) throws IOException {
		for (AbstractResultsWriter<T> bw : writers) {
			bw.writeResult(entry);
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
	

	/**
	 * @brief Finishes writing, optionally generating normed collected measures and flushing outputs
	 * @throws IOException When something fails
	 */
	public void finish() throws IOException {
		// check if only destinations are aggregated
		if(origin2aggMap==null && !sumOrigins) {
			for (Long srcID : measurements.keySet()) {
				HashMap<Long, T> dests = measurements.get(srcID);
				for (Long destID : dests.keySet()) {
					@SuppressWarnings("unchecked")
					T normed = (T) dests.get(destID).getNormed(1, 1);
					write(normed);
				}
			}
		}
		// otherwise
		if(origin2aggMap!=null || sumOrigins) {
			Vector<EdgeMappable> sources = fromLayer.getObjects();
			HashMap<Long, Double> weights = new HashMap<>();
			HashMap<Long, Integer> nums = new HashMap<>();
			for (EdgeMappable o : sources) {
				long aSrc = getMappedSrcID(o.getOuterID());
				if(!weights.containsKey(aSrc)) {
					weights.put(aSrc, 0.);
					nums.put(aSrc, 0);
				}
				weights.put(aSrc, weights.get(aSrc) + ((LayerObject) o).getAttachedValue());
				nums.put(aSrc, nums.get(aSrc) + 1);
			}
			for (Long srcID : measurements.keySet()) {
				// build normed results
				HashMap<Long, T> dests = measurements.get(srcID);
				for (Long destID : dests.keySet()) {
					@SuppressWarnings("unchecked")
					T normed = (T) dests.get(destID).getNormed(nums.get(srcID), weights.get(srcID));
					write(normed);
				}
			}
		}
		//
		for (AbstractResultsWriter<T> bw : writers) {
			bw.close();
		}
	}
	

	/**
	 * @brief Returns the ID of the aggregation area the given origin belongs to
	 * @param srcID The id of the origin
	 * @return The post-aggregation id
	 */
	private long getMappedSrcID(long srcID) {
		if (sumOrigins) {
			return -1;
		} else if (origin2aggMap != null) {
			if(origin2aggMap.containsKey(srcID)) {
				return origin2aggMap.get(srcID);
			} else {
				return -1;
			}
		}
		return srcID;
	}


	/**
	 * @brief Returns the ID of the aggregation area the given destination belongs to
	 * @param srcID The id of the destination
	 * @return The post-aggregation id
	 */
	private long getMappedDestID(long destID) {
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

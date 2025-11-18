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
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.shapes.Layer;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/**
 * @class Aggregator_MultiType
 * @brief Aggregates results by origin / destination aggregation areas optionally
 * @author Daniel Krajzewicz
 * @param <T>
 */
public class Aggregator_MultiType<T extends AbstractSingleResult> extends AggregatorBase<T> {
	/// @brief The map of measurements collected so far (origin -> type -> destination -> value)
	private HashMap<Long, HashMap<Long, HashMap<Long, T>>> measurements = new HashMap<>();
	/// @brief A map from destination IDs to its types
	private HashMap<Long, HashSet<Long>> dest2types = null;
	/// @brief A map from type name to type id
	private HashMap<String, Long> type2typeid = new HashMap<>();
	/// @brief A map from type id to type name
	private HashMap<Long, String> typeid2type = new HashMap<>();
	
	
	/**
	 * @brief Constructor
	 * @param _parent The measurements generator to use
	 * @param _fromLayer The layer to retrieve all origin objects from
	 * @param _shortest Whether only the shortest path shall be computed (@todo: explain why it's here)
	 * @param destTypes A map from destination ID to its types
	 */
	public Aggregator_MultiType(MeasurementGenerator<T> _parent, Layer _fromLayer, boolean _shortest, HashMap<Long, Set<String>> destTypes) {
		super(_parent, _fromLayer, _shortest);
		dest2types = new HashMap<Long, HashSet<Long>>();
		for(Long id : destTypes.keySet()) {
			dest2types.put(id, new HashSet<Long>());
			for(String type : destTypes.get(id)) {
				long nextTypeID = type2typeid.size();
				if(!type2typeid.containsKey(type)) {
					type2typeid.put(type, nextTypeID);
					typeid2type.put(nextTypeID, type);
				}
				long typeid = type2typeid.get(type);
				dest2types.get(id).add(typeid);
			}
		}
	}


	/**
	 * @brief Builds a map to collect aggregated measures within
	 * @param orig The layer with origins
	 * @param dest The layer with destinations
	 */
	public void buildMeasurementsMap(Layer orig, Layer dest) {
		if (origin2aggMap==null && dest2aggMap==null && !sumOrigins && !sumDestinations) {
			return;
		}
		HashMap<Long, T> destMap = new HashMap<>();
		for (EdgeMappable d : dest.getObjects()) {
			long destID = getMappedDestinationID(d.getOuterID());
			if(!destMap.containsKey(destID)) {
				destMap.put(destID, parent.buildEmptyEntry(-1, -1));
			}
		}
		for (EdgeMappable o : orig.getObjects()) {
			long originID = getMappedOriginID(o.getOuterID());
			// add
			if (!measurements.containsKey(originID)) {
				HashMap<Long, HashMap<Long, T>> type2dest2value = new HashMap<>();
				for(Long type : typeid2type.keySet()) {
					HashMap<Long, T> nDestMap = new HashMap<>();
					for(Long l : destMap.keySet()) {
						nDestMap.put(l, parent.buildEmptyEntry(originID, l));
					}
					type2dest2value.put(type,  nDestMap);
				}
				measurements.put(originID, type2dest2value);
			}
		}
	}


	/**
	 * @brief Adds a result
	 * @param beginTime The begin time of the route
	 * @param od A connection (path) between an origin and a destination
	 * @throws IOException When writing fails
	 */
	public void add(int beginTime, SingleODResult od) throws IOException {
		T entry = parent.buildResult(beginTime, od);
		// no aggregation, write directly
		if (origin2aggMap == null && dest2aggMap == null && !sumOrigins && !sumDestinations) {
			for(Long type : dest2types.get(entry.originID))	{ // !!! -1
				write(entry, typeid2type.get(type));
			}
			return;
		}
		// aggregation
		entry.originID = getMappedOriginID(entry.originID);
		long origDestID = entry.destID;
		entry.destID = getMappedDestinationID(entry.destID);
		// TODO: check if we could write directly if no origin aggregation and destination=="all" add
		HashMap<Long, HashMap<Long, T>> type2dest2value = measurements.get(entry.originID);
		for(Long type : dest2types.get(origDestID))	{ // !!! -1
			HashMap<Long, T> destMap = type2dest2value.get(type);
			destMap.get(entry.destID).addCounting(entry);
		}
	}
	
	
	/** @brief Closes the processing of an origin
	 * 
	 * @param originID The origin ID
	 * @throws IOException When something fails
	 */
	public void endOrigin(long originID) throws IOException {
		if(origin2aggMap!=null||sumOrigins) {
			// origins are aggregated - cannot flush
			return;
		}
		if (dest2aggMap==null&&!sumDestinations) {
			// no destination aggregation - nothing to do
			return;
		}
		// flush aggregation for the origin
		HashMap<Long, HashMap<Long, T>> type2dest2value = measurements.get(originID);
		for (Long type : typeid2type.keySet()) {
			HashMap<Long, T> dest2value = type2dest2value.get(type);
			for (Long destID : dest2value.keySet()) {
				@SuppressWarnings("unchecked")
				T normed = (T) dest2value.get(destID).getNormed(1, 1);
				write(normed, typeid2type.get(type));
			}
		}
		measurements.remove(originID);
	}
	

	/**
	 * @brief Finishes writing, optionally generating normed collected measures and flushing outputs
	 * @throws IOException When something fails
	 */
	public void finish() throws IOException {
		// check if only destinations are aggregated
		if(origin2aggMap==null && !sumOrigins) {
			for (Long originID : measurements.keySet()) {
				HashMap<Long, HashMap<Long, T>> type2dest2value = measurements.get(originID);
				for (Long type : typeid2type.keySet()) {
					HashMap<Long, T> dests = type2dest2value.get(originID);
					for (Long destID : dests.keySet()) {
						@SuppressWarnings("unchecked")
						T normed = (T) dests.get(destID).getNormed(1, 1);
						write(normed, typeid2type.get(type));
					}
				}
			}
		}
		// otherwise
		if(origin2aggMap!=null || sumOrigins) {
			Vector<EdgeMappable> origins = fromLayer.getObjects();
			HashMap<Long, Double> weights = new HashMap<>();
			HashMap<Long, Integer> nums = new HashMap<>();
			for (EdgeMappable o : origins) {
				long aOrigin= getMappedOriginID(o.getOuterID());
				if(!weights.containsKey(aOrigin)) {
					weights.put(aOrigin, 0.);
					nums.put(aOrigin, 0);
				}
				weights.put(aOrigin, weights.get(aOrigin) + ((LayerObject) o).getAttachedValue());
				nums.put(aOrigin, nums.get(aOrigin) + 1);
			}
			for (Long originID : measurements.keySet()) {
				HashMap<Long, HashMap<Long, T>> type2dest2value = measurements.get(originID);
				for (Long type : typeid2type.keySet()) {
					// build normed results
					HashMap<Long, T> dests = type2dest2value.get(originID);
					for (Long destID : dests.keySet()) {
						@SuppressWarnings("unchecked")
						T normed = (T) dests.get(destID).getNormed(nums.get(originID), weights.get(originID));
						write(normed, typeid2type.get(type));
					}
				}
			}
		}
		//
		for (AbstractResultsWriter<T> bw : writers) {
			bw.close();
		}
	}
	
}

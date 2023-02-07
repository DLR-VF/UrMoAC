/*
 * Copyright (c) 2016-2023 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * https://github.com/DLR-VF/UrMoAC
 * Licensed under the Eclipse Public License 2.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.shapes;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.locationtech.jts.geom.LineString;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.modes.Mode;

/**
 * @class DBEdge
 * @brief An edge in the transportation network
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class DBEdge {
	/**
	 * @class V
	 * @bried The information about the speed at this edge during an interval
	 * @author dkrajzew
	 */
	class V {
		/**
		 * @brief Constructor
		 * @param ibegin The begin of the interval
		 * @param iending The end of the interval
		 * @param speed The speed during this interval
		 */
		public V(float ibegin, float iending, float speed) {
			ibeg = ibegin;
			iend = iending;
			v = speed;
		}
		
		/// @brief the time this information starts to be valid at
		float ibeg;
		
		/// @brief the time this information ends to be valid at
		float iend;
		
		/// @brief max speed in interval
		float v;
		
	}
	
	
	/**
	 * @brief Compares intervals for sorting them in time
	 * @author dkrajzew
	 */
	class VComparator implements Comparator<V> {
		/**
		 * @brief Compares two intervals
		 * @param a First interval
		 * @param b Second interval
		 * @return Comparison
		 */
	    public int compare(V a, V b) {
	    	return a.ibeg < b.ibeg ? -1 : a.ibeg == b.ibeg ? 0 : 1;
	    }
	}
	
	
	
	
	/// @brief A numerical id of the edge (internal, used?!!!)
	public long numID;
	/// @brief The id of the edge as given in the db
	public String id;
	/// @brief A reference to the node this edge starts at (A GTFStop instance)
	public DBNode from;
	/// @brief A reference to the node this edge ends at (A GTFStop instance)
	public DBNode to;
	/// @brief The allowed modes of transport
	public long modes;
	/// @brief The maximum velocity allowed at this edge
	public double vmax;
	/// @brief The geometry of this edge
	public LineString geom;
	/// @brief The length of this edge
	public double length;
	/// @brief Objects assigned to this edge
	public HashSet<EdgeMappable> objects = null;
	/// @brief The list of travel time informations for this edge
	public Vector<V> speeds = null;
	/// @brief The sum of attached values
	public double attachedValuesSum = 0;
	/// @brief The opposite direction
	public DBEdge opposite = null;


	/**
	 * @brief Constructor
	 * @param _numID A numerical id of the edge (internal, used?!!!)
	 * @param _id The id of the edge as given in the db
	 * @param _from A reference to the node this edge starts at (A GTFStop instance)
	 * @param _to A reference to the node this edge ends at (A GTFStop instance)
	 * @param _modes The allowed modes of transport
	 * @param _vmax The maximum velocity allowed at this edge
	 * @param _geom The geometry of this edge
	 * @param _length The length of this edge
	 */
	public DBEdge(long _numID, String _id, DBNode _from, DBNode _to, long _modes, double _vmax, LineString _geom,
			double _length) {
		numID = _numID;
		id = _id;
		from = _from;
		to = _to;
		modes = _modes;
		vmax = _vmax;
		geom = _geom;
		length = _length;
		_from.addOutgoing(this);
		_to.addIncoming(this);
	}


	/**
	 * @brief Returns the node this edge starts at
	 * @return This edge's starting node
	 */
	public DBNode getFromNode() {
		return from;
	}


	/**
	 * @brief Returns the node this edge ends at
	 * @return This edge's destination node
	 */
	public DBNode getToNode() {
		return to;
	}


	/**
	 * @brief Returns this edge's length [m]
	 * @return This edge's length
	 */
	public double getLength() {
		return length;
	}


	/**
	 * @brief Returns this edge's id
	 * @return This edge's id
	 */
	public String getID() {
		return id;
	}


	/**
	 * @brief Returns this edge's geometry
	 * @return This edge's geometry
	 */
	public LineString getGeometry() {
		return geom;
	}


	/**
	 * @brief Returns whether the given mode of transport is allowed on this edge
	 * @param mode The transport mode to use
	 * @return Whether this mode of transport is allowed
	 */
	public boolean allows(long mode) {
		return (modes & mode) != 0;
	}


	/**
	 * @brief Returns whether the given mode of transport is allowed on this edge
	 * @param mode The transport mode to use
	 * @return Whether this mode of transport is allowed
	 */
	public boolean allows(Mode mode) {
		return (modes & mode.id) != 0;
	}


	/**
	 * @brief Returns whether the given mode of transport is allowed on this edge
	 * @param modes The transport modes to use
	 * @return Whether this mode of transport is allowed
	 */
	public boolean allowsAny(long modes) {
		return (this.modes & modes) != 0;
	}


	/**
	 * @brief Returns the travel time
	 * 
	 * As this edge is walked by continuously, the travel time is the
	 * edge's length divided by the used speed.
	 * @param ivmax The individual's speed
	 * @param time The time the edge is started to be passed
	 * @return The travel time to pass this edge
	 */
	public double getTravelTime(double ivmax, double time) {
		if(speeds!=null) {
			for(V v : speeds) {
				if(v.ibeg<=time && v.iend>=time) {
					double vg = v.v;
					if(vg<=0) {
						vg = 5./3.6;
					}
					return length / Math.min(ivmax, Math.min(vg, vmax));
				}
			}
		}
		double v = Math.min(vmax, ivmax);
		return length / v;
	}


	/**
	 * @brief Adds a new objects to this edge
	 * @param em The object to add
	 */
	public synchronized void addMappedObject(EdgeMappable em) {
		if(objects==null) {
			objects = new HashSet<>();
		}
		objects.add(em);
		attachedValuesSum += ((LayerObject) em).getAttachedValue();
	}


	/**
	 * @brief Returns the attached objects
	 * @return The objects attached to this edge
	 */
	public Set<EdgeMappable> getMappedObjects() {
		return objects;
	}


	/**
	 * @brief Returns the sum of the attached object's values
	 * @return Sum of the values of the objects attached to this edge
	 */
	public double getAttachedValues() {
		return attachedValuesSum;
	}


	/**
	 * @brief Returns the number of attached objects
	 * @return The number of attached objects
	 */
	public int getAttachedObjectsNumber() {
		if(objects==null) {
			return 0;
		}
		return objects.size();
	}


	/**
	 * @brief Adds an information about a speed valid for a given time span
	 * @param ibegin The begin of the time interval
	 * @param iending The end of the time interval
	 * @param speed The speed at this edge during this time interval
	 */
	public void addSpeedReduction(float ibegin, float iending, float speed) {
		if(speeds==null) {
			speeds = new Vector<>();
		}
		speeds.add(new V(ibegin, iending, speed));
	}
	
	
	/**
	 * @brief Sorts the speed limit intervals by time
	 */
	public void sortSpeedReductions() {
		if(speeds==null) {
			return;
		}
		speeds.sort(new VComparator());
	}


	/**
	 * @brief Returns the kilocalories used to pass this edge
	 * @param usedMode The used mode of transport
	 * @param tt The travel time at this edge
	 * @return The used kilocalories
	 * 	
	 */
	public double getKKC(Mode usedMode, double tt) {
		return usedMode.kkcPerHour * tt / 3600.;
	}


	/**
	 * @brief Returns the amount of emitted CO2 in g
	 * @param usedMode The used mode of transport
	 * @return The amount of emitted CO2 in g
	 */
	public double getCO2(Mode usedMode) {
		return usedMode.co2PerKm / 1000 * length;
	}


	/**
	 * @brief Returns the price to pass this edge
	 * @param usedMode The used mode of transport
	 * @param nlines The previously used lines
	 * @return The price to pass this edge
	 */
	public double getPrice(Mode usedMode, Set<String> nlines) {
		return usedMode.pricePerKm / 1000 * length;
	}
	

	/**
	 * @brief Returns whether this is a GTFS edge (false)
	 * @return no, this is not a GTFS edge
	 */
	public boolean isGTFSEdge() {
		return false;
	}


	/**
	 * @brief Adds a mode that can be used at this edge to this edge
	 * @param mode The mode that may be used
	 */
	public void addMode(long mode) {
		modes |= mode;
	}

}

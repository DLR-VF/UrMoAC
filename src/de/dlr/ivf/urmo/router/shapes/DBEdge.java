/*
 * Copyright (c) 2016-2024
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
package de.dlr.ivf.urmo.router.shapes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.modes.Mode;

/**
 * @class DBEdge
 * @brief An edge in the transportation network
 * @author Daniel Krajzewicz
 */
public class DBEdge {
	/**
	 * @class V
	 * @bried The information about the speed at this edge during an interval
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
	
	
	
	
	/// @brief The id of the edge as given in the db
	private String id;
	/// @brief A reference to the node this edge starts at (A GTFStop instance)
	private DBNode from;
	/// @brief A reference to the node this edge ends at (A GTFStop instance)
	private DBNode to;
	/// @brief The allowed modes of transport
	private long modes;
	/// @brief The maximum velocity allowed at this edge
	private double vmax;
	/// @brief The geometry of this edge
	private LineString geom;
	/// @brief The length of this edge
	private double length;
	/// @brief The incline of this edge
	private double incline;
	/// @brief Objects assigned to this edge
	private HashSet<EdgeMappable> objects = null;
	/// @brief The list of travel time informations for this edge
	private Vector<V> speeds = null;
	/// @brief The opposite direction
	private DBEdge opposite = null;
	/// @brief Crossing times to subsequent edges
	private HashMap<DBEdge, Double> crossingTimes; 
	double precomputedTT = -1;


	/**
	 * @brief Constructor
	 * @param _id The id of the edge as given in the db
	 * @param _from A reference to the node this edge starts at (A GTFStop instance)
	 * @param _to A reference to the node this edge ends at (A GTFStop instance)
	 * @param _modes The allowed modes of transport
	 * @param _vmax The maximum velocity allowed at this edge
	 * @param _geom The geometry of this edge
	 * @param _length The length of this edge
	 */
	public DBEdge(String _id, DBNode _from, DBNode _to, long _modes, double _vmax, LineString _geom, double _length, double _incline) {
		id = _id;
		from = _from;
		to = _to;
		modes = _modes;
		vmax = _vmax;
		geom = _geom;
		length = _length;
		incline = _incline;
		_from.addOutgoing(this);
		_to.addIncoming(this);
	}


	/**
	 * @brief Returns the node this edge starts at
	 * @return This edge's starting node
	 */
	public void setOppositeEdge(DBEdge e) {
		if(opposite!=null&&opposite!=e) {
			throw new RuntimeException("opposite edge set twice"); // !!!
		}
		opposite = e;
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
	 * @brief Returns this edge's incline [%]
	 * @return This edge's length
	 */
	public double getIncline() {
		return incline;
	}


	/**
	 * @brief Returns the modes allowed on this edge
	 * @return The modes allowed on this edge
	 */
	public long getModes() {
		return modes;
	}


	/**
	 * @brief Returns the speed allowed on this edge
	 * @return The speed allowed on this edge
	 */
	public double getVMax() {
		return vmax;
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
		if(geom!=null) {
			return geom;
		}
		Coordinate[] edgeCoords = new Coordinate[2];
		edgeCoords[0] = from.getCoordinate();
		edgeCoords[1] = to.getCoordinate();
		GeometryFactory gf = new GeometryFactory(new PrecisionModel());
		return geom = gf.createLineString(edgeCoords);
	}


	/**
	 * @brief Returns this edge's opposite edge
	 * @return This edge's opposite edge
	 */
	public DBEdge getOppositeEdge() {
		return opposite;
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
	 * @return Whether any of the given modes of transport is allowed on this edge
	 */
	public boolean allowsAny(long modes) {
		return (this.modes & modes) != 0;
	}


	/**
	 * @brief Returns whether all given modes may be used at this edges
	 * @param modes The transport modes to use
	 * @return Whether all of the given modes of transport are allowed on this edge
	 */
	public boolean allowsAll(long modes) {
		return (this.modes & modes) == modes;
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
		if(precomputedTT>=0) {
			return precomputedTT;
		}
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
		// !!! per mode
		if(incline!=0) {
			if(incline>6) {
				ivmax *= 0.9;
			} else if(incline<-6) {
				ivmax *= 1.05;
			}
		}
		double v = Math.min(vmax, ivmax);
		return length / v;
	}
	
	
	public void precomputeTT(double ivmax) {
		precomputedTT = getTravelTime(ivmax, 0);
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
	}


	/**
	 * @brief Returns the attached objects
	 * @return The objects attached to this edge
	 */
	public Set<EdgeMappable> getMappedObjects() {
		return objects;
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


	/**
	 * @brief "Joins" both edges
	 * 
	 * It is assumes that both edges have the same geometry and should be treated as one.
	 * 
	 * The method extends the allowed modes by the ones of the given edge and adapts
	 * the allowed velocity to the maximum of both edges' allowed velocities.
	 * @param e The edge to combine with this one
	 */
	public void adapt(DBEdge e) {
		// geometry is same...
		modes = modes | e.modes;
		vmax = Math.max(vmax, e.vmax);
	}


	/** @brief Returns whether both edges are overlapping
	 * 
	 * @param e The edge to compare the geometry to
	 * @return Whether both edges overlap
	 */
	public double maxDistanceTo(DBEdge e) {
		double maxDistance = 0;
		LineString eg = e.getGeometry();
		for(int i=0; i<eg.getNumPoints(); ++i) {
			maxDistance = Math.max(maxDistance, getGeometry().distance(eg.getPointN(i)));
		}
		eg = getGeometry();
		for(int i=0; i<eg.getNumPoints(); ++i) {
			maxDistance = Math.max(maxDistance, e.getGeometry().distance(eg.getPointN(i)));
		}
		return maxDistance;
	}


	/** @brief Sets the time needed to get to the other edge at the intersections
	 * 
	 * @param e The subsequent edge
	 * @param value The crossing time
	 */
	public void setCrossingTimeTo(DBEdge e, double value) {
		if (crossingTimes==null) {
			crossingTimes = new HashMap<DBEdge, Double>();
		}
		crossingTimes.put(e, value);
	}

	
	/** @brief Returns the time needed to get to the other edge at the intersections
	 * 
	 * @param e The subsequent edge
	 * @return The time needed to cross the intersection
	 */
	public double getCrossingTimeTo(DBEdge e) {
		if (crossingTimes==null) {
			return 0;
		}
		if (!crossingTimes.containsKey(e)) {
			return 0;
		}
		return crossingTimes.get(e);
	}


	public void setVMax(double value) {
		vmax = value;
	}
	
	
	public void nullifyGeometry() {
		geom = null;
	}
	
	
	public boolean isUnusedDeadEnd(HashMap<DBEdge, Vector<MapResult>> nearestFromEdges, HashMap<DBEdge, Vector<MapResult>> nearestToEdges, DBEdge prior) {
		if(nearestFromEdges.containsKey(this)||nearestToEdges.containsKey(this)) {
			return false;
		}
		if(opposite!=null) {
			if(nearestFromEdges.containsKey(opposite)||nearestToEdges.containsKey(opposite)) {
				return false;
			}
		}
		Set<DBEdge> followingEdges = new HashSet<DBEdge>(to.getOutgoing());
		if(prior!=null) {
			followingEdges.remove(prior);
		}
		if(opposite!=null) {
			followingEdges.remove(opposite);
		}
		return followingEdges.size()==0; 
	}

}

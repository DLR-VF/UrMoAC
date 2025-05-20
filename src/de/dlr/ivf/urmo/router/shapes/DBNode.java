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

import java.io.IOException;
import java.util.Vector;

import org.locationtech.jts.geom.Coordinate;

import de.dlr.ivf.urmo.router.algorithms.routing.CrossingTimesModel_CTM1;

/**
 * @class DBNode
 * @brief A node in the transportation network
 * @author Daniel Krajzewicz
 */
public class DBNode {
	/** @class AllowedModeChange
	 * @brief Describes the possibility to change the used mode of transport at a node
	 */
	public class AllowedModeChange {
		/// @brief The previously used mode of transport
		private long from;
		/// @brief The used mode of transport after the change
		private long to;
		/// @brief The time needed to change the mode of transport
		private double time;
		/// @brief The price for changing the mode of transport
		private double price;
		
		
		/** @brief Constructor
		 * 
		 * @param _from The previously used mode of transport
		 * @param _to The used mode of transport after the change
		 * @param _time The time needed to change the mode of transport
		 * @param _price The price for changing the mode of transport
		 */
		public AllowedModeChange(long _from, long _to, double _time, double _price) {
			from = _from;
			to = _to;
			time = _time;
			price = _price;
		}
		
		
		/** @brief Returns the previously used mode of transport
		 * @return The previously used mode of transport
		 */
		public long getFromMode() {
			return from;
		}
		

		/** @brief Returns the used mode of transport after the change
		 * @return The used mode of transport after the change
		 */
		public long getToMode() {
			return to;
		}


		/** @brief Returns the time needed to change the mode of transport
		 * @return The time needed to change the mode of transport
		 */
		public double getTime() {
			return time;
		}

		
		/** @brief Returns the price for changing the mode of transport
		 * @return The price for changing the mode of transport
		 */
		public double getPrice() {
			return price;
		}

	}
	
	
	
	/// @brief The node's id
	private long id;
	/// @brief The node's coordinates
	private Coordinate pos;
	/// @brief The list of edges that end at this node
	private Vector<DBEdge> incoming = new Vector<>();
	/// @brief The list of edges that start at this node
	private Vector<DBEdge> outgoing = new Vector<>();
	/// @brief The possibilities to change the mode of transport at this node
	private Vector<AllowedModeChange> modeChanges = null;


	/**
	 * @brief Constructor
	 * @param _id The node's id
	 * @param _pos The node's coordinates
	 */
	public DBNode(long _id, Coordinate _pos) {
		id = _id;
		pos = _pos;
	}


	/**
	 * @brief Returns the ID of this node
	 * @return The id of this node
	 */
	public long getID() {
		return id;
	}

	
	/**
	 * @brief Returns the list of edges that end at this node
	 * @return Edges that end at this node
	 */
	public Vector<DBEdge> getIncoming() {
		return incoming;
	}


	/**
	 * @brief Returns the list of edges that start at this node
	 * @return Edges that start at this node
	 */
	public Vector<DBEdge> getOutgoing() {
		return outgoing;
	}


	/**
	 * @brief Adds an edge that ends at this node
	 * @param e The edge to add
	 */
	public void addIncoming(DBEdge e) {
		incoming.add(e);
	}


	/**
	 * @brief Adds an edge that starts at this node
	 * @param e The edge to add
	 */
	public void addOutgoing(DBEdge e) {
		outgoing.add(e);
	}


	/**
	 * @brief Removes the given edge from the ones that end at this node
	 * @param e The edge to remove
	 */
	public void removeIncoming(DBEdge e) {
		incoming.remove(e);
	}


	public void replaceIncoming(DBEdge e, DBEdge by) {
		incoming.remove(e);
		incoming.add(by);
	}

	
	
	/**
	 * @brief Removes the given edge from the ones that start at this node
	 * @param e The edge to remove
	 */
	public void removeOutgoing(DBEdge e) {
		outgoing.remove(e);
	}

	
	public void replaceOutgoing(DBEdge e, DBEdge by) {
		outgoing.remove(e);
		outgoing.add(by);
		for(DBEdge incEdge : incoming) {
			incEdge.replaceOutgoing(e, by);
		}
	}

	
	
	
	/** @brief Returns the coordinate
	 * @return The coordinate
	 */
	public Coordinate getCoordinate() {
		return pos;
	}

	
	/** @brief Adds the possibility to change the mode of transport at this node
	 * 
	 * @param from The previously used mode of transport
	 * @param to The used mode of transport after the change
	 * @param time The time needed to change the mode of transport
	 * @param price The price for changing the mode of transport
	 */
	public void addModeChange(long from, long to, double time, double price) {
		if (modeChanges==null) {
			modeChanges = new Vector<>();
		}
		modeChanges.add(new DBNode.AllowedModeChange(from, to, time, price));
	}
	
	
	/** @brief Returns whether the mode of transport can be changed at this node
	 * @return Whether the mode of transport can be changed at this node
	 */
	public boolean allowsModeChange() {
		return modeChanges!=null && modeChanges.size()!=0;
	}
	
	
	/** @brief Returns the list of mode changes possible at this node 
	 * @return The list of mode changes possible at this node 
	 */
	public Vector<AllowedModeChange> getAllowedModeChanges() {
		return modeChanges;
	}
	
	
	/** @brief Computes the crossing times to subsequent edges for all incoming edges
	 * 
	 * @param ctm The model used to compute the crossing times
	 * @throws IOException When the crossing times writer fails
	 */
	public void computeCrossingTimes(CrossingTimesModel_CTM1 ctm) throws IOException {
		if(ctm==null) {
			return;
		}
		for(DBEdge incomingEdge : incoming) {
			ctm.computeCrossingTimes(incomingEdge);
		}
	}
	
	
}

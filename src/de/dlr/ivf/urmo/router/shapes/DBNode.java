/**
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * 
 * Copyright © 2016-2019 German Aerospace Center 
 * All rights reserved
 */
package de.dlr.ivf.urmo.router.shapes;

import java.util.Vector;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @class DBNode
 * @brief A node in the transportation network
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of Transport Research
 */
public class DBNode {
	/// @brief The node's id
	public long id;
	/// @brief The node's coordinates
	public Coordinate pos;
	/// @brief The list of edges that end at this node
	public Vector<DBEdge> incoming = new Vector<>();
	/// @brief The list of edges that start at this node
	public Vector<DBEdge> outgoing = new Vector<>();


	/**
	 * @Constructor
	 * @param _id The node's id
	 * @param _pos The node's coordinates
	 */
	public DBNode(long _id, Coordinate _pos) {
		id = _id;
		pos = _pos;
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


	/**
	 * @brief Removes the given edge from the ones that start at this node
	 * @param e The edge to remove
	 */
	public void removeOutgoing(DBEdge e) {
		outgoing.remove(e);
	}


	/**
	 * @brief Returns the set interchange time - or no time if no time is given
	 * 
	 * Returns 0 (seconds) if the line is not changed. Returns the defaultTime otherwise.
	 * 
	 * @param line First line
	 * @param line2 Second line
	 * @param defaultTime The default interchange time
	 * @return The interchange time
	 * @todo Play with this
	 */
	public double getInterchangeTime(String line, String line2, double defaultTime) {
		if(line.equals(line2)) {
			return 0;
		}
		return defaultTime;
	}

}

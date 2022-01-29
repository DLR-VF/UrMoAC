/**
 * Copyright (c) 2016-2021 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * @author: Daniel.Krajzewicz@dlr.de
 * Licensed under the GNU General Public License v3.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.shapes;

import java.util.Vector;

import org.locationtech.jts.geom.Coordinate;

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

	
}

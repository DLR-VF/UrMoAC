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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.index.strtree.STRtree;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.CrossingTimesModel_CTM1;
import de.dlr.ivf.urmo.router.mivspeeds.SpeedModel;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.output.NetErrorsWriter;

/**
 * @class DBNet
 * @brief A transportation network
 * @author Daniel Krajzewicz
 */
public class DBNet {
	/// @brief Map from node ids to nodes
	private HashMap<Long, DBNode> nodes = new HashMap<>();
	/// @brief Map of edge names to edges
	private HashMap<String, DBEdge> name2edge = new HashMap<String, DBEdge>();
	/// @brief Map of node names (if using strings) to nodes
	private HashMap<String, Long> name2nodeID = new HashMap<String, Long>();
	/// @brief The network's minimum coordinates (left top)
	private Coordinate minCorner = null;
	/// @brief The network's maximum coordinates (right bottom)
	private Coordinate maxCorner = null;
	/// @brief The resulting geometry factory
	private GeometryFactory geometryFactory = null;
	/// @brief The id supplier to use
	private IDGiver idGiver = null;
	/// @brief The logger used to store network errors
	private NetErrorsWriter log = null;
	/// @brief A state variable for reporting overwritten edges (0: report all, 1: report first, 2: report first, first reported)
	private int stateDuplicateEdges = 0;
	/// @brief A state variable for reporting edges with speed==0 (0: report all, 1: report first, 2: report first, first reported)
	private int stateEdges0VMax = 0;
	/// @brief A state variable for reporting edges with length==0 (0: report all, 1: report first, 2: report first, first reported)
	private int stateEdges0Length = 0;
	/// @brief A state variable for reporting duplicate edges (0: report all, 1: report first, 2: report first, first reported)
	private int stateEdgesSameID = 0;
	/// @brief Whether network errors (vmax=0, length=0) shall be patched
	private boolean patchErrors;


	/**
	 * @brief Constructor
	 * @param _idGiver The id supplier to use
	 */
	public DBNet(IDGiver _idGiver, NetErrorsWriter _log, boolean _reportAllIssues, boolean _patchErrors) {
		idGiver = _idGiver;
		log = _log;
		stateDuplicateEdges = _reportAllIssues==true ? 0 : 1;
		stateEdges0VMax = _reportAllIssues==true ? 0 : 1;
		stateEdges0Length = _reportAllIssues==true ? 0 : 1;
		stateEdgesSameID = _reportAllIssues==true ? 0 : 1;
		patchErrors = _patchErrors;
		geometryFactory = new GeometryFactory(new PrecisionModel());
	}

	
	/** @brief Adds an edge building it
	 * @param _id The string id of the edge
	 * @param _from The starting node
	 * @param _to The ending node
	 * @param _modes Allowed modes
	 * @param _vmax Maximum velocity allowed at this edge
	 * @param _geom The geometry of the edge
	 * @param _length The length of the edge
	 * @return The built edge
	 * @throws IOException 
	 */
	public boolean addEdge(String _id, DBNode _from, DBNode _to, long _modes, double _vmax, LineString _geom, double _length, double _incline) throws IOException {
		boolean hadError = false;
		if(_length<=0) {
			if(stateEdges0Length!=2) {
				if(patchErrors) {
					System.err.println("Warning: Edge '" + _id + "' has a length of 0. Will be set to 0.1 m.");
				} else {
					System.err.println("Error: Edge '" + _id + "' has a length of 0.");
				}
				if(stateEdges0Length!=0) {
					stateEdges0Length = 2;
					System.err.println("Warning: Subsequent edges with length=0 will not be reported; use --write.net-errors or --net.report-all-errors to get the complete list.");
				}
			}
			if(log!=null) log.writeNoLength(_id);
			if(patchErrors) {
				_length = .1;
			} else {
				hadError = true;
			}
		}
		if(_vmax<=0) {
			if(stateEdges0VMax!=2) {
				if(patchErrors) {
					System.err.println("Warning: Edge '" + _id + "' has a speed of 0. Will be set to 0.1 m/s.");
				} else {
					System.err.println("Error: Edge '" + _id + "' has a speed of 0.");
				}
				if(stateEdges0VMax!=0) {
					stateEdges0VMax = 2;
					System.err.println("Warning: Subsequent edges with vmax=0 will not be reported; use --write.net-errors or --net.report-all-errors to get the complete list.");
				}
			}
			if(log!=null) log.writeNoSpeed(_id);
			if(patchErrors) {
				_vmax = .1;
			} else {
				hadError = true;
			}
		}
		if(name2edge.containsKey(_id)) {
			if(stateEdgesSameID!=2) {
				System.err.println("Error: Edge '" + _id + "' already exists.");
				if(stateEdgesSameID!=0) {
					stateEdgesSameID = 2;
					System.err.println("Warning: Subsequent edge with duplicate IDs will not be reported; use --write.net-errors or --net.report-all-errors to get the complete list.");
				}
			}
			if(log!=null) log.writeDuplicate(_id);
			hadError = true;
		}
		if(!hadError) {
			DBEdge e = new DBEdge(_id, _from, _to, _modes, _vmax, _geom, _length, _incline);
			addEdge(e);
		}
		return !hadError;
	}

	
	/**
	 * @brief Adds an edge to the road network
	 * @param e The edge to add
	 * @throws IOException 
	 */
	private void addEdge(DBEdge e) throws IOException {
		// double edge check
		DBNode fromNode = e.getFromNode();
		DBNode toNode = e.getToNode();
		Vector<DBEdge> outgoing = fromNode.getOutgoing();
		for(DBEdge e2 : outgoing) {
			if(e==e2||e2.getToNode()!=toNode) {
				continue;
			}
			if(e2.maxDistanceTo(e)<.5) {//.getGeometry().equals(e.getGeometry())) {
				e2.adapt(e);
				removeEdge(e);
				if (log!=null) log.writeEdgeReplacement(e2.getID(), e.getID());
				if(stateDuplicateEdges!=2) {
					System.err.println("Warning: removed edge '" + e.getID() + "' as a duplicate of edge '" + e2.getID() + "'.");
					if(stateDuplicateEdges!=0) {
						stateDuplicateEdges = 2;
						System.err.println("Warning: Subsequent replacements will not be reported; use --write.net-errors or --net.report-all-errors to get the complete list.");
					}
				}
				return;
			}
			/** @todo: ok, this happens usually on circular roads that have been split.
			 * But what if there were two edges, e.g. one for pedestrians and one for passenger vehicles over each other? 
			 */
		}
		//
		name2edge.put(e.getID(), e);
		Coordinate[] cs = e.getGeometry().getCoordinates();
		for (int i = 0; i < cs.length; ++i) {
			Coordinate c = cs[i];
			if (minCorner == null) {
				minCorner = new Coordinate(c.x, c.y);
				maxCorner = new Coordinate(c.x, c.y);
			}
			minCorner.x = Math.min(minCorner.x, c.x);
			minCorner.y = Math.min(minCorner.y, c.y);
			maxCorner.x = Math.max(maxCorner.x, c.x);
			maxCorner.y = Math.max(maxCorner.y, c.y);
		}
	}


	/**
	 * @brief Adds a node to this road network
	 * @param node The node to add
	 */
	public boolean addNode(DBNode node) {
		if(nodes.containsKey(node.getID())) {
			return false;
		}
		nodes.put(node.getID(), node);
		return true;
	}


	/**
	 * @brief Adds a node to this road network
	 * @param node The node to add
	 */
	public boolean addNode(DBNode node, String origName) {
		if(!name2nodeID.containsKey(origName)) {
			name2nodeID.put(origName, node.getID());
		}
		return addNode(node);
	}


	/**
	 * @brief Returns the named node or builds it if not existing
	 * @param id The id of the node to return
	 * @param pos The node's position
	 * @return The node
	 */
	public DBNode getNode(long id, Coordinate pos) {
		if (nodes.containsKey(id)) {
			return nodes.get(id);
		}
		DBNode n = new DBNode(id, pos);
		nodes.put(id, n);
		idGiver.hadExternID(id);
		return n;
	}


	/**
	 * @brief Returns the named node or builds it if not existing
	 * @param sid The id of the node to return
	 * @param pos The node's position
	 * @return The node
	 */
	public DBNode getNode(String sid, Coordinate pos) {
		if(!name2nodeID.containsKey(sid)) {
			long id = idGiver.getNextRunningID();
			name2nodeID.put(sid, id);
		}
		long id = name2nodeID.get(sid);
		return getNode(id, pos);
	}

	
	public DBNode getExistingNode(String sid) {
		if(!name2nodeID.containsKey(sid)) {
			return null;
		}
		long id = name2nodeID.get(sid);
		return nodes.get(id);
	}
	
	

	/**
	 * @brief Returns the named edge (by name)
	 * @param name The name of the edge
	 * @return The edge (if known, otherwise null)
	 */
	public DBEdge getEdgeByName(String name) {
		return name2edge.get(name);
	}


	/**
	 * @brief Removes this edge from the network
	 * 
	 * The references to this edge are removed from the start/end node.
	 * 
	 * @param edge The edge to remove
	 */
	public void removeEdge(DBEdge edge) {
		name2edge.remove(edge.getID());
		edge.getFromNode().removeOutgoing(edge);
		edge.getToNode().removeIncoming(edge);
	}
	

	/**
	 * @brief Builds and return a spatial index for the parts of the road
	 *        network that may be traveled by the given transport modes
	 * @param modes The available modes of transport
	 * @return The network subparts compound of edges that may be traveled by the given modes
	 */
	public STRtree getModedSpatialIndex(long modes) {
		STRtree tree = new STRtree();
		for (DBEdge e : name2edge.values()) {
			if (!e.allowsAny(modes)) {
				continue;
			}
			tree.insert(e.getGeometry().getEnvelopeInternal(), e);
		}
		return tree;
	}


	/**
	 * @brief Returns the nodes of this road network
	 * @return This road network's nodes
	 */
	public HashMap<Long, DBNode> getNodes() {
		return nodes;
	}


	/**
	 * @brief Returns the maximum if used within this network
	 * @return The maximum id used in this road network
	 */
	public long getNextID() {
		return idGiver.getNextRunningID();
	}


	/**
	 * @brief Checks which edges are not connected to the major part of the network and removes them
	 * @param report Whether the removal of edges shall be reported
	 */
	public HashMap<Integer, Set<DBEdge>> dismissUnconnectedEdges(boolean report) {
		HashMap<DBEdge, Integer> edge2cluster = new HashMap<>();
		HashMap<Integer, Set<DBEdge>> clusters = new HashMap<>();
		int nextClusterIndex = 0;
		for (DBEdge e : name2edge.values()) {
			if (edge2cluster.containsKey(e)) {
				// skip, it has already been visited
				continue;
			}
			// go through connected edges (outgoing at each node only)
			// build a list of edges to process
			Vector<DBEdge> next = new Vector<>();
			next.add(e);
			Set<DBEdge> currCluster = new HashSet<>();
			int clusterIndex = nextClusterIndex;
			while (!next.isEmpty()) {
				// get next edge to process from the list
				DBEdge e2 = next.remove(next.size() - 1);
				/*
				if(edge2cluster.containsKey(e2)) {
					continue;
				}
				*/
				if(edge2cluster.containsKey(e2)) {
					if(edge2cluster.get(e2)!=clusterIndex) {
						// ok, it already belongs to a cluster - join both and proceed
						int prevClusterIndex = edge2cluster.get(e2);
						Set<DBEdge> prevCluster = clusters.get(prevClusterIndex);
						prevCluster.addAll(currCluster);
						// update information for already set edges
						for(DBEdge e3 : currCluster) {
							edge2cluster.put(e3, prevClusterIndex);
						}
						currCluster = prevCluster;
						clusterIndex = prevClusterIndex;
					}
				} else {
					// add to current cluster
					currCluster.add(e2);
					edge2cluster.put(e2, clusterIndex);
				}
				for(DBEdge e3 : e2.getFromNode().getIncoming()) {
					if(!edge2cluster.containsKey(e3)) {
						next.add(e3);
					}
				}
				for(DBEdge e3 : e2.getToNode().getOutgoing()) {
					if(!edge2cluster.containsKey(e3)) {
						next.add(e3);
					}
				}
			}
			if(clusterIndex==nextClusterIndex) {
				clusters.put(clusterIndex, currCluster);
			}
			++nextClusterIndex;
		}
		// determine major cluster
		Set<DBEdge> major = null;
		for (Set<DBEdge> c : clusters.values()) {
			if (major == null || major.size() < c.size()) {
				major = c;
			}
		}
		// report (or not)
		if(clusters.size()!=1) {
			String msg = "Warning: the network is not connected.";
			if(!report) {
				msg += " Use --subnets-summary for further information.";
			}
			System.out.println(msg);
			if(report) {
				System.out.println(" Major cluster has " + major.size() + " edges.");
				System.out.println(" Further clusters:");
				for (Set<DBEdge> c : clusters.values()) {
					if(c==major) {
						continue;
					}
					System.out.println("  cluster with " + c.size() + " edges.");
				}
			}
		}
		// remove edges from all clusters but the major one
		for (Set<DBEdge> c : clusters.values()) {
			if (c == major) {
				continue;
			}
			for (DBEdge e2 : c) {
				removeEdge(e2);
			}
		}
		return clusters;
	}
	
	
	/** @brief Returns the geometry factory
	 * @return the geometry factory
	 */
	public GeometryFactory getGeometryFactory() {
		return geometryFactory;
	}

	
	/**
	 * @brief Goes through the edges, sorts their speed reductions by time
	 */
	public void sortSpeedReductions() {
		for(DBEdge e : name2edge.values()) {
			e.sortSpeedReductions();
		}
	}


	/**
	 * @brief Extends the network by adding opposite edges
	 * @param addOppositePedestrianEdges Whether backwards edges for pedestrians shall be added
	 * @throws IOException 
	 */
	public void extendDirections(boolean addOppositePedestrianEdges) throws IOException {
		Vector<DBEdge> newEdges = new Vector<>();
		long modeFoot = Modes.getMode("foot").id;
		Collection<DBEdge> edges = name2edge.values(); 
		for(DBEdge e : edges) {
			DBNode to = e.getToNode();
			Vector<DBEdge> edges2 = to.getOutgoing();
			DBEdge opposite = null;
			for(DBEdge e2 : edges2) {
				if(e2.getToNode()==e.getFromNode() && Math.abs(e.getLength()-e2.getLength())<1.) {
					// check whether the edges are parallel
					double maxDistance = e.maxDistanceTo(e2);
					if(maxDistance<.5) {
						// opposite direction found
						opposite = e2;
						if(!e2.allows(modeFoot)&&e.allows(modeFoot)) {
							e2.addMode(modeFoot);
						}
						break;
					}
				}
			}
			// add a reverse direction edge for pedestrians
			if(addOppositePedestrianEdges && ((opposite==null && e.allows(modeFoot)))) {// || (opposite==e))) {
				// todo: recheck whether opposite==e is correct - it happens, though maybe when using an external OSM importer
				opposite = new DBEdge("opp_"+e.getID(), e.getToNode(), e.getFromNode(), modeFoot, e.getVMax(), (LineString) e.getGeometry().reverse(), e.getLength(), -e.getIncline());
				newEdges.add(opposite);
			}
			// add the information about the opposite edge
			if(opposite!=null&&opposite!=e) {
				opposite.setOppositeEdge(e);
				e.setOppositeEdge(opposite);
			}
		}
		for(DBEdge e : newEdges) {
			addEdge(e);
		}
	}
	

	/**
	 * @brief Returns the number of loaded edges
	 * @return The number of loaded edges
	 */
	public long getNumEdges() {
		return name2edge.size();
	}

	
	/**
	 * @brief Returns the bounds of the network
	 * @todo May be inaccurate due to projection?
	 * @todo Use a concave polygon?
	 * @return The bounds of the network
	 */
	public Geometry getBounds() {
		Coordinate cs[] = new Coordinate[5];
		cs[0] = minCorner;
		cs[1] = new Coordinate(maxCorner.x, minCorner.y);
		cs[2] = maxCorner;
		cs[3] = new Coordinate(minCorner.x, maxCorner.y);
		cs[4] = minCorner;
		return geometryFactory.createPolygon(cs);
	}


	/** @brief Computes the crossing times for all intersections
	 * 
	 * @param ctm The model used to compute the crossing times
	 * @throws IOException When the crossing times writer fails
	 */
	public void computeCrossingTimes(CrossingTimesModel_CTM1 ctm) throws IOException {
		if(ctm==null) {
			return;
		}
		for(DBNode n : nodes.values()) {
			n.computeCrossingTimes(ctm);
		}
	}


	public void applyVMaxModel(SpeedModel speedModel) {
		for (DBEdge e : name2edge.values()) {
			e.setVMax(speedModel.compute(e, 0));
		}
	}
	
	
	public void nullifyEdgeGeometries() {
		for (DBEdge e : name2edge.values()) {
			e.nullifyGeometry();
		}
	}


	public void removeUnusedDeadEnds(HashMap<DBEdge, Vector<MapResult>> nearestFromEdges, HashMap<DBEdge, Vector<MapResult>> nearestToEdges) {
		Set<DBEdge> toRemove = new HashSet<>();
		for (DBEdge e : name2edge.values()) {
			if(!e.isUnusedDeadEnd(nearestFromEdges, nearestToEdges, null)) {
				continue;
			}
			toRemove.add(e);
			if(e.getOppositeEdge()!=null) {
				toRemove.add(e.getOppositeEdge());
			}
			// progress backwards
			DBEdge prior = e;
			while(prior!=null) {
				Vector<DBEdge> candidates = new Vector<>();
				for(DBEdge e2 : prior.getFromNode().getIncoming()) {
					if(e2!=prior.getOppositeEdge()) {
						candidates.add(e2);
					}
				}
				if(candidates.size()!=1) {
					break;
				}
				DBEdge candidate = candidates.get(0);
				if(candidate.isUnusedDeadEnd(nearestFromEdges, nearestToEdges, prior)) {
					prior = candidate;
					toRemove.add(prior);
					if(prior.getOppositeEdge()!=null) {
						toRemove.add(prior.getOppositeEdge());
					}
				} else {
					break;
				}
			}
			
		}
		for (DBEdge e : toRemove) {
			removeEdge(e);
		}
	}
	
}

/*
 * Copyright (c) 2016-2024 DLR Institute of Transport Research
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
import java.util.Comparator;
import java.util.Set;
import java.util.Vector;

import de.dlr.ivf.urmo.router.io.Utils;
import de.dlr.ivf.urmo.router.shapes.DBEdge;

/**
 * @class DirectWriter
 * @brief Writes PTODSingleResult results to a database / file
 * @author Daniel Krajzewicz (c) 2023 German Aerospace Center, Institute of Transport Research
 */
public class NetClusterWriter extends BasicCombinedWriter {
	/// @brief Counter of results added to the database / file so far
	private int batchCount = 0;

	
	/**
	 * @brief Constructor
	 * 
	 * Opens the connection to a PostGIS database and builds the table
	 * @param format The used format
	 * @param inputParts The definition of the input/output source/destination
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @throws IOException When something fails
	 */
	public NetClusterWriter(Utils.Format format, String[] inputParts, boolean dropPrevious) throws IOException {
		super(format, inputParts, "subnets-output", 2, dropPrevious, "(edge_id text, cell_id integer, cell_size integer)");
	}
	
	
	/** @brief Get the insert statement string
	 * @param[in] format The used output format
	 * @param[in] rsid The used projection (not needed)
	 * @return The insert statement string
	 */
	protected String getInsertStatement(Utils.Format format, int rsid) {
		return "VALUES (?, ?, ?)";
	}


	/**
	 * @brief Writes the information about network clusters
	 * @param clusters The found clusters
	 * @param major The major cluster
	 * @throws IOException When something fails
	 */
	public synchronized void writeClusters(Set<Set<DBEdge>> clusters) throws IOException {
		// convert to a sorted vector of sorted edge vectors 
		Vector<Vector<DBEdge>> vClusters = new Vector<>();
		for (Set<DBEdge> cluster : clusters) {
			Vector<DBEdge> v = new Vector<>();
			for (DBEdge e : cluster) {
				v.add(e);
			}
			v.sort(new Comparator<DBEdge>() { public int compare(DBEdge e1, DBEdge e2) { return e1.getID().compareTo(e2.getID()); } });
			vClusters.add(v);
		}
		vClusters.sort(new Comparator<Vector<DBEdge>>() {
		    public int compare(Vector<DBEdge> v1, Vector<DBEdge> v2) {
		        if(v1.size()==v2.size()) { return ((DBEdge) (v1.get(0))).getID().compareTo(((DBEdge) v2.get(0)).getID()); }
		    	return v1.size()<v2.size() ? 1 : -1;
		    }
		});
		// write
		int clusterID = 0;
		for(Vector<DBEdge> cluster : vClusters) {
			writeCluster(cluster, clusterID);
			++clusterID;
		}
		// close writing to the database
		if (intoDB()) {
			try {
				_ps.executeBatch();
				_connection.commit();
			} catch (SQLException ex) {
				throw new IOException(ex);
			}	
		}
	}


	/**
	 * @brief Writes the information about a single network cluster
	 * @param cluster The found clusters
	 * @param id The id of the cluster
	 * @throws IOException When something fails
	 */
	private synchronized void writeCluster(Vector<DBEdge> cluster, int id) throws IOException {
		for(DBEdge e : cluster) {
			if (intoDB()) {
				try {
					_ps.setString(1, e.getID());
					_ps.setInt(2, id);
					_ps.setInt(3, cluster.size());
					_ps.addBatch();
					++batchCount;
					if(batchCount>100) {
						_ps.executeBatch();
						_connection.commit();
						batchCount = 0;
					}
				} catch (SQLException ex) {
					throw new IOException(ex);
				}
			} else {
				_fileWriter.append(e.getID() + ";" + id + ";" + cluster.size() + "\n");
			}
		}
	}


}

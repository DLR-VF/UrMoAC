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

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraEntry;
import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.io.Utils;

/**
 * @class ProcessWriter
 * @brief Writes the computation times and basic metrics for single O/D-pairs
 * @author Daniel Krajzewicz
 */
public class ProcessWriter extends BasicCombinedWriter {
	/// @brief Counter of results added to the database / file so far
	private int batchCount = 0;

	
	/**
	 * @brief Constructor
	 * 
	 * Opens the connection to a PostGIS database and builds the table
	 * @param format The used format
	 * @param inputParts The definition of the input/output origin/destination
	 * @param precision The floating point precision to use
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @throws IOException When something fails
	 */
	public ProcessWriter(Utils.Format format, String[] inputParts, int precision, boolean dropPrevious) throws IOException {
		super(format, inputParts, "process-output", precision, dropPrevious, false,
				"(fid bigint, duration bigint, path_length bigint, seen_edges bigint, seen_nodes bigint)");
	}
	
	
	/** @brief Returns the insert statement string
	 * @param[in] format The used output format
	 * @param[in] epsg The used projection
	 * @return The insert statement string
	 */
	protected String getInsertStatement(Utils.Format format, int epsg) {
		return "VALUES (?, ?, ?, ?, ?)";
	}


	/**
	 * @brief Writes the "direct" representation of the result
	 * @param beg The time in nanoseconds the computation of this result began
	 * @param numSeenEdges The number of seen edges with destinations
	 * @param numSeenNodes The number of seen nodes
	 * @param mr The origin
	 * @param result The computation result (path)
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	public synchronized void write(long beg, long numSeenEdges, long numSeenNodes, MapResult mr, SingleODResult result) throws IOException {
		long originID = mr.em.getOuterID();
		long dur = System.nanoTime() - beg;
		long path_size = 0;
		DijkstraEntry c = result.path;
		do {
			++path_size;
			c = c.prev;
		} while(c!=null);
		
		if (intoDB()) {
			try {
				_ps.setLong(1, originID);
				_ps.setLong(2, dur);
				_ps.setLong(3, path_size);
				_ps.setLong(4, numSeenEdges);
				_ps.setLong(5, numSeenNodes);
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
			_fileWriter.append(originID + ";" + dur + ";" + path_size + ";" + numSeenEdges + ";" + numSeenNodes + "\n");
		}
	}


}

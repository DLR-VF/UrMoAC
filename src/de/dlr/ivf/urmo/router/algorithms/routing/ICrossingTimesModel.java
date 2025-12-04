/*
 * Copyright (c) 2024-2025
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
package de.dlr.ivf.urmo.router.algorithms.routing;

import java.io.IOException;

import de.dlr.ivf.urmo.router.shapes.DBEdge;

/** @interface ICrossingTimesModel
 * @brief An interface for models that add delays when crossing a road
 * @author Daniel Krajzewicz
 */
public interface ICrossingTimesModel {
	/** @brief Computes crossing times for a given starting edge
	 * 
	 * The computed crossing times are stored in the node the edge yields in
	 * 
	 * @param subjectEdge The regarded edge
	 */
	public void computeCrossingTimes(DBEdge subjectEdge) throws IOException;
	
	
	/** @brief Closes the used crossing times writer, if given
	 * 
	 */
	public void closeWriter() throws IOException;

	
}

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
package de.dlr.ivf.urmo.router.algorithms.edgemapper;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

/**
 * @interface EdgeMappable
 * @brief Something that can be mapped onto an edge of a road network
 * @author Daniel Krajzewicz
 */
public interface EdgeMappable {
	/**
	 * @brief Returns this thing's id
	 * @return This thing's id
	 */
	public long getOuterID();


	/**
	 * @brief Returns the position of this thing
	 * @return The point this thing is located at
	 */
	public Point getPoint();


	/**
	 * @brief Returns the envelope of this thing
	 * @return The bounding box
	 */
	public Envelope getEnvelope();


	/**
	 * @brief Returns the object's complete geometry
	 * @return The object's geometry
	 */
	public Geometry getGeometry();

}

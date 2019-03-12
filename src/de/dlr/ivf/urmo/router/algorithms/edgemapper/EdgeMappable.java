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
package de.dlr.ivf.urmo.router.algorithms.edgemapper;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @interface EdgeMappable
 * @brief Something that can be mapped onto an edge of a road network
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
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
	 * @brief Returns the object's complete geometry
	 * @return The object's geometry
	 */
	public Geometry getGeometry();

}

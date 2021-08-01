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

import com.infomatiq.jsi.Rectangle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;

/**
 * @class Layer
 * @brief An object layer
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of Transport Research
 */
public class Layer {
	/// @brief The layer's name
	String name;
	/// @brief Whether this layer is visible (unused!!!)
	public boolean visible;
	/// @brief List of objects stored in this layer
	Vector<EdgeMappable> objects;


	/**
	 * @brief Constructor
	 * @param _name The name of this layer
	 */
	public Layer(String _name) {
		name = _name;
		objects = new Vector<>();
		visible = true;
	}


	/**
	 * @brief Adds an object to the layer
	 * @param o The object to add
	 */
	public void addObject(LayerObject o) {
		// !!! update min/max
		objects.add(o);
		Geometry e = o.getGeometry().getEnvelope();
		Rectangle r;
		if (e instanceof Point) {
			Point p = (Point) e;
			r = new Rectangle((float) (p.getX() - 1.), (float) (p.getY() - 1.), (float) (p.getX() + 1.),
					(float) (p.getY() + 1.));
		} else {
			double minX = 0;
			double minY = 0;
			double maxX = 0;
			double maxY = 0;
			Coordinate cs[] = e.getCoordinates();
			for (int i = 0; i < cs.length; ++i) {
				Coordinate c = cs[i];
				if (i == 0 || minX > c.x)
					minX = c.x;
				if (i == 0 || minY > c.y)
					minY = c.y;
				if (i == 0 || maxX < c.x)
					maxX = c.x;
				if (i == 0 || maxY < c.y)
					maxY = c.y;
			}
			r = new Rectangle((float) (minX - 1.), (float) (minY - 1.), (float) (maxX + 1.), (float) (maxY + 1.));
		}
	}


	/**
	 * @brief Returns this layer's name
	 * @return This layer's name
	 */
	public String getName() {
		return name;
	}


	/**
	 * @brief Returns the objects stored in this layer
	 * @return Objects stored in this layer
	 */
	public Vector<EdgeMappable> getObjects() {
		return objects;
	}


	/**
	 * @brief Builds and returns the list of ids of allo objects stored in this layer
	 * @return List of stored obnjects' ids
	 */
	public Vector<String> getObjectIDs() {
		Vector<String> ids = new Vector<>();
		for (EdgeMappable o : objects) {
			ids.add(String.valueOf(o.getOuterID()));
		}
		return ids;
	}

}

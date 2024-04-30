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
package de.dlr.ivf.urmo.router.shapes;

import java.util.Vector;

import org.locationtech.jts.geom.Geometry;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;

/**
 * @class Layer
 * @brief An object layer
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of Transport Research
 */
public class Layer {
	/// @brief The layer's name
	private String name;
	/// @brief List of objects stored in this layer
	private Vector<EdgeMappable> objects;
	/// @brief The boundary to clip objects to
	private Geometry envelope;


	/**
	 * @brief Constructor
	 * @param _name The name of this layer
	 * @param _envelope The boundary to clip objects to
	 */
	public Layer(String _name, Geometry _envelope) {
		name = _name;
		objects = new Vector<>();
		envelope = _envelope;
		envelope = null;
	}


	/**
	 * @brief Adds an object to the layer
	 * 
	 * If a boundary was given, only objects which are (at least partially) within it, will be kept 
	 * @param o The object to add
	 */
	public void addObject(LayerObject o) {
		if(envelope!=null) {
			Geometry g = o.getGeometry();
			if(!g.within(envelope)&&!g.crosses(envelope)) {
				return;
			}
		}
		objects.add(o);
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

}

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

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;

/**
 * @class LayerObject
 * @brief An object that resides in a layer
 * 
 * The object consists of a unique id, the original (database) id, an
 * optionally attached value and a geometry (usually the position).
 * 
 * @author Daniel Krajzewicz
 */
public class LayerObject implements EdgeMappable {
	/// @brief The original id of the object (in the database, e.g.)
	private long outerID;
	/// @brief An optionally attached variable value
	private double attachedVar;
	/// @brief The object's geometry (usually its position)
	private Geometry geom;


	/**
	 * @brief Constructor
	 * @param _id The application's running object id
	 * @param _outerID The original id of the object (in the database, e.g.)
	 * @param _attachedVar An optionally attached variable value
	 * @param _geom The object's geometry (usually its position)
	 */
	public LayerObject(long _outerID, double _attachedVar, Geometry _geom) {
		outerID = _outerID;
		geom = _geom;
		attachedVar = _attachedVar;
	}


	/**
	 * @brief Returns the object's original id
	 * @return The object's original id
	 */
	@Override
	public long getOuterID() {
		return outerID;
	}


	/**
	 * @brief Returns the object's geometry
	 * @return The object's geometry
	 */
	@Override
	public Geometry getGeometry() {
		return geom;
	}


	/**
	 * @brief Returns the object's centroid
	 * @return The object's centroid
	 */
	@Override
	public Point getPoint() {
		return geom.getCentroid();
	}


	/**
	 * @brief Returns the envelope of this thing
	 * @return The bounding box
	 */
	public Envelope getEnvelope() {
		return geom.getEnvelopeInternal();
	}
	
	
	/**
	 * @brief Returns the object's attached value
	 * @return The object's attached value
	 */
	public double getAttachedValue() {
		return attachedVar;
	}
	
	
}

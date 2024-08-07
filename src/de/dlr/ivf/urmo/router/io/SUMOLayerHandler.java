/*
 * Copyright (c) 2016-2024
 * Institute of Transport Research
 * German Aerospace Center
 * 
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * Licensed under the GNU General Public License v3.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraße 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.io;

import java.util.StringTokenizer;
import java.util.Vector;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import de.dlr.ivf.urmo.router.shapes.IDGiver;
import de.dlr.ivf.urmo.router.shapes.Layer;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/** @class SUMOLayerHandler
 * @brief Parses a SUMO-POI-file reading it as objects
 * @author Daniel Krajzewicz
 */
public class SUMOLayerHandler extends DefaultHandler {
	/// @brief The layer to fill
	private Layer _layer;
	/// @brief The object to get internal IDs from
	private IDGiver _idGiver;
	/// @brief The geometry factory to use
	private GeometryFactory _gf;

	
	/** @brief Constructor
	 * @param layer The layer to add the read objects to
	 * @param idGiver Object that supports (running) IDs
	 */
	public SUMOLayerHandler(Layer layer, IDGiver idGiver) {
		_layer = layer;
		_idGiver = idGiver;
		_gf = new GeometryFactory(new PrecisionModel());
	}

	
	/** @brief Called when an element starts
	 * @param uri The element's URI
	 * @param localName The element's local name
	 * @param qName The element's qualified name
	 * @param attributes The element's attributes
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if(localName.equals("poi")) {
			String id = attributes.getValue("id");
			String xS = attributes.getValue("x");
			String yS = attributes.getValue("y");
			Geometry geom2 = _gf.createPoint(new Coordinate(Double.parseDouble(xS), Double.parseDouble(yS)));
			// @todo use string ids?
			_layer.addObject(new LayerObject(_idGiver.getNextRunningID(), Long.parseLong(id), 1, geom2));
		}
		if(localName.equals("poly")) {
			String id = attributes.getValue("id");
			Vector<Coordinate> geom = new Vector<>();
			String shape = attributes.getValue("shape");
			StringTokenizer st = new StringTokenizer(shape);
			while(st.hasMoreTokens()) {
				String pos = st.nextToken();
				String[] r = pos.split(",");
				geom.add(new Coordinate(Double.parseDouble(r[0]), Double.parseDouble(r[1])));
			}
			if(geom.get(0)!=geom.get(geom.size()-1)) {
				geom.add(geom.get(0));
			}
			Coordinate[] arr = new Coordinate[geom.size()];
			Geometry geom2 = _gf.createPolygon(geom.toArray(arr));
			// @todo use string ids?
			_layer.addObject(new LayerObject(_idGiver.getNextRunningID(), Long.parseLong(id), 1, geom2));
		}
	}

}


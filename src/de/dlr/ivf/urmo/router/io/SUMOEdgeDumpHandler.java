/*
 * Copyright (c) 2021-2023 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * Licensed under the GNU General Public License v3.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstra�e 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.io;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNet;

/** @class SUMOLayerHandler
 * @brief Parses a SUMO-edge dump-file 
 * @author Daniel Krajzewicz (c) 2021 German Aerospace Center, Institute of Transport Research
 */
public class SUMOEdgeDumpHandler extends DefaultHandler {
	/// @brief The net to fill
	private DBNet _net;
	/// @brief The current interval's starting time
	private float _intervalStart;
	/// @brief The current interval's ending time
	private float _intervalEnd;
	/// @brief The number of not mapped values
	private int _numFalse = 0;
	
	
	
	/** @brief Constructor
	 * @param net The net to add read edges to
	 * @param uModes The used modes of transport
	 */
	public SUMOEdgeDumpHandler(DBNet net) {
		_net = net;
	}


	/** @brief Called when an element starts
	 * @param uri The element's URI
	 * @param localName The element's local name
	 * @param qName The element's qualified name
	 * @param attributes The element's attributes
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if(qName.equals("interval")) {
			try {
				_intervalStart = Float.parseFloat(attributes.getValue("begin"));
			} catch(NumberFormatException e) {
				System.err.println("Can not parse interval begin.");
			}
			try {
				_intervalEnd = Float.parseFloat(attributes.getValue("end"));
			} catch(NumberFormatException e) {
				System.err.println("Can not parse interval end.");
			}
		}
		if(qName.equals("edge")) {
			String id = attributes.getValue("id");
			DBEdge edge = _net.getEdgeByName(id);
			if(edge==null) {
				++_numFalse;
				return;
			}
			try {
				float speed = Float.parseFloat(attributes.getValue("speed"));
				edge.addSpeedReduction(_intervalStart, _intervalEnd, speed);
			} catch(NumberFormatException e) {
				System.err.println("Can not parse speed information for edge '" + id + "'.");
			}
		}
	}


	/** @brief Returns the number of information for missing edges
	 * @return The number of not mapped information
	 */
	public int getNumFalse() {
		return _numFalse;
	}

}


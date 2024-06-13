/*
 * Copyright (c) 2021-2024
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

import java.util.Vector;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.DBNode;

/** @class SUMOLayerHandler
 * @brief Parses a SUMO-net-file 
 * @author Daniel Krajzewicz
 */
public class SUMONetHandler extends DefaultHandler {
	/// @brief The geometry factory to use
	GeometryFactory _gf;
	/// @brief The net to fill
	private DBNet _net;
	/// @brief The regarded modes of transport
	private long _uModes;
	/// @brief The network's offset
	Coordinate _offset = new Coordinate(0, 0);
	/// @brief Faster access to foot representation
	private long modeFoot = Modes.getMode("foot").id;
	/// @brief Faster access to bike representation
	private long modeBike = Modes.getMode("bike").id;
	/// @brief Faster access to car representation
	private long modeCar = Modes.getMode("car").id;

	
	

	/// @brief Temporary storage for edge attributes
	/// @{
	
	/// @brief The ID of the edge
	private String _id = null;
	/// @brief The ID of the begin node
	private String _from = null;
	/// @brief The ID of the end node
	private String _to = null;
	/// @brief The type of the edge
	private String _type = null;
	/// @brief The shape of the edge
	private String _shape = null;
	/// @brief The shapes of the edge's lanes
	private Vector<String> _laneShapes = null;
	/// @brief The list of modes allowed on each lane
	private Vector<String> _allowed = null;
	/// @brief The list of modes disallowed on each lane
	private Vector<String> _disallowed = null;
	/// @brief The maximum speed allowed on a lane of the edge
	private double _maxLaneSpeed = 0;
	/// @brief The sum of lanes' lengths
	private double _laneLengths = 0;
	/// @}
	
	
	/** @brief Constructor
	 * @param net The net to add read edges to
	 * @param uModes The used modes of transport
	 */
	public SUMONetHandler(DBNet net, long uModes) {
		_net = net;
		_uModes = uModes;
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
		if(qName.equals("location")) {
			String o = attributes.getValue("netOffset");
			String split[] = o.split(",");
			_offset = new Coordinate(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
		}
		if(qName.equals("edge")) {
			_laneShapes = new Vector<>();
			_allowed = new Vector<>();
			_disallowed = new Vector<>();
			_maxLaneSpeed = 0;
			_laneLengths = 0;
			_id = attributes.getValue("id");
			_from = attributes.getValue("from");
			_to = attributes.getValue("to");
			_shape = attributes.getValue("shape");
			_type = attributes.getValue("type");
		}
		if(qName.equals("lane")) {
			for(int i=0; i<attributes.getLength(); ++i) {
				_maxLaneSpeed = Math.max(_maxLaneSpeed,  Double.parseDouble(attributes.getValue("speed")));
				_laneLengths += Double.parseDouble(attributes.getValue("length"));
				_laneShapes.add(attributes.getValue("shape"));
				_allowed.add(attributes.getValue("allowed"));
				_disallowed.add(attributes.getValue("disallowed"));
			}
			// make sure that the allowed/disallowed information is given for each lane
			if(_allowed.size()<_laneShapes.size()) {
				_allowed.add(null);
			}
			if(_disallowed.size()<_laneShapes.size()) {
				_disallowed.add(null);
			}
		}
	}

	
	/** @brief Called when an element ends
	 * @param uri The element's URI
	 * @param localName The element's local name
	 * @param qName The element's qualified name
	 */
	@Override
	public void endElement(String uri, String localName, String qName) {
		if(qName.equals("edge")) {
			if(_type==null||_type.equals("normal")) {
				double length = _laneLengths / (double) _laneShapes.size();
				long modes = getModes();
				modes = (modes&Modes.customAllowedAt)!=0 ? modes | Modes.getMode("custom").id : modes;
				if(modes==0 || ((modes&_uModes)==0)) {
					return;
				}
				LineString geom2 = getShape();
				int numPoints = geom2.getNumPoints();
				for(int i=0; i<numPoints; ++i) {
					geom2.getCoordinateN(i).x = geom2.getCoordinateN(i).x - _offset.x; 
					geom2.getCoordinateN(i).y = geom2.getCoordinateN(i).y - _offset.y; 
				}				
				Coordinate[] cs = geom2.getCoordinates();
				DBNode fromNode = _net.getNode(_from, cs[0]);
				DBNode toNode = _net.getNode(_to, cs[cs.length - 1]);
				_net.addEdge(_id, fromNode, toNode, modes, _maxLaneSpeed, geom2, length);
			}
		}
	}
	
	
	/** @brief Parses the lanes' modes of transport
	 * @return The modes of transport that may be used on the current edge's lanes
	 */
	private long getModes() {
		long modes = 0;
		long allModes = modeFoot | modeBike | modeCar;
		for(int i=0; i<_laneShapes.size(); ++i) {
			long laneModes = 0;
			// parse allowed modes
			if(_allowed.get(i)==null) {
				laneModes = allModes;
			} else {
				String[] r = _allowed.get(i).split(" ");
				for(int j=0; j<r.length; ++j) {
					if("pedestrian".equals(r[j])) {
						laneModes |= modeFoot;
					}
					if("bicycle".equals(r[j])) {
						laneModes |= modeBike;
					}
					if("passenger".equals(r[j])) {
						laneModes |= modeCar;
					}
				}
			}
			// parse disallowed modes
			if(_disallowed.get(i)!=null) {
				String[] r = _disallowed.get(i).split(" ");
				for(int j=0; j<r.length; ++j) {
					if("pedestrian".equals(r[j])) {
						laneModes &= (~modeFoot);
					}
					if("bicycle".equals(r[j])) {
						laneModes &= (~modeBike);
					}
					if("passenger".equals(r[j])) {
						laneModes &= (~modeCar);
					}
				}
			}
			//
			modes |= laneModes;
		}
		return modes;
	}


	/** @brief Parses the lanes' shapes for constructing the edge shape
	 * @return The edge's shape
	 */
	private LineString getShape() {
		// use edge shape if given
		if(_shape!=null) {
			return parseShape(_shape);
		}
		// take the center lane if the lane number is odd
		if(_laneShapes.size()%2==1) {
			return parseShape(_laneShapes.get((_laneShapes.size()/2)));
		}
		// ok, parse both center shapes, take the center (that's an approximation only)
		// @todo 
		int index = (int) (_laneShapes.size()/2);
		LineString s1 = parseShape(_laneShapes.get(index));
		LineString s2 = parseShape(_laneShapes.get(index+1));
		int numPoints = s1.getNumPoints();
		Coordinate[] coords = new Coordinate[numPoints];
		for(int i=0; i<numPoints; ++i) {
			Coordinate c1 = s1.getCoordinateN(i);
			Coordinate c2 = s2.getCoordinateN(i);
			coords[i].x = (c1.x + c2.x) / 2.;
			coords[i].y = (c1.y + c2.y) / 2.;
		}
		return _gf.createLineString(coords);
	}	
	
	
	/** @brief Parses SUMO's shape definition string into a LineString
	 * @param shapeS A lane shape as encoded by SUMO
	 * @return The parsed shape
	 */
	private LineString parseShape(String shapeS) {
		String[] r = shapeS.split(" ");
		Coordinate[] coords = new Coordinate[r.length];
		for(int i=0; i<r.length; ++i) {
			String[] r2 = r[i].split(",");
			coords[i] = new Coordinate(Double.parseDouble(r2[0]), Double.parseDouble(r2[1]));
		}
		return _gf.createLineString(coords);
	}


}


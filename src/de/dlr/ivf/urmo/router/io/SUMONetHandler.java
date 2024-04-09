/*
 * Copyright (c) 2021-2022 DLR Institute of Transport Research
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
 * @author dkrajzew
 */
class SUMONetHandler extends DefaultHandler {
	GeometryFactory _gf;
	/// @brief The net to fill
	DBNet _net;
	long _uModes;
	long modeFoot = Modes.getMode("foot").id;
	long modeBike = Modes.getMode("bicycle").id;
	long modeCar = Modes.getMode("passenger").id;

	
	String _id = null;
	String _from = null;
	String _to = null;
	String _type = null;
	String _shape = null;
	String _spreadType = null;
	Vector<String> _laneShapes = null;
	Vector<String> _allowed = null;
	Vector<String> _disallowed = null;
	double _laneSpeeds = 0;
	double _laneLengths = 0;
	double _laneModes = 0;
	
	
	/** @brief Constructor
	 */
	public SUMONetHandler(DBNet net, long uModes) {
		_net = net;
		_uModes = uModes;
		_gf = new GeometryFactory(new PrecisionModel());
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if(localName.equals("edge")) {
			_laneShapes = new Vector<>();
			_allowed = new Vector<>();
			_disallowed = new Vector<>();
			_laneSpeeds = 0;
			_laneLengths = 0;
			_laneModes = 0;
			_id = null;
			_from = null;
			_to = null;
			_shape = null;
			_spreadType = null;
			_type = null;
			for(int i=0; i<attributes.getLength()&&(_id==null); ++i) {
				if(attributes.getLocalName(i).equals("id")) {
					_id = attributes.getValue(i);
				}
				if(attributes.getLocalName(i).equals("from")) {
					_from = attributes.getValue(i);
				}
				if(attributes.getLocalName(i).equals("to")) {
					_to = attributes.getValue(i);
				}
				if(attributes.getLocalName(i).equals("shape")) {
					_shape = attributes.getValue(i);
				}
				if(attributes.getLocalName(i).equals("type")) {
					_type = attributes.getValue(i);
				}
				if(attributes.getLocalName(i).equals("spreadType")) {
					_spreadType = attributes.getValue(i);
				}
			}
		}
		if(localName.equals("lane")) {
			for(int i=0; i<attributes.getLength()&&(_id==null); ++i) {
				if(attributes.getLocalName(i).equals("speed")) {
					_laneSpeeds += Double.parseDouble(attributes.getValue(i));
				}
				if(attributes.getLocalName(i).equals("length")) {
					_laneLengths += Double.parseDouble(attributes.getValue(i));
				}
				if(attributes.getLocalName(i).equals("shape")) {
					_laneShapes.add(attributes.getValue(i));
				}
				if(attributes.getLocalName(i).equals("allowed")) {
					_allowed.add(attributes.getValue(i));
				}
				if(attributes.getLocalName(i).equals("disallowed")) {
					_disallowed.add(attributes.getValue(i));
				}
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

	
	@Override
	public void endElement(String uri, String localName, String qName) {
		if(localName.equals("edge")) {
			if(_type==null||_type.equals("normal")) {
				double speed = _laneSpeeds / (double) _laneShapes.size();
				double length = _laneLengths / (double) _laneShapes.size();
				long modes = getModes();
				modes = (modes&Modes.customAllowedAt)!=0 ? modes | Modes.getMode("custom").id : modes;
				if(modes==0 && ((modes&_uModes)==0)) {
					return;
				}
				LineString geom2 = getShape();
				Coordinate[] cs = geom2.getCoordinates();
				DBNode fromNode = _net.getNode(_from, cs[0]);
				DBNode toNode = _net.getNode(_to, cs[cs.length - 1]);
				_net.addEdge(_net.getNextID(), _id, fromNode, toNode, modes, speed, geom2, length);
			}
		}
	}
	
	
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


	private LineString getShape() {
		// use edge shape if given
		if(_shape!=null) {
			return parseShape(_shape);
		}
		// take the only lane if only one exists
		if(_laneShapes.size()%2==1) {
			return parseShape(_laneShapes.get(0));
		}
		// take the center lane if the lane number is odd
		if(_laneShapes.size()%2==1) {
			int index = (int) (_laneShapes.size()/2) + 1;
			return parseShape(_laneShapes.get(index));
		}
		// ok, parse both center shapes, take the center (that's an approximation only)
		// @todo 
		int index = (int) (_laneShapes.size()/2);
		LineString s1 = parseShape(_laneShapes.get(index));
		return s1;
		//LineString s2 = parseShape(_laneShapes.get(index+1));
	}
	
	
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


package de.dlr.ivf.urmo.router.io;

import java.util.StringTokenizer;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.IDGiver;
import de.dlr.ivf.urmo.router.shapes.Layer;
import de.dlr.ivf.urmo.router.shapes.LayerObject;

/** @class SUMOLayerHandler
 * @brief Parses a SUMO-net-file 
 * @author dkrajzew
 */
class SUMONetHandler extends DefaultHandler {
	/// @brief The net to fill
	DBNet _net;
	
	
	String _id = null;
	String _from = null;
	String _to = null;
	String _type = null;
	String _shape = null;
	Vector<String> _laneShapes = null;
	double _laneSpeeds = 0;
	double _laneLengths = 0;
	double _laneModes = 0;
	
	
	/** @brief Constructor
	 * @param layer The layer to add the read objects to
	 */
	public SUMONetHandler(DBNet net) {
		_net = net;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if(localName.equals("edge")) {
			_laneShapes = new Vector<>();
			_laneSpeeds = 0;
			_laneLengths = 0;
			_laneModes = 0;
			_id = null;
			_from = null;
			_to = null;
			_shape = null;
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
					_to = attributes.getValue(i);
				}
				if(attributes.getLocalName(i).equals("disallowed")) {
					_to = attributes.getValue(i);
				}
			}
		}
	}

	
	@Override
	public void endElement(String uri, String localName, String qName) {
		if(localName.equals("edge")) {
			if(_type==null||_type.equals("normal")) {
				double speed = _laneSpeeds / (double) _laneShapes.size();
				double length = _laneLengths / (double) _laneShapes.size();
			}
		}
	}

}


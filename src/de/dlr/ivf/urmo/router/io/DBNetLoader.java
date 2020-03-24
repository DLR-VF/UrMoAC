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
package de.dlr.ivf.urmo.router.io;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import org.postgresql.PGConnection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.DBNode;

/**
 * @class DBNetLoader
 * @brief Loads the road network stored in the db
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class DBNetLoader {
	/**
	 * @brief Loads the road network from the database
	 * @return The loaded net
	 * @throws SQLException
	 * @throws ParseException
	 */
	public static DBNet loadNet(IDGiver idGiver, String url, String table, String user, String pw, int epsg) throws SQLException, ParseException {
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
		String query = "SELECT *,ST_AsBinary(ST_TRANSFORM(the_geom," + epsg + ")) FROM " + table + ";";
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery(query);
		WKBReader wkbRead = new WKBReader();
		DBNet net = new DBNet();
		long index = 0;
		while (rs.next()) {
			ResultSetMetaData rsmd = rs.getMetaData();
			//double length = rs.getDouble(rsmd.getColumnCount());
			Geometry geom = wkbRead.read(rs.getBytes(rsmd.getColumnCount()));
			// !!! hack - for some reasons, edge geometries are stored as MultiLineStrings in the database 
			if(geom.getNumGeometries()!=1) {
				System.err.println("Edge '' has a multi geometries...");
			}
			LineString geom2 = (LineString) geom.getGeometryN(0);
			
			Coordinate[] cs = geom2.getCoordinates();
			DBNode fromNode = net.getNode(rs.getLong("nodefrom"), cs[0]);
			DBNode toNode = net.getNode(rs.getLong("nodeto"), cs[cs.length - 1]);
			long modes = 0;//rs.getLong("modes");
			if(rs.getBoolean("mode_walk")) modes = modes | Modes.getMode("foot").id;
			if(rs.getBoolean("mode_bike")) modes = modes | Modes.getMode("bicycle").id;
			if(rs.getBoolean("mode_mit")) modes = modes | Modes.getMode("passenger").id;
			//if (modes == 4)
				//modes = modes | Modes.getMode("foot").id | Modes.getMode("bicycle").id | Modes.getMode("passenger").id;
			DBEdge e = new DBEdge(net.getNextID(), rs.getString("oid"), fromNode, toNode, modes, rs.getDouble("vmax") / 3.6, geom2, rs.getDouble("length"));
			net.addEdge(e);
			++index;
		}
		// add other directions to mode foot
		Vector<DBEdge> edges = net.getEdges();
		Vector<DBEdge> newEdges = new Vector<>();
		long modeFoot = Modes.getMode("foot").id;
		for(DBEdge e : edges) {
			DBNode to = e.getToNode();
			Vector<DBEdge> edges2 = to.getOutgoing();
			DBEdge opposite = null;
			for(DBEdge e2 : edges2) {
				if(e2.getToNode()==e.getFromNode() && Math.abs(e.length-e2.length)<1.) {
					// check whether the edges are parallel
					LineString eg = e.geom;
					boolean distant = false;
					for(int i=0; i<eg.getNumPoints()&&!distant; ++i) {
						if(e2.geom.distance(eg.getPointN(i))>.1) {
							distant = true;
						}
					}
					if(!distant) {
						// opposite direction found
						opposite = e2;
						if(!e2.allows(modeFoot)&&e.allows(modeFoot)) {
							e2.addMode(modeFoot);
						}
						break;
					}
				}
			}
			// add a reverse direction edge for pedestrians
			if(opposite==null && e.allows(modeFoot)) {
				opposite = new DBEdge(index, "opp_"+e.id, e.to, e.from, modeFoot, e.vmax, (LineString) e.geom.reverse(), e.length);
				newEdges.add(opposite);
				++index;
			}
			// add the information about the opposite edge
			if(opposite!=null) {
				opposite.opposite = e;
				e.opposite = opposite;
			}
		}
		edges.addAll(newEdges);
		return net;
	}

	public static int loadTravelTimes(DBNet net, String url, String table, String user, String pw, boolean verbose) throws SQLException, ParseException {
		int numFalse = 0;
		int numOk = 0;
		Connection connection = DriverManager.getConnection(url, user, pw);
		connection.setAutoCommit(true);
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		((PGConnection) connection).addDataType("geometry", org.postgis.PGgeometry.class);
		String query = "SELECT ibegin,iend,eid,speed FROM " + table + ";";
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery(query);
		while (rs.next()) {
			String eid = rs.getString("eid");
			DBEdge edge = net.getEdgeByName(eid);
			if(edge==null) {
				++numFalse;
				continue;
			}
			++numOk;
			float ibegin = rs.getFloat("ibegin");
			float iending = rs.getFloat("iend");
			float speed = rs.getFloat("speed");
			edge.addSpeedReduction(ibegin, iending, speed);
		}
		for(DBEdge e : net.getEdges()) {
			e.sortSpeedReductions();
		}
		if(verbose) {
			System.out.println(" " + numFalse + " of " + (numOk+numFalse) + " informations could not been loaded.");
		}
		return numFalse;
	}

}

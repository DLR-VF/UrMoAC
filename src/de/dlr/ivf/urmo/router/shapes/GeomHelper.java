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

import java.util.Vector;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

/** @class GeomHelper
 * @brief Some geometric help functions
 * @author Daniel Krajzewicz
 */
public class GeomHelper {
	/**
	 * @brief Returns the point at the linestring at the given distance
	 * @param ls The line string to get the point from
	 * @param distance The distance to get the point at
	 * @return The point at the given line string at the given distance
	 */
	public static Coordinate getPointAtDistance(LineString ls, double distance) {
	    double seenLength = 0;
		Coordinate tcoord[] = ls.getCoordinates();
	    int numPoints = ls.getNumPoints();
		for(int i=0; i<numPoints-1; ++i) {
			double nextLength = distance(tcoord[i], tcoord[i+1]);
			if (seenLength + nextLength > distance) {
				double offset = distance - seenLength;
				return new Coordinate(tcoord[i].x+(tcoord[i+1].x-tcoord[i].x)*offset/nextLength, tcoord[i].y+(tcoord[i+1].y-tcoord[i].y)*offset/nextLength, 0);
			}
			seenLength += nextLength;
		}
	    return tcoord[numPoints-1];
	}


	/**
	 * @brief Returns the part of the given line string until the given distance
	 * @param ls The line string to strip
	 * @param distance The distance at which the returns line string shall end
	 * @return The line string until the given distance
	 */
	public static LineString getGeomUntilDistance(LineString ls, double distance) {
		return getSubGeom(ls, 0, distance);
	}


	/**
	 * @brief Returns the part of the given line string that starts at the given distance
	 * @param ls The line string to strip
	 * @param distance The distance at which the returned part shall start
	 * @return The part of the lines string that starts at the given distance
	 */
	public static LineString getGeomBehindDistance(LineString ls, double distance) {
		return getSubGeom(ls, distance, ls.getLength());
	}

	
	/**
	 * @brief Returns the part of the given line string that starts and ends at the given distances
	 * @param ls The line string to strip
	 * @param beg The distance at which the returned part shall start
	 * @param end The distance at which the returned part shall end
	 * @return The part of the lines string that starts and ends at the given distances
	 */
	public static LineString getSubGeom(LineString ls, double beg, double end) {
		beg = clampDistance(ls, beg);
		end = clampDistance(ls, end);
		Vector<Coordinate> ncoord = new Vector<>();
		ncoord.add(getPointAtDistance(ls, beg));

		double seenLength = 0;
		Coordinate tcoord[] = ls.getCoordinates();
	    int i1 = 0;
	    int numPoints = ls.getNumPoints();
		for(; i1<numPoints-1; ++i1) {
			double nextLength = distance(tcoord[i1], tcoord[i1+1]);
			if(seenLength>beg) {
				ncoord.add(tcoord[i1]);
			}
			seenLength += nextLength;
			if(seenLength>=end) {
				ncoord.add(getPointAtDistance(ls, end));
				break;
			}
		}
		Coordinate coords[] = new Coordinate[ncoord.size()];
		for(int i=0; i<ncoord.size(); ++i) {
			coords[i] = ncoord.elementAt(i);
		}
		return ls.getFactory().createLineString(coords);
	}

	
	/**
	 * @brief Assure that the distance is not longer than the given line string's length
	 * @param ls The line string
	 * @param distance The distance
	 * @return A distance prunned to the line string's length
	 */
	private static double clampDistance(LineString ls, double distance) {
		return Math.min(ls.getLength(), Math.max(0, distance));
	}

	
	/**
	 * @brief Returns the euclidian distance between two points
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @return The distance between both points
	 */
	public static double distance(Point p1, Point p2) {
		double dx = p1.getX() - p2.getX();
		double dy = p1.getY() - p2.getY();
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	
	/**
	 * @brief Returns the euclidian distance between two coordinates
	 * @param p1 Coordinate 1
	 * @param p2 Coordinate 2
	 * @return The distance between both coordinates
	 */
	public static double distance(Coordinate p1, Coordinate p2) {
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	
	/**
	 * @brief Returns the distance to the given point from the given line
	 * @param lineStart The begin of the line
	 * @param lineEnd The end of the line
	 * @param p The point
	 * @param perpendicular Whether the point has to be perpendicular to the line
	 * @return The distance to the point, -1 if the point is not perpendicular but should be
	 */
	public static double getDistanceOnLine(Point lineStart, Point lineEnd, Point p, boolean perpendicular) {
		double lineLength2D = distance(lineStart, lineEnd);
		if(lineLength2D==0) {
			return 0;
		}
        double u = (((p.getX() - lineStart.getX()) * (lineEnd.getX() - lineStart.getX())) + ((p.getY() - lineStart.getY()) * (lineEnd.getY() - lineStart.getY())) ) / (lineLength2D * lineLength2D);
        if (u < 0.0f || u > 1.0f) {  // closest point does not fall within the line segment
        	if (perpendicular) {
        		return -1;
        	}
        	if (u < 0.0f) {
        		return 0.0f;
        	}
        	return lineLength2D;
        }
        return u * lineLength2D;
	}
	
	
	/**
	 * @brief Returns the distance to the given point from the given line
	 * @param lineStart The begin of the line
	 * @param lineEnd The end of the line
	 * @param p The point
	 * @param perpendicular Whether the point has to be perpendicular to the line
	 * @return The distance to the point, -1 if the point is not perpendicular but should be
	 */
	public static double getDistanceOnLine(Coordinate lineStart, Coordinate lineEnd, Coordinate p, boolean perpendicular) {
		double lineLength2D = distance(lineStart, lineEnd);
		if(lineLength2D==0) {
			return 0;
		}
        double u = (((p.x - lineStart.x) * (lineEnd.x - lineStart.x)) + ((p.y - lineStart.y) * (lineEnd.y - lineStart.y)) ) / (lineLength2D * lineLength2D);
        if (u < 0.0f || u > 1.0f) {  // closest point does not fall within the line segment
        	if (perpendicular) {
        		return -1;
        	}
        	if (u < 0.0f) {
        		return 0.0f;
        	}
        	return lineLength2D;
        }
        return u * lineLength2D;
	}
	
	
	/**
	 * @brief Returns the distance to the given point from the given line string
	 * @param e The DBEdge representation of the line
	 * @param point The point represented as a coordinate
	 * @param opivot The point represented as a point
	 * @return The distance to the point, -1 if the point is not perpendicular
	 */
	public static double getDistanceOnLineString(DBEdge e, Coordinate point, org.locationtech.jts.geom.Point opivot) {
		double minDist = -1;
		double pos = 0;
		double minPos = 0;
		LineString edgeGeom = e.getGeometry();
		int numPoints = edgeGeom.getNumPoints();
		Coordinate tcoord[] = edgeGeom.getCoordinates();
		Coordinate coord[] = new Coordinate[2];
		for(int i=0; i<numPoints-1; ++i) {
			coord[0] = tcoord[i];
			coord[1] = tcoord[i+1];
			LineString ls = edgeGeom.getFactory().createLineString(coord);
			double dist = opivot.distance(ls);
			if(minDist<0 || minDist>dist) {
				minDist = dist;
				double d2 = getDistanceOnLine(coord[0], coord[1], point, true);
				if(d2<0) {
					d2 = distance(coord[0], point)<distance(coord[1], point) ? 0 : distance(coord[0], coord[1]);
				}
				minPos = pos + d2;
			}
			pos += ls.getLength();
		}
		return Math.max(0, Math.min(e.getLength(), minPos));
	}
}

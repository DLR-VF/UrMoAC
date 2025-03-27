/*
 * Copyright (c) 2024-2025
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
package de.dlr.ivf.urmo.router.algorithms.routing;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import org.locationtech.jts.geom.Point;

import de.dlr.ivf.urmo.router.output.CrossingTimesWriter;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNode;

/** @class CrossingTimesModel
 * @brief A simple model for adding delays when crossing a road
 * @author Daniel Krajzewicz
 */
public class CrossingTimesModel_CTM1 implements ICrossingTimesModel {
	
	/** @class RelativeDirectionComparator
	 * @brief Sorts roads by their angle in relation to a given road
	 * @author Daniel Krajzewicz
	 */
	class RelativeDirectionComparator implements Comparator<DBEdge> {
		/// @brief A pre-computed map of relative angles
		HashMap<DBEdge, Double> relAngles;
		
		
		/** @brief Constructor
		 * @param _relAngles A pre-computed map of relative angles
		 */
		RelativeDirectionComparator(HashMap<DBEdge, Double> _relAngles) {
			relAngles = _relAngles;
		}
		
		
		/** @brief Performs the comparison
		 * @param e1 First edge
		 * @param e2 Second edge
		 */
		public int compare(DBEdge e1, DBEdge e2) {
			double rangle1 = relAngles.get(e1);
			double rangle2 = relAngles.get(e2);
			if(rangle1==rangle2) {
				return 0;
			}
			return rangle1<rangle2 ? -1 : 1;
		}
		
	}
	
	
	/// @brief An optional writer to save the computed crossing times
	private CrossingTimesWriter writer;
	
	
	/** @brief Constructor 
	 */
	public CrossingTimesModel_CTM1(CrossingTimesWriter _writer) {
		writer = _writer;
	}
	
		
	/** @brief Computes crossing times for a given starting edge
	 * 
	 * The computed crossing times are stored in the node the edge yields in
	 * 
	 * @param subjectEdge The regarded edge
	 * @throws IOException 
	 */
	public void computeCrossingTimes(DBEdge subjectEdge) throws IOException {
		DBNode n = subjectEdge.getToNode();
		double CROSSING_TIME = 10;
		// join incoming / outgoing
		Vector<DBEdge> all = new Vector<DBEdge>(n.getIncoming());
		all.addAll(n.getOutgoing());
		all.remove(subjectEdge);
		if(subjectEdge.getOppositeEdge()!=null) {
			all.remove(subjectEdge.getOppositeEdge());
		}
		double refAngle = getAngle(n, subjectEdge);
		// compute angles
		HashMap<DBEdge, Double> relAngles = new HashMap<DBEdge, Double>();
		relAngles.put(subjectEdge, 0.);
		for(DBEdge e : all) {
			double angle = getAngle(n, e);
		    double relAngle = angle - refAngle;
		    if (relAngle < 0) {
		    	relAngle = Math.PI*2. + relAngle;
		    }
		    relAngles.put(e, relAngle);
		}
		// sort from rightmost to leftmost
		Collections.sort(all, new RelativeDirectionComparator(relAngles));
		/*
		System.out.println(subjectEdge.getID() + " (" + refAngle + ")");
		for(DBEdge e : all) {
			System.out.println(e.getID() + ": " + relAngles.get(e));
		}
		System.out.println("----------------------------");
		*/
		// compute crossing times
		int crossed = 0;
		DBEdge last = subjectEdge;
		for(DBEdge e : all) {
			boolean isIncoming = e.getToNode()==subjectEdge.getToNode();
			double value = crossed * CROSSING_TIME;
			if(!isIncoming) {
				subjectEdge.setCrossingTimeTo(e, value);
				if(writer!=null) {
					writer.writeCrossingTime(subjectEdge.getID(), e.getID(), value);
				}
				System.out.println(e.getID() + ": " + value + " (" + crossed + ")");
			}
			if(last==null||last.getOppositeEdge()==e) {
				crossed += 1;
			}
			last = e;
		}
		if(subjectEdge.getOppositeEdge()!=null) {
			subjectEdge.setCrossingTimeTo(subjectEdge.getOppositeEdge(), 1 * CROSSING_TIME);
		}
		//System.out.println("============================");
	}


	/** @brief Returns the angle of the given edge at the given node
	 * 
	 * @param n The reference node
	 * @param e The edge to get the angle at the given node of
	 * @return The angle of the given edge at the given node
	 */
	private double getAngle(DBNode n, DBEdge e) {
		boolean isIncoming = e.getToNode()==n;
	    double angle = 0;
		if(isIncoming) {
			int numPoints = e.getGeometry().getNumPoints();
			Point pos1 = e.getGeometry().getPointN(numPoints-1);
			Point pos2 = e.getGeometry().getPointN(numPoints-2);
			angle = Math.atan2(pos1.getX()-pos2.getX(), pos1.getY()-pos2.getY());
		} else {
			Point pos1 = e.getGeometry().getPointN(0);
			Point pos2 = e.getGeometry().getPointN(1);
			angle = Math.atan2(pos1.getX()-pos2.getX(), pos1.getY()-pos2.getY());
		}
	    return angle;
	}


}

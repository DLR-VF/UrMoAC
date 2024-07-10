/*
 * Copyright (c) 2017-2024
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
package de.dlr.ivf.urmo.router.output.interchanges;

import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraEntry;
import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;
import de.dlr.ivf.urmo.router.gtfs.GTFSStop;
import de.dlr.ivf.urmo.router.output.MeasurementGenerator;

/**
 * @class InterchangeMeasuresGenerator
 * @brief Interprets a path to build an InterchangeSingleResult
 * @author Daniel Krajzewicz
 */
public class InterchangeMeasuresGenerator extends MeasurementGenerator<InterchangeSingleResult> {
	/**
	 * @brief Interprets the path to build an InterchangeSingleResult
	 * @param beginTime The start time of the path
	 * @param result The processed path between the origin and the destination
	 * @return An InterchangeSingleResult computed using the given path
	 */
	public InterchangeSingleResult buildResult(int beginTime, SingleODResult result) {
		DijkstraEntry current = result.path; //dr.getPath(to);//getEdgeInfo(to.edge);
		InterchangeSingleResult e = new InterchangeSingleResult(result.origin.em.getOuterID(), result.destination.em.getOuterID());
		
		do {
			DijkstraEntry next = current;
			current = current.prev;
			if(current==null) {
				continue;
			}
			if( (next.ptConnection==null&&current.ptConnection==null) || (next.ptConnection!=null && next.ptConnection.equals(current.ptConnection)&&next.usedMode.equals(current.usedMode)) ) {
				continue;
			}
			//++numInterchanges;
			String currentLine = current.buildLineModeID();
			String nextLine = next.buildLineModeID();
			String key = InterchangeSingleResult.buildLinesKey(currentLine, nextLine);
			String haltID = Long.toString(current.n.getID());
			if(current.n instanceof GTFSStop) {
				haltID = ((GTFSStop) current.n).mid;
			}
			e.addSingle(haltID, key, 1, 0);
		} while(current!=null);
		
		/*
		if(numInterchanges>5) {
			int num68 = 0;
			System.out.println("---------------------");
			current = dr.getEdgeInfo(to.edge);
			do {
				DijkstraEntry next = current;
				current = current.prev;
				if(current==null) {
					continue;
				}
				if(next.line.equals(current.line)) {
					continue;
				}
				String haltID = Long.toString(current.n.id);
				if(current.n instanceof GTFSStop) {
					haltID = ((GTFSStop) current.n).mid;
				}
				System.out.println(current.line + ";" + next.line + ";" + haltID);
				if("tram(68)".equals(current.line)||"tram(68)".equals(next.line)) {
					int bla = 0;
					bla = bla + 1;
					++num68;
				}
			} while(current!=null);
			if(num68>2) {
				int bla = 0;
				bla = bla + 1;
			}
			System.out.println("---------------------");
		}
		*/
		
		return e;
	}	
	
	
	/**
	 * @brief Builds an empty entry of type InterchangeSingleResult
	 * @param srcID The id of the origin the path started at
	 * @param destID The id of the destination accessed by this path
	 * @return An empty entry type InterchangeSingleResult
	 */
	public InterchangeSingleResult buildEmptyEntry(long srcID, long destID) {
		return new InterchangeSingleResult(srcID, destID);
	}

	
}

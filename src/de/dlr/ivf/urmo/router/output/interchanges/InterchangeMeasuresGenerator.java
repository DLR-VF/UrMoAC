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
package de.dlr.ivf.urmo.router.output.interchanges;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraEntry;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.gtfs.GTFSStop;
import de.dlr.ivf.urmo.router.output.MeasurementGenerator;

/**
 * @class InterchangeSingleResult
 * @brief Interprets a path to build an InterchangeSingleResult
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 * @param <T>
 */
public class InterchangeMeasuresGenerator extends MeasurementGenerator<InterchangeSingleResult> {
	/**
	 * @brief Interprets the path to build an InterchangeSingleResult
	 * @param beginTime The start time of the path
	 * @param from The origin the path started at
	 * @param to The destination accessed by this path
	 * @param dr The routing result
	 * @return An InterchangeSingleResult computed using the given path
	 */
	public InterchangeSingleResult buildResult(int beginTime, MapResult from, MapResult to, DijkstraResult dr) {
		DijkstraEntry current = dr.getEdgeInfo(to.edge);
		InterchangeSingleResult e = new InterchangeSingleResult(from.em.getOuterID(), to.em.getOuterID());
		
		//int numInterchanges = 0;
		
		do {
			DijkstraEntry next = current;
			current = current.prev;
			if(current==null) {
				continue;
			}
			if(next.line.equals(current.line)&&next.usedMode.equals(current.usedMode)) {
				continue;
			}
			//++numInterchanges;
			String currentLine = current.buildLineModeID();
			String nextLine = next.buildLineModeID();
			String key = InterchangeSingleResult.buildLinesKey(currentLine, nextLine);
			String haltID = Long.toString(current.n.id);
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

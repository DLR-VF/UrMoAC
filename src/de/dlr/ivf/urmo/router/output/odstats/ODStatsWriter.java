/**
 * Copyright (c) 2016-2021 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * @author: Daniel.Krajzewicz@dlr.de
 * Licensed under the GNU General Public License v3.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.output.odstats;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Vector;

import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;

/**
 * @class ODStatsWriter
 * @brief Writes ODSingleStatsResult results to a database / file
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class ODStatsWriter extends AbstractResultsWriter<ODSingleStatsResult> {
	/// @brief Counter of results added to the database / file so far
	private int batchCount = 0;
	
	/**
	 * @class Stats
	 * @brief Computes percentiles and mean / median from a vector of entries
	 * @author dkrajzew
	 *
	 */
	class Stats {
		/// @brief The average value
		double avg = 0;
		/// @brief The median value
		double med = 0;
		/// @brief The min value
		double min = 0;
		/// @brief The max value
		double max = 0;
		/// @brief The 15% percentile
		double p15 = 0;
		/// @brief The 85% percentile
		double p85 = 0;
		
		/**
		 * @brief Constructor
		 * 
		 * Computes the statistics
		 * @param from The verctor of entries to use
		 */
		public Stats(Vector<Double> from) {
			if(from.size()!=0) {
				Collections.sort(from);
				double sum = 0;
				for(Double d : from) {
					sum += d;
				}
				avg = sum / (double) from.size();
				med = from.elementAt((int) from.size() / 2);
				min = from.elementAt(0);
				max = from.lastElement();
				p15 = from.elementAt((int) ((double) from.size() * .15));
				p85 = from.elementAt((int) ((double) from.size() * .85));
			}
		}
	}
	
	
	/**
	 * @brief Constructor
	 * 
	 * Opens the connection to a database and builds the table
	 * @param url The URL to the database
	 * @param tableName The name of the table
	 * @param user The name of the database user
	 * @param pw The password of the database user
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @throws SQLException When something fails
	 */
	public ODStatsWriter(String url, String tableName, String user, String pw, boolean dropPrevious) throws SQLException {
		super(url, user, pw, tableName,
				"(fid bigint, sid bigint, num bigint, "
				+ "avg_distance real, avg_tt real, avg_value real, avg_kcal real, avg_price real, avg_co2 real, "
				+ "med_distance real, med_tt real, med_value real, med_kcal real, med_price real, med_co2 real, "
				+ "min_distance real, min_tt real, min_value real, min_kcal real, min_price real, min_co2 real, "
				+ "max_distance real, max_tt real, max_value real, max_kcal real, max_price real, max_co2 real, "
				+ "p15_distance real, p15_tt real, p15_value real, p15_kcal real, p15_price real, p15_co2 real, "
				+ "p85_distance real, p85_tt real, p85_value real, p85_kcal real, p85_price real, p85_co2 real "
				+ ")",
				"VALUES (?, ?, ?,  ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?)", 
				dropPrevious);
	}


	/**
	 * @brief Constructor
	 * 
	 * Opens the file to write the results to
	 * @param fileName The path to the file to write the results to
	 * @throws IOException When something fails
	 */
	public ODStatsWriter(String fileName) throws IOException {
		super(fileName);
	}

	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	@Override
	public void writeResult(ODSingleStatsResult result) throws SQLException, IOException {
		Stats distD = new Stats(result.allDistances);
		Stats ttD = new Stats(result.allTravelTimes);
		Stats valuesD = new Stats(result.allValues);
		Stats kcalsD = new Stats(result.allKCals);
		Stats pricesD = new Stats(result.allPrices);
		Stats CO2D = new Stats(result.allCO2s);
		if (intoDB()) {
			_ps.setLong(1, result.srcID);
			_ps.setLong(2, result.destID);
			_ps.setLong(3, result.allCO2s.size());
			insertIntoPS(_ps, distD, 4);
			insertIntoPS(_ps, ttD, 5);
			insertIntoPS(_ps, valuesD, 6);
			insertIntoPS(_ps, kcalsD, 7);
			insertIntoPS(_ps, pricesD, 8);
			insertIntoPS(_ps, CO2D, 9);
			_ps.addBatch();
			++batchCount;
			if(batchCount>10000) {
				_ps.executeBatch();
				batchCount = 0;
			}
		} else {
			_fileWriter.append(result.srcID + ";" + result.destID + ";" + result.allCO2s.size()
					+ ";" + distD.avg + ";" + ttD.avg + ";" + valuesD.avg + ";" + kcalsD.avg + ";" + pricesD.avg + ";" + CO2D.avg
					+ ";" + distD.med + ";" + ttD.med + ";" + valuesD.med + ";" + kcalsD.med + ";" + pricesD.med + ";" + CO2D.med
					+ ";" + distD.min + ";" + ttD.min + ";" + valuesD.min + ";" + kcalsD.min + ";" + pricesD.min + ";" + CO2D.min
					+ ";" + distD.max + ";" + ttD.max + ";" + valuesD.max + ";" + kcalsD.max + ";" + pricesD.max + ";" + CO2D.max
					+ ";" + distD.p15 + ";" + ttD.p15 + ";" + valuesD.p15 + ";" + kcalsD.p15 + ";" + pricesD.p15 + ";" + CO2D.p15
					+ ";" + distD.p85 + ";" + ttD.p85 + ";" + valuesD.p85 + ";" + kcalsD.p85 + ";" + pricesD.p85 + ";" + CO2D.p85
					+ "\n");
		}
	}

	
	/**
	 * @brief Inserts the given stats into the given prepared statement starting at the given index
	 * @param ps The prepared statement to add the stats to 
	 * @param distD The stats
	 * @param i The index
	 * @throws SQLException When something fails
	 */
	private void insertIntoPS(PreparedStatement ps, Stats distD, int i) throws SQLException {
		ps.setDouble(i+0*6, distD.avg);
		ps.setDouble(i+1*6, distD.med);
		ps.setDouble(i+2*6, distD.min);
		ps.setDouble(i+3*6, distD.max);
		ps.setDouble(i+4*6, distD.p15);
		ps.setDouble(i+5*6, distD.p85);
	}


	
}

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
package de.dlr.ivf.urmo.router.output.odstats;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Locale;
import java.util.Vector;

import de.dlr.ivf.urmo.router.io.Utils;
import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;

/**
 * @class ODStatsWriter
 * @brief Writes ODSingleStatsResult results to a database / file
 * @author Daniel Krajzewicz
 */
public class ODStatsWriter extends AbstractResultsWriter<ODSingleStatsResult> {
	/// @brief Counter of results added to the database / file so far
	private int batchCount = 0;
	
	/**
	 * @class Stats
	 * @brief Computes percentiles and mean / median from a vector of entries
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
		 * @param from The vector of entries to use
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
	 * Opens the connection to a PostGIS database and builds the table
	 * @param format The used format
	 * @param inputParts The definition of the input/output origin/destination
	 * @param precision The floating point precision to use
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @param haveTypes Whether destinations may have different types 
	 * @throws IOException When something fails
	 */
	public ODStatsWriter(Utils.Format format, String[] inputParts, int precision, boolean dropPrevious, boolean haveTypes) throws IOException {
		super(format, inputParts, "od-ext-stats", precision, dropPrevious, haveTypes,
				"(fid bigint, sid bigint, num bigint, "
				+ "avg_distance real, avg_tt real, avg_value real, avg_kcal real, avg_price real, avg_co2 real, "
				+ "med_distance real, med_tt real, med_value real, med_kcal real, med_price real, med_co2 real, "
				+ "min_distance real, min_tt real, min_value real, min_kcal real, min_price real, min_co2 real, "
				+ "max_distance real, max_tt real, max_value real, max_kcal real, max_price real, max_co2 real, "
				+ "p15_distance real, p15_tt real, p15_value real, p15_kcal real, p15_price real, p15_co2 real, "
				+ "p85_distance real, p85_tt real, p85_value real, p85_kcal real, p85_price real, p85_co2 real "
				+ ")");
	}


	/** @brief Get the insert statement string
	 * @param[in] format The used output format
	 * @param[in] epsg The used projection
	 * @return The insert statement string
	 */
	protected String getInsertStatement(Utils.Format format, int epsg) {
		return "VALUES (?, ?, ?,  ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?)";
	}

	
	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @param destType The type of the destination
	 * @throws IOException When something fails
	 */
	@Override
	public void writeResult(ODSingleStatsResult result, String destType) throws IOException {
		Stats distD = new Stats(result.allDistances);
		Stats ttD = new Stats(result.allTravelTimes);
		Stats valuesD = new Stats(result.allValues);
		Stats kcalsD = new Stats(result.allKCals);
		Stats pricesD = new Stats(result.allPrices);
		Stats CO2D = new Stats(result.allCO2s);
		if (intoDB()) {
			try {
				_ps.setLong(1, result.originID);
				_ps.setLong(2, result.destID);
				_ps.setLong(3, result.allCO2s.size());
				insertIntoPS(_ps, distD, 4);
				insertIntoPS(_ps, ttD, 5);
				insertIntoPS(_ps, valuesD, 6);
				insertIntoPS(_ps, kcalsD, 7);
				insertIntoPS(_ps, pricesD, 8);
				insertIntoPS(_ps, CO2D, 9);
				if(_haveTypes) {
					_ps.setString(45, destType);
				}
				_ps.addBatch();
				++batchCount;
				if(batchCount>10000) {
					_ps.executeBatch();
					batchCount = 0;
				}
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		} else {
			_fileWriter.append(Long.toString(result.originID)).append(";").append(Long.toString(result.destID)).append(";");
			_fileWriter.append(Integer.toString(result.allCO2s.size())).append(";");
			_fileWriter.append(String.format(Locale.US, _FS, distD.avg)).append(";").append(String.format(Locale.US, _FS, ttD.avg)).append(";").append(String.format(Locale.US, _FS, valuesD.avg)).append(";").append(String.format(Locale.US, _FS, kcalsD.avg)).append(";").append(String.format(Locale.US, _FS, pricesD.avg)).append(";").append(String.format(Locale.US, _FS, CO2D.avg)).append(";");
			_fileWriter.append(String.format(Locale.US, _FS, distD.med)).append(";").append(String.format(Locale.US, _FS, ttD.med)).append(";").append(String.format(Locale.US, _FS, valuesD.med)).append(";").append(String.format(Locale.US, _FS, kcalsD.med)).append(";").append(String.format(Locale.US, _FS, pricesD.med)).append(";").append(String.format(Locale.US, _FS, CO2D.med)).append(";");
			_fileWriter.append(String.format(Locale.US, _FS, distD.min)).append(";").append(String.format(Locale.US, _FS, ttD.min)).append(";").append(String.format(Locale.US, _FS, valuesD.min)).append(";").append(String.format(Locale.US, _FS, kcalsD.min)).append(";").append(String.format(Locale.US, _FS, pricesD.min)).append(";").append(String.format(Locale.US, _FS, CO2D.min)).append(";");
			_fileWriter.append(String.format(Locale.US, _FS, distD.max)).append(";").append(String.format(Locale.US, _FS, ttD.max)).append(";").append(String.format(Locale.US, _FS, valuesD.max)).append(";").append(String.format(Locale.US, _FS, kcalsD.max)).append(";").append(String.format(Locale.US, _FS, pricesD.max)).append(";").append(String.format(Locale.US, _FS, CO2D.max)).append(";");
			_fileWriter.append(String.format(Locale.US, _FS, distD.p15)).append(";").append(String.format(Locale.US, _FS, ttD.p15)).append(";").append(String.format(Locale.US, _FS, valuesD.p15)).append(";").append(String.format(Locale.US, _FS, kcalsD.p15)).append(";").append(String.format(Locale.US, _FS, pricesD.p15)).append(";").append(String.format(Locale.US, _FS, CO2D.p15)).append(";");
			_fileWriter.append(String.format(Locale.US, _FS, distD.p85)).append(";").append(String.format(Locale.US, _FS, ttD.p85)).append(";").append(String.format(Locale.US, _FS, valuesD.p85)).append(";").append(String.format(Locale.US, _FS, kcalsD.p85)).append(";").append(String.format(Locale.US, _FS, pricesD.p85)).append(";").append(String.format(Locale.US, _FS, CO2D.p85));
			if(_haveTypes) {
				_fileWriter.append(";").append(destType);
			}
			_fileWriter.append("\n");
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

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
package de.dlr.ivf.urmo.router.output.edge_use;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

import de.dlr.ivf.urmo.router.io.Utils;
import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;
import de.dlr.ivf.urmo.router.output.edge_use.EUSingleResult.EdgeParam;

/**
 * @class EUWriter
 * @brief Writes EUSingleResult results to a database / file
 * @author Daniel Krajzewicz
 */
public class EUWriter extends AbstractResultsWriter<EUSingleResult> {
	/// @brief Counter of results added to the database / file so far
	private int batchCount = 0;
	
	
	/**
	 * @brief Constructor
	 * 
	 * Opens the connection to a PostGIS database and builds the table
	 * @param format The used format
	 * @param inputParts The definition of the input/output source/destination
	 * @param precision The floating point precision to use
	 * @param dropPrevious Whether a previous table with the name shall be dropped 
	 * @param haveTypes Whether destinations may have different types 
	 * @throws IOException When something fails
	 */
	public EUWriter(Utils.Format format, String[] inputParts, int precision, boolean dropPrevious, boolean haveTypes) throws IOException {
		super(format, inputParts, "edges-output", precision, dropPrevious, haveTypes,
				"(fid bigint, sid bigint, eid text, num real, origins_weight real, normed real)");
	}


	/** @brief Get the insert statement string
	 * @param[in] format The used output format
	 * @param[in] epsg The used projection
	 * @return The insert statement string
	 */
	protected String getInsertStatement(Utils.Format format, int epsg) {
		return "VALUES (?, ?, ?, ?, ?, ?)";
	}


	/** 
	 * @brief Writes the results to the open database / file
	 * @param result The result to write
	 * @param destType The type of the destination
	 * @throws IOException When something fails
	 */
	@Override
	public void writeResult(EUSingleResult result, String destType) throws IOException {
		if (intoDB()) {
			try {
				for(String id : result.stats.keySet()) {
					EdgeParam ep = result.stats.get(id);
						_ps.setLong(1, result.originID);
						_ps.setLong(2, result.destID);
						_ps.setString(3, id);
						_ps.setFloat(4, (float) ep.num);
						_ps.setFloat(5, (float) ep.originsWeight);
						_ps.setFloat(6, (float) (ep.num / ep.originsWeight));
						if(_haveTypes) {
							_ps.setString(7, destType);
						}
						_ps.addBatch();
						++batchCount;
						if(batchCount>10000) {
							_ps.executeBatch();
							batchCount = 0;
						}
				}
			} catch (SQLException ex) {
				throw new IOException(ex);
			}
		} else {
			for(String eid : result.stats.keySet()) {
				EdgeParam ep = result.stats.get(eid);
				_fileWriter.append(Long.toString(result.originID)).append(";").append(Long.toString(result.destID)).append(";").append(eid).append(";");
				_fileWriter.append(String.format(Locale.US, _FS, ep.num)).append(";");
				_fileWriter.append(String.format(Locale.US, _FS, ep.originsWeight)).append(";");
				_fileWriter.append(String.format(Locale.US, _FS, (ep.num / ep.originsWeight)));
				if(_haveTypes) {
					_fileWriter.append(";").append(destType);
				}
				_fileWriter.append("\n");
			}
		}
	}

}

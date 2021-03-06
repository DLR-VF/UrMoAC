/**
 * Copyright (c) 2016-2020 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * @author: Daniel.Krajzewicz@dlr.de
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

import java.io.IOException;

public class Utils {
	/**
	 * @brief Parses the definition of the output storage and verfies it
	 * @param input The definition of an output storage (database / file)
	 * @param outputName The name of the definition
	 * @return The split (and verified) definition
	 * @throws IOException When something fails
	 */
	public static String[] checkDefinition(String input, String outputName) throws IOException {
		String[] r = input.split(";");
		if (r[0].equals("db")) {
			if(r.length!=5) {
				throw new IOException("False database definition for '" + outputName + "'\nshould be 'db;<DB_HOST>;<SCHEMA.TABLE>;<USER>;<PASSWORD>'.");
			}
		} else {
			if(r.length!=2) {
				throw new IOException("False file definition for '" + outputName + "'\nshould be 'file;<FILENAME>'.");
			}
		}
		return r;
	}

}

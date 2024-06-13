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
package de.dlr.ivf.urmo.router.io;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.locationtech.jts.geom.Geometry;

import de.dks.utils.options.OptionsCont;
import de.dlr.ivf.urmo.router.gtfs.GTFSData;
import de.dlr.ivf.urmo.router.modes.EntrainmentMap;
import de.dlr.ivf.urmo.router.shapes.DBNet;

/**
 * @class GTFSReader
 * @brief Reads a GTFS plan from a DB or a file
 * @author Daniel Krajzewicz
 */
public class GTFSLoader {
	/** @brief Loads GTFS data from a database or a file
	 * @param options The options to read the input definition from
	 * @param bounds A bounding box for prunning read information
	 * @param net The used network
	 * @param entrainmentMap The used entrainment map
	 * @param epsg The used projection
	 * @param numThreads The number of threads to use when mapping positions to edges
	 * @param verbose Whether additional information shall be printed
	 * @return The loaded GTFS data
	 * @throws IOException When something fails
	 */
	public static GTFSData load(OptionsCont options, Geometry bounds, DBNet net, EntrainmentMap entrainmentMap, int epsg, int numThreads, boolean verbose) throws IOException {
		if(!options.isSet("date")) {
			throw new IOException("A date must be given when using GTFS.");
		}
		String def = options.getString("pt");
		Utils.Format format = Utils.getFormat(def);
		if(format==Utils.Format.FORMAT_UNKNOWN) {
			File f = new File(def + "stops.txt");
			if(f.exists() && !f.isDirectory()) {
				format = Utils.Format.FORMAT_CSV;
			}
		}
		String[] inputParts = Utils.getParts(format, def, "pt");
		Vector<Integer> allowedCarrier = options.isSet("pt-restriction") ? parseCarrierDef(options.getString("pt-restriction")) : new Vector<>();
		AbstractGTFSReader reader = null;
		switch(format) {
		case FORMAT_POSTGRES:
		case FORMAT_SQLITE:
			reader = new GTFSReader_DB(net, epsg, options.getString("date"), allowedCarrier);
			break;
		case FORMAT_CSV:
			reader = new GTFSReader_File(net, epsg, options.getString("date"), allowedCarrier);
			break;
		case FORMAT_SHAPEFILE:
		case FORMAT_SUMO:
		case FORMAT_GEOPACKAGE:
			throw new IOException("Reading GTFS from " + Utils.getFormatMMLName(format) + " is not supported.");
		default:
			throw new IOException("Could not recognize the format used for GTFS or the path is not correct.");
		}
		// load
		return reader.load(format, inputParts, bounds, entrainmentMap, numThreads, verbose);
	}
	
	
	
	/** @brief Parses the definition of pt carriers to load
	 * @param carrierDef The definition to parse
	 * @return The list of carriers to load
	 * @throws IOException 
	 */
	private static Vector<Integer> parseCarrierDef(String carrierDef) throws IOException {
		if("".equals(carrierDef)) {
			return null;
		}
		// catching deprecated divider
		String[] r = null;
		if(carrierDef.contains(";")&&!carrierDef.contains(",")) {
			System.err.println("Warning: Using ';' as divider is deprecated, please use ','.");
			r = carrierDef.split(";");
		} else {
			r = carrierDef.split(",");
		}
		//
		Vector<Integer> allowedCarrier = new Vector<>();
		for(String r1 : r) {
			try {
				allowedCarrier.add(Integer.parseInt(r1));
			} catch(NumberFormatException e) {
				throw new IOException("Carrier definition should contain numeric values.");
			}
		}
		if(allowedCarrier.size()==0) {
			allowedCarrier = null;
		}
		return allowedCarrier;
	}
	
	

}

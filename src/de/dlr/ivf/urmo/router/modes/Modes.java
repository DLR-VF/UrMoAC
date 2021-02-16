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
 * Rutherfordstraße 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.modes;

import java.util.HashMap;
import java.util.Vector;

/**
 * @class Modes
 * @brief Known modes of transport
 * @author Daniel Krajzewicz
 * (c) 2016 German Aerospace Center, Institute of Transport Research
 *         
 * kkcPerHour: 
 * * http://www.herz.at/meinlebensstil/Lifestyle/Tabelle_Kalorienverbrauch_bei_koerperlicher_Taetigkeit.htm
 * * http://gesuender-abnehmen.com/abnehmen/kalorienverbrauch-ruhe-passiv.html
 * * Annahme: 85kg 
 * co2PerKm: 
 * * https://www.vcd.org/themen/klimafreundliche-mobilitaet/verkehrsmittel-im-vergleich/
 * * http://www.co2-emissionen-vergleichen.de/verkehr/CO2-PKW-Bus-Bahn.html        
 */
public class Modes {
	/// @brief A (static) map of mode names to modes
	static public HashMap<String, Mode> mml2mode;
	/// @brief A (static) map of mode ids to modes
	static public HashMap<Long, Mode> id2mode;
	/// @brief The modes as a list
	static public Vector<Mode> modes;


	/**
	 * @brief Initialises the known modes information
	 */
	public static void init() {
		mml2mode = new HashMap<>();
		id2mode = new HashMap<>();
		modes = new Vector<>();

		// costs: Eisenmann, Christine und Kuhnimhof, Tobias (2017) Vehicle cost imputation in travel surveys: Gaining insight into the fundamentals of (auto-) mobility choices. 11th International Conference on Transport Survey Methods, 24.-29. Sept. 2017, Estérel, Kanada. 
		add(new Mode(1, "passenger", 200, 500, 170, 150, 31)); // kcal: 16010
		add(new Mode(2, "bus", 80, 500, 85, 75, 0)); // kcal: 16016
		add(new Mode(4, "foot", 3.6, 50, 280, 0, 0)); // kcal: 17190
		add(new Mode(8, "bicycle", 13, 300, 510, 0, 0)); // kcal: 1020
		add(new Mode(16, "motorcycle", 200, 300, 0, 0, 0));
		add(new Mode(32, "delivery", 200, 500, 170, 150, 0));
		add(new Mode(64, "hgv", 120, 500, 0, 0, 0));
		add(new Mode(128, "psv", 80, 100, 0, 0, 0));

		add(new Mode(256, "horse", 20, 100, 0, 0, 0));
		add(new Mode(512, "moped", 200, 500, 0, 0, 0));
		add(new Mode(1024, "emergency", 200, 500, 0, 0, 0));
		add(new Mode(2048, "rail", 200, 500, 0, 0, 0));
		add(new Mode(4096, "tram", 200, 500, 0, 0, 0));
		add(new Mode(8192, "taxi", 200, 500, 0, 0, 0));
		add(new Mode(16384, "ski", 200, 500, 0, 0, 0));
		add(new Mode(32768, "inline skates", 200, 500, 0, 0, 0));

		add(new Mode(65536, "ice skates", 200, 500, 0, 0, 0));
		add(new Mode(131072, "carriage", 200, 500, 0, 0, 0));
		add(new Mode(262144, "trailer", 200, 500, 0, 0, 0));
		add(new Mode(524288, "caravan", 200, 500, 0, 0, 0));
		add(new Mode(1048576, "mofa", 200, 500, 0, 0, 0));
		add(new Mode(2097152, "motorhome", 200, 500, 0, 0, 0));
		add(new Mode(4194304, "tourist bus", 200, 500, 0, 0, 0));
		add(new Mode(8388608, "goods", 200, 500, 0, 0, 0));
		add(new Mode(16777216, "agricultural", 200, 500, 0, 0, 0));
		add(new Mode(33554432, "atv", 200, 500, 0, 0, 0));
		add(new Mode(67108864, "snowmobile", 200, 500, 0, 0, 0));
		add(new Mode(134217728, "hov", 200, 500, 0, 0, 0));
		add(new Mode(268435456, "car sharing", 200, 500, 0, 0, 0));
		add(new Mode(536870912, "hazmat", 200, 500, 0, 0, 0));
		add(new Mode(1073741824, "disabled", 200, 500, 0, 0, 0));
		add(new Mode(2147483648L, "closed", 200, 500, 0, 0, 0));
	}


	/**
	 * @brief Adds a mode
	 * @param m The mode to add
	 */
	public static void add(Mode m) {
		mml2mode.put(m.mml, m);
		id2mode.put(m.id, m);
		modes.add(m);
	}


	/**
	 * @brief Returns an array of the names of all known modes
	 * @return The names of all known modes
	 */
	public static String[] getModeNames() {
		String[] ret = new String[modes.size()];
		int i = 0;
		for (Mode m : modes) {
			ret[i++] = m.mml;
		}
		return ret;
	}


	/**
	 * @brief Returns the mode with the given id
	 * @param id The id of the mode to retrieve
	 * @return The mode
	 */
	public static Mode getMode(long id) {
		return id2mode.get(id);
	}


	/**
	 * @brief Returns the named mode
	 * @param mml The name of the mode
	 * @return The named mode
	 */
	public static Mode getMode(String mml) {
		return mml2mode.get(mml);
	}
	
	
	/**
	 * @brief Returns a long that with all mode ids set
	 * @param modes The modes to convert
	 * @return The mode ids to convert
	 */
	public static long getCombinedModeIDs(Vector<Mode> modes) {
		long ret = 0;
		for(Mode m : modes) {
			ret |= m.id;
		}
		return ret;
	}


	
	/**
	 * @brief Chooses the next mode to use
	 * 
	 * The fastest mode is chosen
	 * @param availableModes
	 * @return The chosen mode
	 * @todo Play with this
	 */
	public static Mode selectModeFrom(long availableModes) {
		Mode selected = null;
		double vmax = 0;
		long modeID = 1;
		for(int i=0; i<32; ++i) {
			if((modeID&availableModes)!=0) {
				Mode m = getMode(modeID);
				if(vmax<m.vmax) {
					vmax = m.vmax;
					selected = m;
				}
			}
			modeID *= 2;
			if(modeID>availableModes) {
				break;
			}
		}
		return selected;
	}

}

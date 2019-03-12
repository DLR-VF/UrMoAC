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
package de.dlr.ivf.helper.options;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

/**
 * @class OptionsHelper
 * @brief Some methods for dealing with org.apache.commons.cli.CommandLine
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of
 *         Transport Research
 */
public class OptionsHelper {
	/**
	 * @brief Checks whether the given option was set, reporting if not
	 * @return Whether the named option was set by the user
	 */
	public static boolean isSet(CommandLine lvCmd, String optionName) {
		if (!lvCmd.hasOption(optionName)) {
			System.err.println("The '" + optionName + "' parameter is missing");
			return false;
		}
		return true;
	}


	/**
	 * @brief Checks for the named option whether it is an integer - if set
	 * @return Whether the named option is unset or a valid integer value
	 */
	public static boolean isIntegerOrUnset(CommandLine lvCmd, String optionName) {
		if (!lvCmd.hasOption(optionName)) {
			return true;
		}
		try {
			((Long) lvCmd.getParsedOptionValue(optionName)).intValue();
			return true;
		} catch (ParseException e) {
			System.err.println("The value of the '" + optionName + "'-parameter must be an integer.");
			return false;
		}
	}


	/**
	 * @brief Checks for the named option whether it is an double - if set
	 * @return Whether the named option is unset or a valid double value
	 */
	public static boolean isDoubleOrUnset(CommandLine lvCmd, String optionName) {
		if (!lvCmd.hasOption(optionName)) {
			return true;
		}
		try {
			((Double) lvCmd.getParsedOptionValue(optionName)).doubleValue();
			return true;
		} catch (ParseException e) {
			System.err.println("The value of the '" + optionName + "'-parameter must be a double.");
			return false;
		}
	}
}

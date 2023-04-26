package de.dks.utils.options;

import java.util.Iterator;
import java.util.Vector;

/**
 * @class OptionsParser
 * @brief Static helpers for parsing options from command line.
 * @author Daniel Krajzewicz (daniel@krajzewicz.de)
 * @copyright Eclipse Public License v2.0 (EPL v2.0), (c) Daniel Krajzewicz 2004-2021
 */
public class OptionsParser {
    /** @brief Parses the given options into the given container
     * @param[in] into The options container to fill
     * @param[in] args The arguments given on the command line
     * @param[in] continueOnError Continues even if an error occures while parsing
     * @return Whether parsing was successful
     */
    public static boolean parse(OptionsCont into, String[] args, boolean continueOnError) {
        boolean ok = true;
        for(int pos=0; pos<args.length;) {
            // try to parse a token combination
            int add = parse(into, args, pos);
            // check whether an error occured
            if(add<0) {
                // error occured
                ok = false;
                // maybe we want to continue although an error occured
                if(continueOnError) {
                    pos += -add;
                } else {
                    return false;
                }
            } else {
                // ok, go on with the next combination
                pos += add;
            }
        }
        // return whether parsing had errors or not
        return ok;
    }


    /** @brief Parses a single option into the container
     * @param[in] into The options container to fill
     * @param[in] args The arguments given on the command line
     * @param[in] pos The current position within the arguments to parse from
     * @return The number of arguments to proceed
     */
    private static int parse(OptionsCont into, String[] args, int pos) {
        // an option name indicator must have at least two characters
        if(args[pos].length()>=2) {
            if(args[pos].charAt(0)=='-'&&args[pos].charAt(1)!='-') {
                // the next combination is an abbreviation
                return parseAbbreviation(into, args, pos);
            } else if(args[pos].charAt(0)=='-'&&args[pos].charAt(1)=='-') {
                // the next combination is a full name argument
                return parseFull(into, args, pos);
            }
        }
        // no option
        String msg = "Unrecognized option '" + args[pos] + "'.";
        if(pos>2&&args[pos-1].charAt(0)=='-'&&args[pos-2].charAt(0)=='-') {
            msg = msg + "\n Propably forgot a parameter for '" + args[pos-2] + "'.";
        }
        // !!! what kind of an exception should be thrown?
        throw new RuntimeException(msg);
    }


    /** @brief Parses a single, abbreviated option into the container
     * @param[in] into The options container to fill
     * @param[in] args The arguments given on the command line
     * @param[in] pos The current position within the arguments to parse from
     * @return The number of arguments to proceed
     */
    private static int parseAbbreviation(OptionsCont into, String[] args, int pos) {
        String options = args[pos].substring(1);
        int len = options.length();
        Vector<String> usingParameter = new Vector<>();
        // go through the combination
        int i = 0;
        for(; i<len&&options.charAt(i)!='='; ++i) {
            // check whether the name is a bool
            if(!into.isBool("" + options.charAt(i))) {
                // if not, then add it to the list of options that need a parameter
                usingParameter.add("" + options.charAt(i));
            } else {
                // otherwise simply set it
                into.set("" + options.charAt(i), true);
            }
        }
        // check options that need a parameter
        if(usingParameter.size()==0) {
            // if no one was needed, return ok for parsing
            return 1;
        }
        if(usingParameter.size()>1) {
            // if there is more than one report an error
            StringBuffer sb = new StringBuffer();
            sb.append("All of the following options need a value: ");
            for (Iterator<String> j=usingParameter.iterator(); j.hasNext(); ) {
                sb.append(j.next());
                if(j.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append("\n");
            throw new RuntimeException(sb.toString()); // !!! exception type
        }
        // assume one token is used
        int tokens = 1;
        // otherwise (one needed a parameter)
        String param = "";
        if(i<options.length() && options.charAt(i)=='=') {
            // maybe the parameter is within the string
            param = options.substring(i+1);
        }
        if(param.length()==0 && pos+1>=args.length) {
            // no parameter was given, report an error
            throw new RuntimeException("Option '" + usingParameter.firstElement() + "' needs a value."); // !!! exception type
        }
        if(param.length()==0) {
            // use the next token as option value
            param = args[pos+1];
            tokens = 2;
        }
        // ok
        try {
        	into.set(usingParameter.firstElement(), param);
        } catch(NumberFormatException e) {
            throw new RuntimeException("The parameter for option '" + usingParameter.firstElement() + "' must be of " + into.getTypeName(usingParameter.firstElement()) + " type.");
        }
        return tokens;
    }


    /** @brief Parses a single, fully-named option into the container
     * @param[in] into The options container to fill
     * @param[in] args The arguments given on the command line
     * @param[in] pos The current position within the arguments to parse from
     * @return The number of arguments to proceed
     */
    private static int parseFull(OptionsCont into, String[] args, int pos) {
        String option = args[pos].substring(2);
        String value = "";
        // check whether the value is given within the same token
        int idx = option.indexOf('=');
        if(idx>=0) {
            value = option.substring(idx+1);
            option = option.substring(0, idx);
        }
        // check whether it is a boolean option
        if(into.isBool(option)) {
            if(value!="") {
                // if a value has been given, inform the user
                throw new RuntimeException("Option '" + option + "' does not need a value.");// !!! exception type
            }
            into.set(option, true);
            return 1;
        }
        // otherwise (parameter needed)
        if(!value.equals("")) {
            // ok, value was given within the same token
            try {
            	into.set(option, value);
            } catch(NumberFormatException e) {
                throw new RuntimeException("The parameter for option '" + option + "' must be of " + into.getTypeName(option) + " type.");
            }
            return 1;
        }
        if(pos+1>=args.length) {
            // there is no further parameter, report an error
            throw new RuntimeException("Parameter '" + option + "' needs a value.");
        }
        // ok, use the next one
        try {
        	into.set(option, args[pos+1]);
        } catch(NumberFormatException e) {
            throw new RuntimeException("The parameter for option '" + option + "' must be of " + into.getTypeName(option) + " type.");
        }
        return 2;
    }


}

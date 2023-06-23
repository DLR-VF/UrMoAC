package de.dks.utils.options;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * @class OptionsIO
 * @brief Static helper methods for parsing and loading of options.
 * @author Daniel Krajzewicz (daniel@krajzewicz.de)
 * @copyright Eclipse Public License v2.0 (EPL v2.0), (c) Daniel Krajzewicz 2004-2021
 */
public class OptionsIO {
    /** @brief Parses options from the command line and optionally loads options from a configuration file
     * @param[in] into The options container to fill
     * @param[in] args The arguments given on the command line
     * @param[in] fileIO The file reading handler to use
     * @param[in] configOptionName The path to the configuration to load
     * @param[in] continueOnError Continues even if an error occurs while parsing
     * @param[in] acceptUnknown Unknown options do not throw an exception
     * @return Whether parsing and loading was successful
     * @todo continueOnError is not used
     * @todo acceptUnknown is not used
     * @throws ParserConfigurationException Thrown if the XML-parser could not be built
     * @throws SAXException Thrown on an XML-parsing error
     * @throws IOException Thrown if the configuration file could not be opened
     */
    public static boolean parseAndLoad(OptionsCont into, String[] args, OptionsTypedFileIO fileIO, String configOptionName, boolean continueOnError, boolean acceptUnknown) throws ParserConfigurationException, SAXException, IOException {
        boolean ok = OptionsParser.parse(into, args, continueOnError);
        if(ok && fileIO!=null && configOptionName!=null && !"".equals(configOptionName) && into.isSet(configOptionName)) {
            ok = fileIO.loadConfiguration(into, configOptionName);
        }
        return ok;
    }
    
    
    /** @brief Output operator
     * @param[in] os The output container to write
     * @param[in] options The options to print
     * @param[in] includeSynonyms Whether synonyms shall be printed
     * @param[in] shortestFirst Whether the synonym shall be printed in increasing length order
     * @param[in] skipDefault Whether options with default values shall be skipped
     */
    public static void printSetOptions(PrintStream os, OptionsCont options, boolean includeSynonyms, boolean shortestFirst, boolean skipDefault) {
    	Vector<String> optionNames = options.getSortedOptionNames();
        for(Iterator<String> i=optionNames.iterator(); i.hasNext(); ) { 
            String name = i.next();
            if(!options.isSet(name)) {
                continue;
            }
            if(skipDefault && options.isDefault(name)) {
                continue;
            }
            Vector<String> synonyms = options.getSynonyms(name);
            if(shortestFirst) {
            	Collections.reverse(synonyms);
            }
            Iterator<String> j=synonyms.iterator();
            String first = j.next();
            os.print(first);
            if(includeSynonyms) {
                if(j.hasNext()) {
                    os.print(" (");
                    for(; j.hasNext(); ) {
                        String name2 = j.next();
                        os.print(name2);
                        if(j.hasNext()) {
                            os.print(", ");
                        }
                    }
                    os.print(")");
                }
            }
            os.print(": " + options.getValueAsString(name));
            if(options.isDefault(name)) {
                os.print(" (default)");
            }
            os.println();
        }
    }
    

    /** @brief Prints the help screen
     *
     * First, the help header is printed. Then, the method iterates over the
     *  known options. In the end, the help tail is printed.
     * @param[in] os The stream to write to
     * @param[in] options The options to print
     * @param[in] maxWidth The maximum width of a line
     * @param[in] optionIndent The indent to use before writing an option
     * @param[in] divider The space between the option name and the description
     * @param[in] sectionIndent The indent to use before writing a section name
     * @param[in] sectionDivider The number of empty lines before a new section starts 
     */
    public static void printHelp(PrintStream os, OptionsCont options, int maxWidth, int optionIndent, int divider, int sectionIndent, int sectionDivider) {
    	Vector<String> optionNames = options.getSortedOptionNames();
    	String helpHead = options.getHelpHead();
    	String helpTail = options.getHelpTail();
        // compute needed width
        int optMaxWidth = 0;
        for(Iterator<String> i=optionNames.iterator(); i.hasNext(); ) {
            String name = i.next();
            String optNames = getHelpFormattedSynonyms(options, name);
            optMaxWidth = Math.max(optMaxWidth, optNames.length());
        }
        // build the indent
        String optionIndentSting = "", sectionIndentSting = "";
        for(int i=0; i<optionIndent; ++i) {
            optionIndentSting += " ";
        }
        for(int i=0; i<sectionIndent; ++i) {
            sectionIndentSting += " ";
        }
        // 
        if(helpHead!=null) {
            os.println(helpHead);
        }
        String lastSection = "";
        for(Iterator<String> i=optionNames.iterator(); i.hasNext(); ) {
        	String name = i.next();
            // check whether a new section starts
            String optSection = options.getSection(name);
            if(optSection!=null && !"".contentEquals(optSection) && lastSection!=optSection) {
            	if(!"".equals(lastSection)) {
            		for(int k=0; k<sectionDivider; ++k) {
            			System.out.println("");
            		}
            	}
                lastSection = optSection;
                os.println(sectionIndentSting+lastSection);
            }
            // write the option
            String optNames = getHelpFormattedSynonyms(options, name);
            // write the divider
            os.print(optionIndentSting+optNames);
            int owidth = optNames.length();
            // write the description
            int beg = 0;
            String desc = options.getDescription(name);
            int offset = divider+optMaxWidth-owidth;
            int startCol = divider+optMaxWidth+optionIndent;
            while(desc!=null&&beg<desc.length()) {
                for(int j=0; j<offset; ++j) {
                    os.print(" ");
                }
                if(maxWidth-startCol>=desc.length()-beg) {
                    os.print(desc.substring(beg));
                    beg = desc.length();
                } else {
                    int end = desc.lastIndexOf(' ', beg+maxWidth-startCol);
                    if(end>=0) {
                    	os.println(desc.substring(beg, end));
                    	beg = end;
                    } else {
                    	throw new RuntimeException("Could not render the description for option '" + name + "'. The description probably contains too long words");
                    }
                }
                startCol = divider+optMaxWidth+optionIndent+1; // could "running description indent"
                offset = startCol;
            }
            os.println();
        }
        if(helpTail!=null) {
            os.println(helpTail);
        }
    }

    


    /** @brief Returns the synomymes of an option as a help-formatted string 
     *
     * The synomymes are sorted by length.
     * @param[in] options The options container to get information from
     * @param[in] optionName The name of option to get the synonyms help string for
     * @return The options as a help-formatted string
     */
    private static String getHelpFormattedSynonyms(OptionsCont options, String optionName) {
        Vector<String> synonyms = options.getSynonyms(optionName);
        Collections.sort(synonyms, options.new SortByLengthComparator());
        StringBuffer sb = new StringBuffer();
        for(Iterator<String> i=synonyms.iterator(); i.hasNext(); ) {
            String name2 = i.next();
            // consider the - / --
            if(name2.length()==1) {
                sb.append('-');
            } else {
                sb.append("--");
            }
            sb.append(name2);
            if(i.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
    
}

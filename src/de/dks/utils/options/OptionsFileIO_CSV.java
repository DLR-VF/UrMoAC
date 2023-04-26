package de.dks.utils.options;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * @class OptionsFileIO_CSV
 * @brief Loads/saves options from/to CSV files
 * @author Daniel Krajzewicz (daniel@krajzewicz.de)
 * @copyright Eclipse Public License v2.0 (EPL v2.0), (c) Daniel Krajzewicz 2021-
 */
public class OptionsFileIO_CSV extends OptionsTypedFileIO {

	/** @brief Loads parameters from a configuration file
	 * @param into The options container to fill
	 * @param configFileName The name of the option to retrieve the file name from
	 * @return Whether options could be loaded
	 * @throws IOException If the file cannot be read 
	 */
	@Override
	protected boolean _loadConfiguration(OptionsCont into, String configFileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(configFileName));
		String line = null;
		do {
			line = br.readLine();
			if(line==null) {
				continue;
			}
			line = line.trim();
			if(line.length()==0) {
				continue;
			}
			String[] r = line.split(";");
			if(r.length!=2) {
				br.close();
				throw new IOException("Missing value for key '" + r[0] + "'.");
			}
			if(into.canBeSet(r[0])) {
				into.set(r[0], r[1]);
			}
	    } while(line!=null);
		br.close();
        return true;
	}

	
    /** @brief Writes the set options as an XML configuration file
     * 
     * @param configName The name of the file to write the configuration to
     * @param options The options container that includes the (set/parsed) options to write 
     * @throws IOException If the file cannot be written
     */
	@Override
    public boolean writeConfiguration(String configName, OptionsCont options) throws IOException {
    	Vector<String> optionNames = options.getSortedOptionNames();
    	FileWriter fileWriter = new FileWriter(configName);
    	for(Iterator<String> i=optionNames.iterator(); i.hasNext(); ) {
    		String oName = i.next();
    		if(options.isSet(oName) && !options.isDefault(oName)) {
    			fileWriter.append(oName+";"+options.getValueAsString(oName)+"\n");
    		}
    	}
    	fileWriter.close();
    	return true;
    }

    
    /** @brief Writes the a template for a configuration file
     * 
     * @param configName The name of the file to write the template to
     * @param options The options container to write a template for 
     * @throws IOException If the file cannot be written
     */
	@Override
    public boolean writeTemplate(String configName, OptionsCont options) throws IOException {
    	Vector<String> optionNames = options.getSortedOptionNames();
    	FileWriter fileWriter = new FileWriter(configName);
    	for(Iterator<String> i=optionNames.iterator(); i.hasNext(); ) {
    		String oName = i.next();
   			fileWriter.append(oName+";\n");
    	}
    	fileWriter.close();
    	return true;
    }

    
	
}

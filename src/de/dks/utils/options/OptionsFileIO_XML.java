package de.dks.utils.options;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @class OptionsFileIO_XML
 * @brief Loads/saves options from/to XML files
 * @author Daniel Krajzewicz (daniel@krajzewicz.de)
 * @copyright Eclipse Public License v2.0 (EPL v2.0), (c) Daniel Krajzewicz 2021-
 */
public class OptionsFileIO_XML extends OptionsTypedFileIO {

	/** @brief Loads parameters from a configuration file
	 * @param into The options container to fill
	 * @param configFileName The name of the option to retrieve the file name from
	 * @return Whether options could be loaded
	 * @throws IOException If the file cannot be read 
	 */
	@Override
	protected boolean _loadConfiguration(OptionsCont into, String configFileName) throws IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser;
		try {
			saxParser = spf.newSAXParser();
	        XMLReader xmlReader = saxParser.getXMLReader();
	        xmlReader.setContentHandler(new OptionsSAXHandler(into));
	        xmlReader.parse(configFileName);
		} catch (ParserConfigurationException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
    	fileWriter.append("<configuration>\n");
    	for(Iterator<String> i=optionNames.iterator(); i.hasNext(); ) {
    		String oName = i.next();
    		if(options.isSet(oName) && !options.isDefault(oName)) {
    			fileWriter.append("   <"+oName+">"+options.getValueAsString(oName)+"</"+oName+">\n");
    		}
    	}
    	fileWriter.append("</configuration>\n");
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
    	fileWriter.append("<configuration>\n");
    	for(Iterator<String> i=optionNames.iterator(); i.hasNext(); ) {
    		String oName = i.next();
   			fileWriter.append("   <"+oName+"></"+oName+">\n");
    	}
    	fileWriter.append("</configuration>\n");
    	fileWriter.close();
    	return true;
    }

    
	
}

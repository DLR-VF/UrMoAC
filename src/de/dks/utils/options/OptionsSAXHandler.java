package de.dks.utils.options;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @class OptionsSAXHandler
 * @brief An XML-SAX-Handler for parsing configurations
 * @author Daniel Krajzewicz (daniel@krajzewicz.de)
 * @copyright Eclipse Public License v2.0 (EPL v2.0), (c) Daniel Krajzewicz 2004-2021
 */
public class OptionsSAXHandler extends DefaultHandler {
    /// @brief The options to fill
    private OptionsCont myOptions;
    
    /// @brief The name of the current option to set
    private String myCurrentOptionName; 
    
    
    /** @brief Constructor
     * @param[in] options The options to fill
     */
    public OptionsSAXHandler(OptionsCont options) {
        myOptions = options;
    }
    
    
    
    /// @brief Handlers for the SAX ContentHandler interface
    /// @{
    
    /** @brief Called on element begin
     * @param[in] namespaceURI
     * @param[in] localName
     * @param[in] qName
     * @param[in] atts
     */
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        myCurrentOptionName = localName;
    }
    
    
    /** @brief Called on characters
     * @param[in] ch the character string
     * @param[in] start the begin of the character string
     * @param[in] length the length of the character string
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        // https://howtodoinjava.com/xml/sax-parser-read-xml-example/, 15.09.2019
        String value = new String(ch, start, length).trim();
        if(value.length()>0 && myOptions.canBeSet(myCurrentOptionName)) {
            myOptions.set(myCurrentOptionName, value);
        }
    }
    /// @}

}

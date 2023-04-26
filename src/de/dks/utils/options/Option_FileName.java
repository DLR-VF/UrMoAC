package de.dks.utils.options;

/**
 * @class Option_FileName
 * @brief A derivation from Option_String for storing file names.
 * @see Option
 * @see Option_String
 * @author Daniel Krajzewicz (daniel@krajzewicz.de)
 * @copyright Eclipse Public License v2.0 (EPL v2.0), (c) Daniel Krajzewicz 2004-2021
 */
public class Option_FileName extends Option_String {
    /** @brief Constructor
     * 
     * The option is marked as being not set (having no value).
     */
    public Option_FileName() {
        super();
    }
    
    
    /** @brief Constructor
     * 
     * The given value is stored as the default value.
     * The option is marked as being set (having a value).
     * @param value The default value
     */
    public Option_FileName(String value) {
        super(value);
    }
    
    
    /** @brief Returns the type name, here: "filename"
     * @return The type name ("filename")
     */
    @Override
    public String getTypeName() {
        return "filename";
    }
    

   /** @brief Returns whether this option is of the type "filename", true in this case
    * @return Whether this options is a file name (yes)
    */
    @Override
    public boolean isFileName()  {
        return true;
    }
    
}

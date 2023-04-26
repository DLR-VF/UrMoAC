package de.dks.utils.options;

/**
 * @class Option_String
 * @brief A derivation from Option for storing string values.
 * @see Option
 * @author Daniel Krajzewicz (daniel@krajzewicz.de)
 * @copyright Eclipse Public License v2.0 (EPL v2.0), (c) Daniel Krajzewicz 2004-2021
 */
public class Option_String extends Option {
    /// @brief The value (no default)
    private String myValue;
    
    
    /** @brief Constructor
     * 
     * The option is marked as being not set (having no value).
     */
    public Option_String() {
        super(false);
    }
    
    
    /** @brief Constructor
     * 
     * The given value is stored as the default value.
     * The option is marked as being set (having a value).
     * @param value The default value
     */
    public Option_String(String value) {
        super(true);
        myValue = value;
    }
    
    
    /** @brief Returns the type name, here: "string"
     * @return The type name ("string")
     */
    @Override
    public String getTypeName() {
        return "string";
    }
    
    
    /** @brief Sets the given value
     * @param[in] valueS The value to set
     */
    @Override
    public void set(String valueS) {
        myValue = valueS;
        setSet();
    }

    
    /** @brief Returns the set value
     * @return The set value
     */
    public String getValue() {
        return myValue;
    }

    
    /** @brief Returns the string representation of the value
     * @return The value encoded to a string
     */
    @Override
    public String getValueAsString() {
        return myValue;
    }

}

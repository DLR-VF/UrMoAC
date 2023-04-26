package de.dks.utils.options;

/**
 * @class Option_Bool
 * @brief A derivation from Option for storing boolean values.
 * @see Option
 * @author Daniel Krajzewicz (daniel@krajzewicz.de)
 * @copyright Eclipse Public License v2.0 (EPL v2.0), (c) Daniel Krajzewicz 2004-2021
 */
public class Option_Bool extends Option {
    /// @brief The value (false by default)
    private boolean myValue = false;
    
    
    /** @brief Constructor
     */
    public Option_Bool() {
        super(true);
    }
    

    /** @brief Returns the type name, here: "bool"
     * @return The type name ("bool")
     */
    @Override
    public String getTypeName() {
        return "bool";
    }
    
    
    /** @brief Sets the given value
     * 
     * "t", "true", and "1" are interpreted as true, "f", "false", and "0" as false.
     * @param[in] value The given string value that shall be translated to double 
     */
    @Override
    public void set(String value) {
        value = value.toLowerCase();
        if("t".equals(value)||"true".equals(value)||"1".equals(value)) {
            myValue = true;
        } else if("f".equals(value)||"false".equals(value)||"0".equals(value)) {
            myValue = false;
        } else {
            throw new NumberFormatException("Could not convert '" + value + "' to a boolean");
        }
        setSet();
    }

    
    /** @brief Returns the set value
     * @return The set value
     */
    public boolean getValue() {
        return myValue;
    }

    
    
    /** @brief Returns the string representation of the value
     * @return The value encoded to a string
     */
    @Override
    public String getValueAsString() {
        if(myValue) {
            return "true";
        } else {
            return "false";
        }
    }

}

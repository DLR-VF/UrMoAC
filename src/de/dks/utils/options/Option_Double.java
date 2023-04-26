package de.dks.utils.options;

/**
 * @class Option_Double
 * @brief A derivation from Option for storing double values.
 * @see Option
 * @author Daniel Krajzewicz (daniel@krajzewicz.de)
 * @copyright Eclipse Public License v2.0 (EPL v2.0), (c) Daniel Krajzewicz 2004-2021
 */
public class Option_Double extends Option {
    /// @brief The value (no default)
    private double myValue;
    
    
    /** @brief Constructor
     * 
     * The option is marked as being not set (having no value).
     */
    public Option_Double() {
        super(false);
    }
    
    
    /** @brief Constructor
     * 
     * The given value is stored as the default value.
     * The option is marked as being set (having a value).
     * @param value The default value
     */
    public Option_Double(double value) {
        super(true);
        myValue = value;
    }
    
    
    /** @brief Returns the type name, here: "double"
     * @return The type name ("double")
     */
    @Override
    public String getTypeName() {
        return "double";
    }
    
    
    /** @brief Sets the given value
     * @param[in] valueS The given string value that shall be translated to double 
     */
    @Override
    public void set(String valueS) {
        double value = Double.parseDouble(valueS);
        myValue = value;
        setSet();
    }

    
    /** @brief Returns the set value
     * @return The set value
     */
    public double getValue() {
        return myValue;
    }

        
    /** @brief Returns the string representation of the value
     * @return The value encoded to a string
     */
    @Override
    public String getValueAsString() {
        return Double.toString(myValue);
    }

}

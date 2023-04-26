package de.dks.utils.options;

/**
 * @class Option
 * @brief A base class for options.
 * @author Daniel Krajzewicz (daniel@krajzewicz.de)
 * @copyright Eclipse Public License v2.0 (EPL v2.0), (c) Daniel Krajzewicz 2004-2021
 */
public abstract class Option {
    /// @brief Information whether a new value can be assigned
    private boolean myAmSetable;

    /// @brief Information whether this option's value may be read
    private boolean myAmSet;

    /// @brief Information whether this option's value is the one given optionally at initialisation
    private boolean myHaveDefaultValue;

    /// @brief The description (what appears in the help screen) of the option
    private String myDescription;

    
    
    /** @brief constructor
     *
     * Use this constructor to build an option with a given type and no default value
     * Sets:
     * @arg myAmSetable to true
     * @arg myAmSet to false
     * @arg myHaveDefaultValue to hasDefault
     * @param[in] hasDefault Whether a default values has been supplied
     */
    protected Option(boolean hasDefault) {
        myAmSetable = true;
        myAmSet = hasDefault;
        myHaveDefaultValue = hasDefault;
    }
        
    
    /** @brief Returns whether this option's value may be read
     *
     * The option's value may be read if either a default value was
     *  given or the user supplied a value.
     * @return Whether the option has been set
     */
    public boolean isSet() {
        return myAmSet;
    }


   /** @brief Returns whether this option's value is the default value
    * @return Whether the option has the default value
    */
    public boolean isDefault() {
        return myHaveDefaultValue;
    }


   /** @brief Allows setting this option
    *
    * After an option has been set by the user, it would normally throw an
    *  InvalidArgument-exception. To reallow setting the option, use this
    *  method.
    */
   void remarkSetable() {
        myAmSetable = true;
   }

   
   /** @brief Returns the name of the type this option has
    *
    * Pure virtual, this method has to be implemented by the respective type-aware subclasses
    * @return This option's value's type name
    */
   public abstract String getTypeName();


   /** @brief Returns whether this option is of the type "filename"
    *
    * Returns false unless overridden (in Option_Filename)
    * @return Whether this options is a file name
    */
   public boolean isFileName()  {
        return false;
   }


   /** @brief Sets the current value to the given
    *
    * Pure virtual, this method has to be implemented by the respective type-aware subclasses
    * @param[in] value The value to set
    * @throw InvalidArgument if this option already has been set (see setSet())
    * @throw NotOfThisTypeException if it is not a string
    */
   public abstract void set(String value);


   /** @brief Adds a description (what appears in the help screen) to the option
    *
    * @param[in] desc The description to set
    */
   public void setDescription(String desc) {
        myDescription = desc;
   }


   /** @brief Retuns the option's description
    * @return The option's description
    */
   public String getDescription()  {
        return myDescription;
   }


   /** @brief Returns the value (if set) as a string
    *
    * Throws an exception if not set.
    * Pure virtual, this method has to be implemented by the respective type-aware subclasses
    * @return The value as a string, if set
    */
   public abstract String getValueAsString();
   

   /** @brief Checks and marks further usage of this option on setting it
    *
    * Checks whether this option may be set using myAmSetable.
    * If not, throws an InvalidArgument-exception.
    * If yes, sets myAmSetable to false, myAmSet to true and myHaveDefaultValue to false
    */
   protected void setSet() {
        if(myAmSetable==false) {
            throw new RuntimeException("This option was already set.");
        }
        myAmSetable = false;
        myHaveDefaultValue = false;
        myAmSet = true;
   }

   
   /** @brief Returns whether the option can be set
    * @return Whether the option can be set
    */
   protected boolean canBeSet() {
	   return myAmSetable;
   }
   
}

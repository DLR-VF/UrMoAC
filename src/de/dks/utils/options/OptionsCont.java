package de.dks.utils.options;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * @class OptionsCont
 * @brief An options storing container.
 * @author Daniel Krajzewicz (daniel@krajzewicz.de)
 * @copyright Eclipse Public License v2.0 (EPL v2.0), (c) Daniel Krajzewicz 2004-2021
 */
public class OptionsCont {
    /// @brief A map from option names to options
    private HashMap<String, Option> myOptionsMap = new HashMap<>();
    
    /// @brief The option's assignment to sections
    private HashMap<Option, String> myOption2Section = new HashMap<>();
    
    /// @brief The list of known options
    private Vector<Option> myOptions = new Vector<>();
    
    /// @brief THe last section added
    private String myCurrentSection = null;
    
    /// @brief The head and the tail of the help pages
    private String myHelpHead = null, myHelpTail = null;
    
    /// @brief The name of the option that defines the parent configuration
    private String myParentConfigurationName = null;
    
    

    /** @brief A string-by-length comparator (increasing length)
     */
    public class SortByLengthComparator implements Comparator<String> {
    	/** @brief Comparator function
    	 * @param[in] obj1 The first item to compare
    	 * @param[in] obj2 The second item to compare
    	 * @return Whether the length of the first string is smaller than the one of the second string
    	 */
        public int compare(String obj1, String obj2) {
            if(obj1.length() == obj2.length()) {
                return 0;
            }
            return obj1.length() < obj2.length() ? -1 : 1;
        }
    }
    
    
    /** @brief Constructor 
     */
    public OptionsCont() {
    }

    

    /// @brief Filling Options
    /// @{

    /** @brief Registers an option under an abbreviation
     * @param[in] abbr The option's abbreviated name
     * @param[in] option The option
     */
    public void add(char abbr, Option option) {
        add(convert(abbr), option);
    }


    /** @brief Registers an option under the given name
     * @param[in] name The option's name
     * @param[in] option The option
     */
    public void add(String name, Option option) {
        // check whether the name is already used
        if(myOptionsMap.containsKey(name)) {
            throw new RuntimeException("An option with the name '" + name + "' already exists.");
        }
        // check whether a synonym already exists, if not, add the option to option's array
        if(!myOptions.contains(option)) {
            myOptions.add(option);
            myOption2Section.put(option, myCurrentSection);
        }
        // add the option to the name-to-option map
        myOptionsMap.put(name, option);
    }


    /** @brief Registers an option under the given abbreviation and the given name
     * @param[in] name The option's name
     * @param[in] abbr The option's abbreviated name
     * @param[in] option The option
     */
    public void add(String name, char abbr, Option option) {
        add(name, option);
        add(convert(abbr), option);
    }


    /** @brief Registers a known option under the other synonym
     * @param[in] name1 The name the option was already known under
     * @param[in] name2 The synonym to register
     */
    public void addSynonym(String name1, String name2) {
        Option o1 = getOptionSecure(name1);
        Option o2 = getOptionSecure(name2);
        if(o1==null&&o2==null) {
            throw new RuntimeException("Neither an option with the name '" + name1 + "' nor an option with the name '" + name2 + "' is known.");
        }
        if(o1!=null&&o2!=null) {
            throw new RuntimeException("Both options are already set ('" + name1 + "' and '" + name2 + "')!");
        }
        if(o1!=null) {
            add(name2, o1);
        } else {
            add(name1, o2);
        }
    }
    /// @{



    /// @brief Filling Help Information
    /// @{
    
    /** @brief Sets the description for an already added option
     *
     * The description is what appears in the help menu
     * @param[in] name The name of the option
     * @param[in] desc The description of the option
     */
    public void setDescription(String name, String desc) {
        Option o = getOption(name);
        o.setDescription(desc);
    }


    /** @brief Starts a new section
     *
     * Options will be stored under this section until a new starts.
     * @param[in] name The name of the section
     */
    public void beginSection(String name) {
        myCurrentSection = name;
    }


    /** @brief Sets the head and the tail of the help output
     * @param[in] head The head of the help output
     * @param[in] tail The tail of the help output
     */
    public void setHelpHeadAndTail(String head, String tail) {
        myHelpHead = head;
        myHelpTail = tail;
    }
    /// @}




    /// @brief Retrieving Option Values
    /// @{

    /** @brief Returns the integer value of the named option
     * @param[in] name The name of the option to retrieve the value from
     * @return The named option's value
     */
    public int getInteger(String name) {
        Option o = getOption(name);
        if(!(o instanceof Option_Integer)) { 
            throw new RuntimeException("Option '" + name + "' is not an integer option!");
        }
        if(!o.isSet()) {
            throw new RuntimeException("The option '" + name + "' is not set!");
        }
        return ((Option_Integer) o).getValue();
    }


    /** @brief Returns the float value of the named option
     * @param[in] name The name of the option to retrieve the value from
     * @return The named option's value
     */
    public double getDouble(String name) {
        Option o = getOption(name);
        if(!(o instanceof Option_Double)) { 
            throw new RuntimeException("Option '" + name + "' is not a double option!");
        }
        if(!o.isSet()) {
            throw new RuntimeException("The option '" + name + "' is not set!");
        }
        return ((Option_Double) o).getValue();
    }


    /** @brief Returns the boolean value of the named option
     * @param[in] name The name of the option to retrieve the value from
     * @return The named option's value
     */
    public boolean getBool(String name) {
        Option o = getOption(name);
        if(!(o instanceof Option_Bool)) { 
            throw new RuntimeException("Option '" + name + "' is not a bool option!");
        }
        if(!o.isSet()) {
            throw new RuntimeException("The option '" + name + "' is not set!");
        }
        return ((Option_Bool) o).getValue();
    }


    /** @brief Returns the string value of the named option
     * @param[in] name The name of the option to retrieve the value from
     * @return The named option's value
     */
    public String getString(String name) {
        Option o = getOption(name);
        if(!(o instanceof Option_String)) { 
            throw new RuntimeException("Option '" + name + "' is not a string option!");
        }
        if(!o.isSet()) {
            throw new RuntimeException("The option '" + name + "' is not set!");
        }
        return ((Option_String) o).getValue();
    }


    /** @brief Returns the value of the named option as a string
     * @param[in] name The name of the option to retrieve the value from
     * @return The string representation of the option's value
     */
    public String getValueAsString(String name) {
        Option o = getOption(name);
        if(!o.isSet()) {
            throw new RuntimeException("The option '" + name + "' is not set!");
        }
        return o.getValueAsString();
    }


    /** @brief Returns the name of the option's type
     * @param[in] name The name of the option get the type of
     * @return The type of the option
     */
    public String getTypeName(String name) {
        Option o = getOption(name);
        return o.getTypeName();
    }


    /** @brief Returns the information whether the option is set
     * @param[in] name The name of the option to check
     * @return Whether the option has a value set
     */
    public boolean isSet(String name) {
        Option o = getOption(name);
        return o.isSet();
    }


    /** @brief Returns whether the named option's value is its default value
     * @param[in] name The name of the option to check whether it has the default value
     * @return Whether the named option has the default value
     */
     public boolean isDefault(String name) {
         Option o = getOption(name);
         return o.isDefault();
     }


     /** @brief Returns whether the named option can be set
      * @param[in] name The name of the option to check whether it can be set
      * @return Whether the named option can be set
      */
     public boolean canBeSet(String name) {
         Option o = getOption(name);
         return o.canBeSet();
     }


    /** @brief Returns the information whether the option is a boolean option
     * @param[in] name The name of the option to check
     * @return Whether the option stores a bool
     */
    public boolean isBool(String name) {
        Option o = getOptionSecure(name);
        if(!(o instanceof Option_Bool)) { 
            return false;
        }
        return true;
    }

     
    /** @brief Returns the information whether the named option is known
     * @param[in] name The name of the option
     * @return Whether the option is known
     */
    public boolean contains(String name) {
        return myOptionsMap.containsKey(name);
    }


    /** @brief Returns the sorted (as inserted) option names
     * @return The sorted list of option names
     */
    public Vector<String> getSortedOptionNames() {
    	Vector<String> ret = new Vector<>();
    	for(Iterator<Option> i=myOptions.iterator(); i.hasNext(); ) {
    		Option o = i.next();
    		ret.add(getSynonyms(o).lastElement());
    	}
        return ret;
    }


    /** @brief Returns the list of synonyms to the given option name
     * @param[in] name The name of the option
     * @return List of this option's names
     */
    public Vector<String> getSynonyms(String name) {
        Option option = getOption(name);
        return getSynonyms(option);
    }


    /** @brief Returns the list of names of the given option
     * @param[in] option The option to retrieve her names
     * @return List of this option's names
     */
    public Vector<String> getSynonyms(Option option) {
        Vector<String> ret = new Vector<>();
        for(Iterator<String> i=myOptionsMap.keySet().iterator(); i.hasNext(); ) {
            String name = i.next();
            if(myOptionsMap.get(name)==option) {
                ret.add(name);
            }
        }
        Collections.sort(ret, new SortByLengthComparator()) ;
        return ret;
    }
    /// @}


    
    /// @brief (Re-)Setting values
    /// @{

    /** @brief Sets the given value to the given option
     * @param[in] name The name of the option to set
     * @param[in] value The value to set
     */
    public void set(String name, String value) {
        Option o = getOption(name);
        o.set(value);
    }


    /** @brief Sets the given value to the given option (boolean options only)
     * @param[in] name The name of the option to set
     * @param[in] value The value to set
     */
    public void set(String name, boolean value) {
        Option o = getOptionSecure(name);
        if(!(o instanceof Option_Bool)) { 
            throw new RuntimeException("This is not a boolean option");
        }
        if(value) {
            o.set("true");
        } else {
            o.set("false");
        }
    }


    /// @brief Remarks all options as unset
    protected void remarkUnset() {
        for(Iterator<Option> i=myOptions.iterator(); i.hasNext(); ) {
            Option o = i.next();
            o.remarkSetable();
        }
    }


    /** @brief Remarks the named option as unset
     * @param[in] name The name of the option to unset
     */
    protected void remarkUnset(String name) {
    	Option option = getOption(name);
    	option.remarkSetable();
    }
    /// @}

    
    
    /// @brief Retrieving Help Information
    /// @{

    /** @brief Returns the name of the section the option belongs to
     * 
     * @param optionName The name of the option to return the section name for
     * @return The name of the section the named option belongs to
     */
    public String getSection(String optionName) {
    	Option option = getOption(optionName);
    	return myOption2Section.get(option); 
    }
    
    
    /** @brief Returns the description of the named option
     * 
     * @param optionName The name of the option to return the description for
     * @return The description of the option
     */
    public String getDescription(String optionName) {
    	Option option = getOption(optionName);
    	return option.getDescription(); 
    }
    
    
    /** @brief Returns the help head
     * 
     * @return The help head
     */
    public String getHelpHead() {
    	return myHelpHead; 
    }
    
    
    /** @brief Returns the help tail
     * 
     * @return The help tail
     */
    public String getHelpTail() {
    	return myHelpTail; 
    }
    /// @}

    
    
    /// @brief Configuration Hierarchy Name Handling
    /// @{
    
    /** @brief Sets the name of the configuration parent option
     * @param parentName The name to find the parent configuration at
     */
    public void setParentConfigurationName(String parentName) {
    	myParentConfigurationName = parentName;
    }
    
    
    /** @brief Returns the name of the configuration parent option
     * @return The name to find the parent configuration at
     */
    public String getParentConfigurationName() {
    	return myParentConfigurationName;
    }
    
    
    
    
    
    /// @brief Private helper options
    /// @{

    /** @brief Returns the option; throws an exception when not existing
     * @param[in] name The name of the option
     * @return The option if known
     * @throw InvalidArgument If the option is not known
     */
    private Option getOption(String name) {
        if(!myOptionsMap.containsKey(name)) {
            throw new RuntimeException("The option '" + name + "' is not known.");
        }
        return myOptionsMap.get(name);
    }


    /** @brief Returns the option or null when not existing
     * @param[in] name The name of the option
     * @return The option if known, null otherwise
     */
    private Option getOptionSecure(String name) {
        if(!myOptionsMap.containsKey(name)) {
            return null;
        }
        return myOptionsMap.get(name);
    }


    /** @brief Converts the character into a string
      * @param[in] abbr The abbreviated name
     * @return The abbreviated name as a string
     */
    private String convert(char abbr) {
        String ret = "";
        ret += abbr;
        return ret;
    }

    
}

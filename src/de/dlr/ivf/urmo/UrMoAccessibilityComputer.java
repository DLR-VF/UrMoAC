/*
 * Copyright (c) 2016-2025
 * Institute of Transport Research
 * German Aerospace Center
 * 
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * https://github.com/DLR-VF/UrMoAC
 * Licensed under the Eclipse Public License 2.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraße 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.locationtech.jts.geom.Geometry;
import org.xml.sax.SAXException;

import de.dks.utils.options.Option_Bool;
import de.dks.utils.options.Option_Double;
import de.dks.utils.options.Option_Integer;
import de.dks.utils.options.Option_String;
import de.dks.utils.options.OptionsCont;
import de.dks.utils.options.OptionsFileIO_XML;
import de.dks.utils.options.OptionsIO;
import de.dks.utils.options.OptionsTypedFileIO;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.NearestEdgeFinder;
import de.dlr.ivf.urmo.router.algorithms.routing.AbstractRouteWeightFunction;
import de.dlr.ivf.urmo.router.algorithms.routing.CrossingTimesModel_CTM1;
import de.dlr.ivf.urmo.router.algorithms.routing.RouteWeightFunction_ExpInterchange_TT;
import de.dlr.ivf.urmo.router.algorithms.routing.RouteWeightFunction_MaxInterchange_TT;
import de.dlr.ivf.urmo.router.algorithms.routing.RouteWeightFunction_Price_TT;
import de.dlr.ivf.urmo.router.algorithms.routing.RouteWeightFunction_TT_ModeSpeed;
import de.dlr.ivf.urmo.router.gtfs.GTFSData;
import de.dlr.ivf.urmo.router.io.GTFSLoader;
import de.dlr.ivf.urmo.router.io.InputReader;
import de.dlr.ivf.urmo.router.io.NetLoader;
import de.dlr.ivf.urmo.router.io.OutputBuilder;
import de.dlr.ivf.urmo.router.mivspeeds.SpeedModel;
import de.dlr.ivf.urmo.router.modes.EntrainmentMap;
import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.output.AggregatorBase;
import de.dlr.ivf.urmo.router.output.CrossingTimesWriter;
import de.dlr.ivf.urmo.router.output.ResultsProcessor;
import de.dlr.ivf.urmo.router.output.DirectWriter;
import de.dlr.ivf.urmo.router.output.NetErrorsWriter;
import de.dlr.ivf.urmo.router.output.ProcessWriter;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.DBODRelation;
import de.dlr.ivf.urmo.router.shapes.DBODRelationExt;
import de.dlr.ivf.urmo.router.shapes.IDGiver;
import de.dlr.ivf.urmo.router.shapes.Layer;

/**
 * @class UrMoAccessibilityComputer
 * @brief The main class for computing accessibility measures based on two sets
 *        of positions and a road network.
 * 
 *        The positions and the network are loaded first, The positions are then
 *        mapped onto the road network. Distances and travel times between each
 *        member of set1 and each member of set2 are then computed using plain
 *        Dijkstra. The results (including the mapping of the objects onto the
 *        road network) are written.
 * @author Daniel Krajzewicz
 */
public class UrMoAccessibilityComputer implements IDGiver {
	// --------------------------------------------------------
	// member variables
	// --------------------------------------------------------
	/// @brief A running id for the loaded objects
	private long runningID = 0;
	/// @brief A mapping from an edge to allocated origins
	HashMap<DBEdge, Vector<MapResult>> nearestFromEdges;
	/// @brief A mapping from an edge to allocated destinations
	HashMap<DBEdge, Vector<MapResult>> nearestToEdges;
	/// @brief A point to the currently processed origin edge
	private Iterator<DBEdge> nextEdgePointer = null;
	/// @brief A counter for seen edges for reporting purposes
	private long seenEdges = 0;
	/// @brief Whether this runs in verbose mode
	private boolean verbose = false;
	/// @brief The route weight computation function
	private AbstractRouteWeightFunction measure = null; // TODO: add documentation on github
	/// @brief The results processor
	private ResultsProcessor resultsProcessor = null;
	/// @brief Starting time for routing
	private int time = -1;
	/// @brief Allowed modes
	private Vector<Mode> modes = null;
	/// @brief list of connections to process
	Vector<DBODRelation> connections = null;
	/// @brief A point to the currently processed connection
	private Iterator<DBODRelation> nextConnectionPointer = null;
	/// @brief A counter for seen connections for reporting purposes
	private long seenODs = 0;
	/// @brief Whether an error occurred
	boolean hadError = false;
	HashMap<Long, Set<String>> toTypes = null; 

	
	
	// --------------------------------------------------------
	// static methods
	// --------------------------------------------------------
	/**
	 * @brief Returns the parsed command line options
	 * 
	 *        It also checks whether the needed (mandatory) options are given
	 *        and whether the options have the proper type.
	 * @param args The list of arguments given to the application
	 * @return The parsed options
	 */
	private static OptionsCont getCMDOptions(String[] args) {
		// set up options
		OptionsCont options = new OptionsCont();
		options.setHelpHeadAndTail("Urban Mobility Accessibility Computer (UrMoAC) v0.10.0\n  (c) German Aerospace Center (DLR), 2016-2025\n  https://github.com/DLR-VF/UrMoAC\n\nUsage:\n"
				+"  java -jar UrMoAC.jar --help\n"
				+"  java -jar UrMoAC.jar --from origins.csv --to destinations.csv --net network.csv\n    --od-output nm_output.csv --mode bike --time 0\n", "");
		
		options.beginSection("Input Options");
		options.add("config", 'c', new Option_String());
		options.setDescription("config", "Defines the configuration to load.");
		options.add("from", 'f', new Option_String());
		options.setDescription("from", "Defines the data source of origins.");
		options.add("to", 't', new Option_String());
		options.setDescription("to", "Defines the data source of destinations.");
		options.add("net", 'n', new Option_String());
		options.setDescription("net", "Defines the road network to load.");
		options.add("mode", 'm', new Option_String());
		options.setDescription("mode", "The mode to use ['car', 'foot', 'bike'].");
		options.add("from-agg", new Option_String());
		options.setDescription("from-agg", "Defines the data source of origin aggregation areas.");
		options.add("to-agg", new Option_String());
		options.setDescription("to-agg", "Defines the data source of destination aggregation areas.");
		/*
		options.add("from-types", new Option_String());
		options.setDescription("from-types", "Defines the data source of origin to types map.");
		*/
		options.add("to-types", new Option_String());
		options.setDescription("to-types", "Defines the data source of destination to types map.");
		options.add("pt", 'p', new Option_String());
		options.setDescription("pt", "Defines the GTFS-based public transport representation.");
		options.add("traveltimes", new Option_String());
		options.setDescription("traveltimes", "Defines the data source of traveltimes.");
		options.add("epsg", new Option_Integer());
		options.setDescription("epsg", "The EPSG projection to use.");
		options.add("time", new Option_Integer());
		options.setDescription("time", "The time the trips start at in seconds.");
		options.add("od-connections", new Option_String());
		options.setDescription("od-connections", "The OD connections to compute.");
		options.add("mode-changes", new Option_String());
		options.setDescription("mode-changes", "Load places where the mode of transport can be changed (no pt).");
		
		options.beginSection("Input Adaptation");
		options.add("from.filter", 'F', new Option_String());
		options.setDescription("from.filter", "Defines a filter for origins to load.");
		options.add("from.id", new Option_String("id"));
		options.setDescription("from.id", "Defines the column name of the origins' ids.");
		options.add("from.geom", new Option_String("geom"));
		options.setDescription("from.geom", "Defines the column name of the origins' geometries.");
		options.add("from.boundary", new Option_String(""));
		options.setDescription("from.boundary", "Defines a boundary for the origins.");
		options.add("to.filter", 'T', new Option_String());
		options.setDescription("to.filter", "Defines a filter for destinations to load.");
		options.add("to.id", new Option_String("id"));
		options.setDescription("to.id", "Defines the column name of the destinations' ids.");
		options.add("to.geom", new Option_String("geom"));
		options.setDescription("to.geom", "Defines the column name of the destinations' geometries.");
		options.add("to.boundary", new Option_String(""));
		options.setDescription("to.boundary", "Defines a boundary for the destinations.");
		options.add("from-agg.filter", new Option_String());
		options.setDescription("from-agg.filter", "Defines a filter for origin aggregation areas to load.");
		options.add("from-agg.id", new Option_String("id"));
		options.setDescription("from-agg.id", "Defines the column name of the origins aggregation areas' ids.");
		options.add("from-agg.geom", new Option_String("geom"));
		options.setDescription("from-agg.geom", "Defines the column name of the origins aggregation areas' geometries.");
		options.add("from-agg.boundary", new Option_String(""));
		options.setDescription("from-agg.boundary", "Defines a boundary for the origins aggregation areas.");
		options.add("to-agg.filter", new Option_String());
		options.setDescription("to-agg.filter", "Defines a filter for destinations aggregation areas to load.");
		options.add("to-agg.id", new Option_String("id"));
		options.setDescription("to-agg.id", "Defines the column name of the destinations aggregation areas' ids.");
		options.add("to-agg.geom", new Option_String("geom"));
		options.setDescription("to-agg.geom", "Defines the column name of the destinations aggregation areas' geometries.");
		options.add("to-agg.boundary", new Option_String(""));
		options.setDescription("to-agg.boundary", "Defines a boundary for the destinations aggregation areas.");
		options.add("net.vmax", new Option_String("vmax"));
		options.setDescription("net.vmax", "Defines the column name of networks's vmax attribute.");
		options.add("net.vmax-model", new Option_String("none"));
		options.setDescription("net.vmax-model", "Defines the model to use for adapting edge speeds ['none', 'vmm1'].");
		options.add("net.geom", new Option_String("geom"));
		options.setDescription("net.geom", "Defines the column name of the network's geometries.");
		options.add("net.boundary", new Option_String(""));
		options.setDescription("net.boundary", "Defines a boundary for the network.");
		options.add("net.keep-subnets", new Option_Bool());
		options.setDescription("net.keep-subnets", "When set, unconnected network parts are not removed.");
		options.add("net.patch-errors", new Option_Bool());
		options.setDescription("net.patch-errors", "When set, broken edge lengths and speeds will be patched.");
		options.add("net.incline", new Option_Bool());
		options.setDescription("net.incline", "Uses incline information.");
		options.add("pt.boundary", new Option_String(""));
		options.setDescription("pt.boundary", "Defines a boundary for the PT offer.");

		options.beginSection("O/D Weighting Options");
		options.add("weight", 'W', new Option_String(""));
		options.setDescription("weight", "An optional weighting attribute for the origins.");
		options.add("variable", 'V', new Option_String(""));
		options.setDescription("variable", "An optional destinations' variable to collect.");

		options.beginSection("Routing Options");
		options.add("max-number", new Option_Integer());
		options.setDescription("max-number", "The maximum number of destinations to visit.");
		options.add("max-distance", new Option_Double());
		options.setDescription("max-distance", "The maximum distance to check.");
		options.add("max-tt", new Option_Double());
		options.setDescription("max-tt", "The maximum travel time to check.");
		options.add("max-variable-sum", new Option_Double());
		options.setDescription("max-variable-sum", "The maximum sum of variable's values to collect.");
		options.add("shortest", new Option_Bool());
		options.setDescription("shortest", "Searches only one destination per origin.");
		options.add("requirespt", new Option_Bool());
		options.setDescription("requirespt", "When set, only information that contains a PT part are stored.");
		options.add("routing-measure", new Option_String());
		options.setDescription("routing-measure", "The measure to use during the routing ['tt_mode', 'price_tt', 'interchanges_tt', 'maxinterchanges_tt'].");
		options.add("routing-measure.param1", new Option_Double());
		options.setDescription("routing-measure.param1", "First parameter of the chosen weight function.");
		options.add("routing-measure.param2", new Option_Double());
		options.setDescription("routing-measure.param2", "Second parameter of the chosen weight function.");
		options.add("crossing-model", new Option_String("none"));
		options.setDescription("crossing-model", "The crossing model to use during the routing ['none', 'ctm1'].");
		options.add("crossing-model.param1", new Option_Double());
		options.setDescription("crossing-model.param1", "First parameter of the chosen crossing model.");
		options.add("crossing-model.param2", new Option_Double());
		options.setDescription("crossing-model.param2", "Second parameter of the chosen crossing model.");
		
		options.beginSection("Network Simplification Options");
		options.add("prunning.remove-geometries", new Option_Bool());
		options.setDescription("prunning.remove-geometries", "Removes edge geometries.");
		options.add("prunning.remove-dead-ends", new Option_Bool());
		options.setDescription("prunning.remove-dead-ends", "Removes dead ends with no objects.");
		options.add("prunning.precompute-tt", new Option_Bool());
		options.setDescription("prunning.precompute-tt", "Precomputes travel times.");
		options.add("prunning.join-similar", new Option_Bool());
		options.setDescription("prunning.join-similar", "Joins edges with similar attributes.");
				
		options.beginSection("Public Transport Options");
		options.add("date", new Option_String());
		options.setDescription("date", "The date for which the accessibilities shall be computed.");
		options.add("entrainment", 'E', new Option_String());
		options.setDescription("entrainment", "Data source for entrainment description.");
		options.add("pt-restriction", new Option_String());
		options.setDescription("pt-restriction", "Restrictions to usable GTFS carriers.");
		
		options.beginSection("Mode Options");
		options.add("foot.vmax", new Option_Double(3.6));
		options.setDescription("foot.vmax", "Sets the maximum walking speed (default: 3.6 km/h).");
		options.add("custom.vmax", new Option_Double());
		options.setDescription("custom.vmax", "Maximum velocity of the custom mode.");
		options.add("custom.kkc-per-hour", new Option_Double());
		options.setDescription("custom.kkc-per-hour", "kkc used per hour when using the custom mode.");
		options.add("custom.co2-per-km", new Option_Double());
		options.setDescription("custom.co2-per-km", "CO2 emitted per kilometer when using the custom mode.");
		options.add("custom.price-per-km", new Option_Double());
		options.setDescription("custom.price-per-km", "Price for using the custom mode per kilometre.");
		options.add("custom.allowed", new Option_String());
		options.setDescription("custom.allowed", "The type of roads the custom mode can use (combination of 'foot', 'bike', 'car' divided by ';').");

		options.beginSection("Output Options");
		options.add("od-output", 'o', new Option_String());
		options.addDeprecatedSynonym("od-output", "nm-output");
		options.setDescription("od-output", "Defines the simple o/d-output to generate.");
		options.add("ext-od-output", new Option_String());
		options.addDeprecatedSynonym("ext-od-output", "ext-nm-output");
		options.setDescription("ext-od-output", "Defines the extended o/d-output to generate.");
		options.add("stat-od-output", new Option_String());
		options.addDeprecatedSynonym("stat-od-output", "stat-nm-output");
		options.setDescription("stat-od-output", "Defines the o/d statistics output to generate.");
		options.add("interchanges-output", 'i', new Option_String());
		options.setDescription("interchanges-output", "Defines the interchanges output to generate.");
		options.add("edges-output", 'e', new Option_String());
		options.setDescription("edges-output", "Defines the edges output to generate.");
		options.add("pt-output", new Option_String());
		options.setDescription("pt-output", "Defines the public transport output to generate.");
		options.add("direct-output", 'd', new Option_String());
		options.setDescription("direct-output", "Defines the direct output to generate.");
		options.add("process-output", new Option_String());
		options.setDescription("process-output", "Defines the process output to generate.");
		options.add("origins-to-road-output", new Option_String());
		options.setDescription("origins-to-road-output", "Defines the output of the mapping between origins and the network.");
		options.add("destinations-to-road-output", new Option_String());
		options.setDescription("destinations-to-road-output", "Defines the output of the mapping between destinations and the network.");
		options.add("subnets-output", new Option_String());
		options.setDescription("subnets-output", "Defines the output for subnets.");
		options.add("net-errors-output", new Option_String());
		options.setDescription("net-errors-output", "Defines the output for network errors and warnings.");
		options.add("crossings-output", new Option_String());
		options.setDescription("crossings-output", "Defines the output for crossing times.");
		options.add("dropprevious", new Option_Bool());
		options.setDescription("dropprevious", "When set, previous output with the same name is replaced.");
		options.add("precision", new Option_Integer(2));
		options.setDescription("precision", "Defines the number of digits after the decimal point.");
		options.add("comment", new Option_Bool());
		options.setDescription("comment", "Adds a comment with the used options into generated output dbs.");
		
		options.beginSection("Process Options");
		options.add("threads", new Option_Integer(1));
		options.setDescription("threads", "The number of threads to use.");
		options.add("verbose", 'v', new Option_Bool());
		options.setDescription("verbose", "Prints what is being done.");
		options.add("net.report-all-errors", new Option_Bool());
		options.setDescription("net.report-all-errors", "When set, all errors are printed.");
		options.add("subnets-summary", new Option_Bool());
		options.setDescription("subnets-summary", "Prints a summary on found subnets.");
		options.add("save-config", new Option_String());
		options.setDescription("save-config", "Saves the set options as a configuration file.");
		options.add("save-template", new Option_String());
		options.setDescription("save-template", "Saves a configuration template to add options to.");
		options.add("help", '?', new Option_Bool());
		options.setDescription("help", "Prints the help screen.");

		// 
		if (args.length==0) {
			System.err.println("Error: no options given.");
			System.err.println("");
			OptionsIO.printHelp(System.out, options, 80, 2, 2, 1, 1);
			return null;
		}
		// parse options
		OptionsTypedFileIO optionsIO = new OptionsFileIO_XML(); 
		try {
			OptionsIO.parseAndLoad(options, args, optionsIO, "config", false, false);
		} catch (RuntimeException e) {
			System.err.println("Error: " + e.getMessage());
			return null;
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			return null;
		}
		// print help if wanted
		if(options.getBool("help")) {
			OptionsIO.printHelp(System.out, options, 80, 2, 2, 1, 1);
			return null;
		}
		// check template / configuration options
		if(options.isSet("save-config")) {
			try {
				optionsIO.writeConfiguration(options.getString("save-config"), options);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		if(options.isSet("save-template")) {
			try {
				optionsIO.writeTemplate(options.getString("save-template"), options);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		// check options
		boolean check = true;
		if(!options.isSet("from")) {
			System.err.println("Error: The mandatory 'from' parameter is missing.");
			check = false;
		}
		if(!options.isSet("to")) {
			System.err.println("Error: The mandatory 'to' parameter is missing.");
			check = false;
		}
		if(!options.isSet("net")) {
			System.err.println("Error: The mandatory 'net' parameter is missing.");
			check = false;
		}
		if(!options.isSet("mode")) {
			System.err.println("Error: The mandatory 'mode' parameter is missing.");
			check = false;
		}
		if(!options.isSet("time")) {
			System.err.println("Error: The mandatory 'time' parameter is missing.");
			check = false;
		}
		if(!options.isSet("epsg")) {
			System.err.println("Error: The mandatory 'epsg' parameter is missing.");
			check = false;
		}
		//
		if(options.isSet("pt")) {
			if(options.isSet("date")) {
				try {
					String date = options.getString("date");
					if(date.length()!=8) {
						System.err.println("The date must be given as yyyyMMdd.");
						check = false;
					}
					Integer.parseInt(date);
					SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd");
					parser.parse(date);							
				} catch (NumberFormatException e) {
					System.err.println("The date must be given as yyyyMMdd.");
					check = false;
				} catch (java.text.ParseException e) {
					System.err.println("The date must be given as yyyyMMdd.");
					check = false;
				}
			} else {
				System.err.println("The date must be given when using GTFS data.");
				check = false;
			}
		}
		//
		if(options.isSet("routing-measure")) {
			String t = options.getString("routing-measure");
			if(!"tt_mode".equals(t)&&!"price_tt".equals(t)&&!"interchanges_tt".equals(t)&&!"maxinterchanges_tt".equals(t)) {
				System.err.println("Unknown routing measure '" + t + "'; allowed are: 'tt_mode', 'price_tt', 'interchanges_tt', and 'maxinterchanges_tt'.");
				check = false;
			}
		}
		//
		if(options.isSet("crossing-model")) {
			String t = options.getString("crossing-model");
			if(!"none".equals(t)&&!"ctm1".equals(t)) {
				System.err.println("Unknown crossing model '" + t + "'; allowed are: 'none', 'ctm1'.");
				check = false;
			}
		}
		//
		if(options.isSet("net.vmax-model")) {
			String t = options.getString("net.vmax-model");
			if(!"none".equals(t)&&!"vmm1".equals(t)) {
				System.err.println("Unknown vmax model '" + t + "'; allowed are: 'none', 'vmm1'.");
				check = false;
			}
		}
		//
		if (!check) {
			return null;
		}
		return options;
	}

		
	/**
	 * @brief Parses the text representation to obtain the encoded modes of transport
	 * @param optionValue The text representation
	 * @return Encoded modes of transport
	 */
	protected static Vector<Mode> getModes(String optionValue) {
		Vector<Mode> ret = new Vector<>();
		// catching deprecated divider
		String[] givenModeNames = null;
		if(optionValue.contains(";")&&!optionValue.contains(",")) {
			System.err.println("Warning: Using ';' as divider is deprecated, please use ','.");
			givenModeNames = optionValue.split(";");
		} else {
			givenModeNames = optionValue.split(",");
		}
		for(String modeName : givenModeNames) {
			// catching deprecated mode names
			if("bicycle".equals(modeName)) {
				System.err.println("Warning: Mode name 'bicycle' is deprecated, please use 'bike'.");
				modeName = "bike";
			} else if("passenger".equals(modeName)) {
				System.err.println("Warning: Mode name 'passenger' is deprecated, please use 'car'.");
				modeName = "car";
			} 
			//
			Mode m = Modes.getMode(modeName);
			if(m==null) {
				return null;
			}
			ret.add(m);
		}
		if(ret.size()==0) {
			return null;
		}
		return ret;
	}
	
	
	/**
	 * @brief Checks whether the needed route weight function parameter are set
	 * @param options The options container to check
	 * @param num The number of needed parameters
	 * @return Whether the needed number of parameters has been defined
	 */
	protected boolean checkParameterOptions(OptionsCont options, int num) {
		for(int i=0; i<num; ++i) {
			if(!options.isSet("routing-measure.param"+(i+1))) {
				System.err.println("Error: value for route weighting function #"+(i+1)+" is missing; use --routing-measure.param"+(i+1)+" <DOUBLE>.");
				hadError = true;
			}
		}
		return !hadError;
	}
	
	
	
	// --------------------------------------------------------
	// main class methods
	// --------------------------------------------------------
	/**
	 * @brief Constructor
	 */
	public UrMoAccessibilityComputer() {
		super();
	}

	
	
	/**
	 * @brief Initializes the tool, mainly by reading options
	 * 
	 * Reads options as defined in the command lines
	 * 
	 * @param[in] options The options to use
	 * @return Whether everything went good
	 * @throws IOException When accessing a file failed
	 */
	protected boolean init(OptionsCont options) throws IOException {
		verbose = options.getBool("verbose");
		// -------- modes
		// ------ set up and parse modes
		Modes.init(options.getDouble("foot.vmax"));
		if (!options.isSet("mode")) {
			throw new IOException("At least one allowed mode must be given.");
		}
		modes = getModes(options.getString("mode"));
		if (modes == null) {
			throw new IOException("The mode(s) '" + options.getString("mode") + "' is/are not known.");
		}
		// ------ reset custom mode if used
		if(Modes.isIncluded(modes, "custom")) {
			double custom_vmax = options.isSet("custom.vmax") ? options.getDouble("custom.vmax") : -1;
			double custom_kkc = options.isSet("custom.kkc-per-hour") ? options.getDouble("custom.kkc-per-hour") : -1;
			double custom_co2 = options.isSet("custom.co2-per-km") ? options.getDouble("custom.co2-per-km") : -1;
			double custom_price = options.isSet("custom.price-per-km") ? options.getDouble("custom.price-per-km") : -1;
			Vector<Mode> allowedV = options.isSet("custom.allowed") ? getModes(options.getString("custom.allowed")) : new Vector<>();
			if(allowedV==null) {
				System.err.println("Could not parse usable lane modes defined in --custom.allowed <MODES>.");
				return false;
			}
			long allowedModes = Modes.getCombinedModeIDs(allowedV);
			if(allowedModes==0) {
				System.err.println("At least one lane type should be allowed for the custom mode; use --custom.allowed <MODES>.");
				return false;
			}
			if(custom_vmax<0) {
				System.err.println("The custom mode must have a maximum velocity given that is >0; use --custom.vmax <SPEED>.");
				return false;
			}
			Modes.setCustomMode(custom_vmax, custom_kkc, custom_co2, custom_price, allowedModes);
		}
		// -------- projection
		if(!options.isSet("epsg")) {
			throw new IOException("An EPSG to use for projection must be given.");
		}
		int epsg = options.getInteger("epsg");
		// !!! todo: check epsg for using metric coordinates
		// -------- loading
		boolean dismissWeight = !options.isSet("from-agg");
		if(dismissWeight && !options.isDefault("weight")) {
			System.out.println("Warning: the weight option is not used as no aggregation takes place.");
		}

		// net
		if (!options.isSet("net")) {
			throw new IOException("A network must be given.");
		}
		if (verbose) System.out.println("Reading the road network");
		NetErrorsWriter netErrorsOutput = options.isSet("net-errors-output") 
				? OutputBuilder.buildNetErrorsWriter(options.getString("net-errors-output"), options.getBool("dropprevious")) : null;  
		Geometry netBoundary = InputReader.getGeometry(options.getString("net.boundary"), "net.boundary", epsg);  
		CrossingTimesWriter ctmWriter = null;
		if(options.isSet("crossings-output")) {
			if("none".equals(options.getString("crossing-model"))) {
				System.err.println("Warning: a writer for crossing times is defined, but no model to compute them.");
			} else {
				ctmWriter = OutputBuilder.buildCrossingTimesWriter(options.getString("crossings-output"), options.getBool("dropprevious"));
			}
		}
		CrossingTimesModel_CTM1 ctm = "ctm1".equals(options.getString("crossing-model")) ? new CrossingTimesModel_CTM1(ctmWriter) : null;
		DBNet net = NetLoader.loadNet(this, options.getString("net"), netBoundary, options.getString("net.vmax"), options.getString("net.geom"), 
				epsg, modes, netErrorsOutput, options.getBool("net.report-all-errors"), options.getBool("net.patch-errors"),
				!options.getBool("net.incline"), ctm);
		if (net.getNumEdges()==0) {
			throw new IOException("No network edges loaded.");
		}
		if (verbose) System.out.println(" " + net.getNumEdges() + " edges loaded (" + net.getNodes().size() + " nodes)");
		if(!options.getBool("net.keep-subnets")) {
			if (verbose) System.out.println("Checking for connectivity...");
			HashMap<Integer, Set<DBEdge>> clusters = net.dismissUnconnectedEdges(options.getBool("subnets-summary"));
			if (options.isSet("subnets-output")) {
				OutputBuilder.writeSubnets("subnets-output", options, clusters);
			}
			if (verbose) System.out.println(" " + net.getNumEdges() + " remaining after removing unconnected ones.");
		}
		if(options.isSet("net.vmax-model")&&"vmm1".equals(options.getString("net.vmax-model"))) {
			if (verbose) System.out.println(" ... recomputing edge vmax.");
			net.applyVMaxModel(new SpeedModel());
		}
		
		// from
		if (verbose) System.out.println("Reading origin places");
		Geometry fromBoundary = InputReader.getGeometry(options.getString("from.boundary"), "from.boundary", epsg);
		Layer fromLayer = InputReader.loadLayer(options, fromBoundary, "from", "weight", dismissWeight, epsg); 
		if (verbose) System.out.println(" " + fromLayer.getObjects().size() + " origin places loaded");
		if (fromLayer.getObjects().size()==0) {
			hadError = true;
			return false;
		}
		// from aggregation
		Layer fromAggLayer = null;
		if (options.isSet("from-agg") && !options.getString("from-agg").equals("all")) {
			if (verbose) System.out.println("Reading origin aggregation zones");
			Geometry fromAggBoundary = InputReader.getGeometry(options.getString("from-agg.boundary"), "from-agg.boundary", epsg);
			fromAggLayer = InputReader.loadLayer(options, fromAggBoundary, "from-agg", null, true, epsg);
			if (verbose) System.out.println(" " + fromAggLayer.getObjects().size() + " origin aggregation geometries loaded");
		}
		// to
		if (verbose) System.out.println("Reading destination places");
		Geometry toBoundary = InputReader.getGeometry(options.getString("to.boundary"), "to", epsg);
		Layer toLayer = InputReader.loadLayer(options, toBoundary, "to", "variable", false, epsg);
		if (verbose) System.out.println(" " + toLayer.getObjects().size() + " destination places loaded");
		if (toLayer.getObjects().size()==0) {
			hadError = true;
			return false;
		}
		// to aggregation
		Layer toAggLayer = null;
		if (options.isSet("to-agg") && !options.getString("to-agg").equals("all")) {
			if (verbose) System.out.println("Reading destination aggregation zones");
			Geometry toAggBoundary = InputReader.getGeometry(options.getString("to-agg.boundary"), "to-agg.boundary", epsg);
			toAggLayer = InputReader.loadLayer(options, toAggBoundary, "to-agg", null, true, epsg); 
			if (verbose) System.out.println(" " + toAggLayer.getObjects().size() + " destination aggregation geometries loaded");
		}
		// from types
		/*
		HashMap<Long, Set<String>> fromTypes = null; 
		if (options.isSet("from-types")) {
			if (verbose) System.out.println("Reading assigned origin types");
			fromTypes = InputReader.loadTypes(options.getString("from-types"), "from-types"); 
			if (verbose) System.out.println(" " + fromTypes.size() + " read origin type assigments");
		}
		*/
		// to types
		if (options.isSet("to-types")) {
			if (verbose) System.out.println("Reading assigned destination types");
			toTypes = InputReader.loadTypes(options.getString("to-types"), "to-types"); 
			if (verbose) System.out.println(" " + toTypes.size() + " read destination type assigments");
		}
		
		// travel times
		if (options.isSet("traveltimes")) {
			if (verbose) System.out.println("Reading the roads' travel times");
			NetLoader.loadTravelTimes(net, options.getString("traveltimes"), verbose);
		}
		
		// entrainment
		EntrainmentMap entrainmentMap = new EntrainmentMap();
		if (options.isSet("entrainment")) {
			if (verbose) System.out.println("Reading entrainment table");
			entrainmentMap = InputReader.loadEntrainment(options);
			if (verbose) System.out.println(" " + entrainmentMap.carrier2carried.size() + " entrainment fields loaded");
		}
		
		// public transport network
		if (options.isSet("pt")) {
			if (verbose) System.out.println("Reading the public transport network");
			Geometry ptBoundary = InputReader.getGeometry(options.getString("pt.boundary"), "pt.boundary", epsg);
			GTFSData gtfs = GTFSLoader.load(options, ptBoundary, net, entrainmentMap, epsg, options.getInteger("threads"), verbose);
			if(gtfs==null) {
				return false;
			}
			if (verbose) System.out.println(" loaded");
		}
		
		// mode changes
		if (options.isSet("mode-changes")) {
			if (verbose) System.out.println("Reading mode change locations");
			if(!InputReader.loadModeChangeLocations(options.getString("mode-changes"), net)) {
				return false;
			}
			if (verbose) System.out.println(" loaded");
		}

		// explicit O/D-connections
		if (options.isSet("od-connections")) {
			if (verbose) System.out.println("Reading the explicite O/D connections");
			connections = InputReader.loadODConnections(options.getString("od-connections"));
			nextConnectionPointer = connections.iterator();
			if (verbose) System.out.println(" loaded");
		}

		// -------- simplify the network#1
		if(!hadError&&options.getBool("prunning.join-similar")) {
			//System.err.println("Error: Joining edges is currently not working. Come back :-)");
			//hadError = true;
			if(Modes.isIncluded(modes, "car")&&options.isSet("traveltimes")) {
				System.err.println("Error: Joining edges is currently not possible when using time-dependent travel times.");
				hadError = true;
			} if(modes.size()>1) {
				System.err.println("Error: Joining edges is currently not possible when using more than one mode.");
				hadError = true;
			} else {
				Mode m = modes.get(0);
				net.joinSimilar(m.vmax, m.id);
				if (verbose) System.out.println(" " + net.getNumEdges() + " remaining after joining similar edges.");
			}
		}
		
		// -------- compute (and optionally write) nearest edges
		// compute nearest edges
		if (verbose) System.out.println("Computing access from the origins to the network");
		NearestEdgeFinder nef1 = new NearestEdgeFinder(fromLayer.getObjects(), net, modes);
		nearestFromEdges = nef1.getNearestEdges(false, false, options.getInteger("threads"));
		if (options.isSet("origins-to-road-output")) {
			OutputBuilder.writeEdgeAllocation("origins-to-road-output", options, nearestFromEdges, epsg);
		}
		if (verbose) System.out.println("Computing egress from the network to the destinations");
		NearestEdgeFinder nef2 = new NearestEdgeFinder(toLayer.getObjects(), net, modes);
		nearestToEdges = nef2.getNearestEdges(true, true, options.getInteger("threads"));
		if (options.isSet("destinations-to-road-output")) {
			OutputBuilder.writeEdgeAllocation("destinations-to-road-output", options, nearestToEdges, epsg);
		}
		
		// -------- simplify the network#2
		if(!hadError&&options.getBool("prunning.remove-geometries")) {
			if (verbose) System.out.println("Nullifying edge geometries...");
			if(options.isSet("direct-output")) {
				System.err.println("Warning: Removing edge geometries will reduce the quality of direct output!");
			}
			net.nullifyEdgeGeometries();
		}
		if(!hadError&&options.getBool("prunning.remove-dead-ends")) {
			if (verbose) System.out.println("Removing unused dead ends...");
			net.removeUnusedDeadEnds(nearestFromEdges, nearestToEdges);
			if (verbose) System.out.println(" " + net.getNumEdges() + " remaining after removing empty dead ends.");
		}
		if(!hadError&&options.getBool("prunning.precompute-tt")) {
			if(Modes.isIncluded(modes, "car")&&options.isSet("traveltimes")) {
				System.err.println("Error: Travel time precomputation is not possible when using time-dependent travel times.");
				hadError = true;
			} if(modes.size()>1) {
				System.err.println("Error: Travel time precomputation is not possible when using more than one mode.");
				hadError = true;
			} else {
				Mode m = modes.get(0);
				net.precomputeTTs(m.vmax);
			}
		}

		// -------- build outputs
		@SuppressWarnings("rawtypes")
		Vector<AggregatorBase> aggregators = OutputBuilder.buildOutputs(options, fromLayer, fromAggLayer, /*fromTypes,*/ toLayer, toAggLayer, toTypes, epsg);
		DirectWriter dw = OutputBuilder.buildDirectOutput(options, epsg, nearestToEdges);
		ProcessWriter tl = OutputBuilder.buildProcessWriter(options);
		time = options.getInteger("time");
		boolean needsPT = options.getBool("requirespt"); // !!!
		resultsProcessor = new ResultsProcessor(time, dw, tl, aggregators, needsPT); 
		// -------- measure
		measure = new RouteWeightFunction_TT_ModeSpeed();
		if(options.isSet("routing-measure")) {
			String t = options.getString("routing-measure");
			if("price_tt".equals(t)) {
				measure = new RouteWeightFunction_Price_TT();
			} else if("interchanges_tt".equals(t)) {
				if(!checkParameterOptions(options, 2)) {
					hadError = true;
					return false;
				} 
				measure = new RouteWeightFunction_ExpInterchange_TT(options.getDouble("routing-measure.param1"), options.getDouble("routing-measure.param2"));
			} else if("maxinterchanges_tt".equals(t)) {
				if(!checkParameterOptions(options, 1)) {
					hadError = true;
					return false;
				}
				measure = new RouteWeightFunction_MaxInterchange_TT((int) options.getDouble("routing-measure.param1"));
			} else if(!"tt_mode".equals(t)) {
				System.err.println("Error: the route weight function '" + t + "' is not known.");
				hadError = true;
			}
		}
		// done everything
		return !hadError;
	}
	
	
	
	/**
	 * @brief Performs the computation
	 * 
	 * The limits and the number of threads to use are read from options.
	 * The computation threads are initialized and started.
	 * 
	 * @param[in] options The options to use
	 * @return Whether everything went good
	 * @throws IOException When something failed
	 */
	protected boolean run(OptionsCont options) throws IOException {
		int maxNumber = options.isSet("max-number") ? options.getInteger("max-number") : -1;
		double maxTT = options.isSet("max-tt") ? options.getDouble("max-tt") : -1;
		double maxDistance = options.isSet("max-distance") ? options.getDouble("max-distance") : -1;
		double maxVar = options.isSet("max-variable-sum") ? options.getDouble("max-variable-sum") : -1;
		boolean shortestOnly = options.getBool("shortest");
		if (verbose) {
			if(connections==null) {
				System.out.println("Computing shortest paths between " + nearestFromEdges.size() + " origin and " + nearestToEdges.size() + " destination edges");
			} else {
				System.out.println("Computing shortest paths for " + connections.size() + " connections.");
			}
		}
		
		// initialise threads
		int numThreads = options.getInteger("threads");
		Vector<DBEdge> fromEdges = new Vector<DBEdge>();
		fromEdges.addAll(nearestFromEdges.keySet());
		Collections.sort(fromEdges, (a, b) -> a.getID().compareTo(b.getID()));  
		nextEdgePointer = fromEdges.iterator();
		seenEdges = 0;
		Vector<Thread> threads = new Vector<>();
		for (int i=0; i<numThreads; ++i) {
			if(connections==null) {
				Thread t = new Thread(new ComputingThread_Plain(this, measure, resultsProcessor, time, modes, maxNumber, maxTT, maxDistance, maxVar, shortestOnly, options.isSet("pt"), toTypes));
				threads.add(t);
		        t.start();
			} else {
				Thread t = new Thread(new ComputingThread_OD(this, measure, resultsProcessor, time, modes, maxNumber, maxTT, maxDistance, maxVar, shortestOnly));
				threads.add(t);
		        t.start();
			}
		}
		// close threads after computation
		for(Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				hadError = true;
				e.printStackTrace();
			}
		}
		System.out.println(""); // progress ends
		resultsProcessor.finish();
		return true;
	}
	
	
	
	/**
	 * @brief Returns the next starting edge to process
	 * @return The next edge to start routing from
	 */
	public synchronized DBEdge getNextStartingEdge() {
		DBEdge nextEdge = null;
		while(nextEdge==null) {
			if(!nextEdgePointer.hasNext()) {
				return null;
			}
			nextEdge = nextEdgePointer.next();
		}
		++seenEdges;
		if (verbose) System.out.print("\r " + seenEdges + " of " + nearestFromEdges.size() + " edges");
		return nextEdge;
	}

	
	/**
	 * @brief Returns the next connection to process
	 * @return The next connection to process
	 */
	public synchronized DBODRelationExt getNextOD() {
		if(hadError) {
			return null;
		}
		DBODRelation nextOD = null;
		while(nextOD==null) {
			if(!nextConnectionPointer.hasNext()) {
				return null;
			}
			nextOD = nextConnectionPointer.next();
		}
		DBODRelationExt ret = new DBODRelationExt(nextOD.origin, nextOD.destination, nextOD.weight);
		ret.fromEdge = getEdgeForObject(ret.origin, nearestFromEdges);
		ret.toEdge = getEdgeForObject(ret.destination, nearestToEdges);
		if(ret.fromEdge==null||ret.toEdge==null) {
			if(ret.fromEdge==null) {
				System.err.println("\nCould not find the edge for origin " + ret.origin);
			}
			if(ret.toEdge==null) {
				System.err.println("\nCould not find the edge for destination " + ret.destination);
			}
			hadError = true;
			return null;
		}
		for(MapResult m : nearestFromEdges.get(ret.fromEdge)) {
			if(m.em.getOuterID()==ret.origin) {
				ret.fromMR = m;
			}
		}
		for(MapResult m : nearestToEdges.get(ret.toEdge)) {
			if(m.em.getOuterID()==ret.destination) {
				ret.toMR = m;
			}
		}
		++seenODs;
		if (verbose) {
			System.out.print("\r " + seenODs + " of " + connections.size() + " connections");
		}
		return ret;
	}
	
	
	
	/** @brief Returns the edge the named object is allocated at
	 * 
	 * @param objID The ID of the object to find the edge for
	 * @param mapping The previously generated mapping between objects and edges
	 * @return The edge the named object is allocated at
	 * @todo Recheck - iterates over all objects currently...
	 */
	private DBEdge getEdgeForObject(long objID, HashMap<DBEdge, Vector<MapResult>> mapping) {
		for(DBEdge e : mapping.keySet()) {
			Vector<MapResult> t = mapping.get(e);
			for(MapResult m : t) {
				if(m.em.getOuterID()==objID) {
					return e;
				}
			}
		}
		return null;
	}
	

	/**
	 * @brief Returns a running (auto-incremented) number
	 * @return A running number
	 */
	@Override
	public synchronized long getNextRunningID() {
		return ++runningID;
	}


	
	/** @brief Informs the id giver about a new id
	 * @param id An extern id to regard
	 */
	public synchronized void hadExternID(long id) {
		runningID = Math.max(runningID, id+1);
	}

	

	/** @brief Returns whether an error occurred
	 * @return Whether an error occurred
	 */
	public boolean hadError() {
		return hadError;
	}
	

	
	
	// --------------------------------------------------------
	// main
	// --------------------------------------------------------
	/**
	 * @brief The main method
	 * @param args Given command line arguments
	 */
	public static void main(String[] args) {
		// parse and check options
		OptionsCont options = getCMDOptions(args);
		if (options == null) {
			return;
		}
		boolean hadError = false;
		try {
			UrMoAccessibilityComputer worker = new UrMoAccessibilityComputer();
			// initialise (load data and stuff)
			hadError = !worker.init(options);
			// compute
			if(!hadError) {
				hadError = !worker.run(options);
			}
			hadError |= worker.hadError();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			hadError = true;
			//e.printStackTrace();
		}
		// -------- finish
		if(!hadError) {
			System.out.println("done.");
		} else {
			System.err.println("Quitting on error...");
		}
	}
	
	
}

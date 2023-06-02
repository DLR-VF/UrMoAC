/*
 * Copyright (c) 2016-2023 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * https://github.com/DLR-VF/UrMoAC
 * Licensed under the Eclipse Public License 2.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
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
import de.dlr.ivf.urmo.router.algorithms.routing.BoundDijkstra;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.algorithms.routing.RouteWeightFunction_ExpInterchange_TT;
import de.dlr.ivf.urmo.router.algorithms.routing.RouteWeightFunction_MaxInterchange_TT;
import de.dlr.ivf.urmo.router.algorithms.routing.RouteWeightFunction_Price_TT;
import de.dlr.ivf.urmo.router.algorithms.routing.RouteWeightFunction_TT_Modes;
import de.dlr.ivf.urmo.router.io.GTFSLoader;
import de.dlr.ivf.urmo.router.io.InputReader;
import de.dlr.ivf.urmo.router.io.NetLoader;
import de.dlr.ivf.urmo.router.io.OutputBuilder;
import de.dlr.ivf.urmo.router.modes.EntrainmentMap;
import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.output.Aggregator;
import de.dlr.ivf.urmo.router.output.DijkstraResultsProcessor;
import de.dlr.ivf.urmo.router.output.DirectWriter;
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
 *        road network) are written into files.
 */
public class UrMoAccessibilityComputer implements IDGiver {
	// --------------------------------------------------------
	// member variables
	// --------------------------------------------------------
	/// @brief A running id for the loaded objects
	private long runningID = 0;
	/// @brief A mapping from an edge to allocated sources
	private HashMap<DBEdge, Vector<MapResult>> nearestFromEdges;
	/// @brief A mapping from an edge to allocated destinations
	private HashMap<DBEdge, Vector<MapResult>> nearestToEdges;
	/// @brief A point to the currently processed source edge
	private Iterator<DBEdge> nextEdgePointer = null;
	/// @brief A counter for seen edges for reporting purposes
	private long seenEdges = 0;
	/// @brief Whether this runs in verbose mode
	private boolean verbose = false;
	/// @brief The route weight computation function
	private AbstractRouteWeightFunction measure = null; // TODO: add documentation on github
	/// @brief The results processor
	private DijkstraResultsProcessor resultsProcessor = null;
	/// @brief Starting time of computation
	private int time = -1;
	/// @brief Allowed modes
	private long modes = -1;
	/// @brief Initial mode
	private long initMode = -1;
	/// @brief list of connections to process
	private Vector<DBODRelation> connections = null;
	/// @brief A point to the currently processed connection
	private Iterator<DBODRelation> nextConnectionPointer = null;
	/// @brief A counter for seen connections for reporting purposes
	private long seenODs = 0;
	/// @brief Whether an error occurred
	private boolean hadError = false;

	
	
	// --------------------------------------------------------
	// inner classes
	// --------------------------------------------------------
	/** @class ComputingThread
	 * 
	 * A thread which polls for new sources, computes the accessibility and
	 * writes the results before asking for the next one
	 */
	private static class ComputingThread implements Runnable {
		/// @brief The parent to get information from
		UrMoAccessibilityComputer parent;
		/// @brief The results processor to use
		DijkstraResultsProcessor resultsProcessor;
		/// @brief The routing measure to use
		AbstractRouteWeightFunction measure;
		/// @brief Whether only entries which contain a public transport part shall be processed 
		boolean needsPT;
		/// @brief The start time of routing
		int time; 
		/// @brief The mode of transport to use at the begin
		long initMode;
		/// @brief The available transport modes
		long modes;
		/// @brief The maximum number of destinations to find
		int boundNumber;
		/// @brief The maximum travel time to use
		double boundTT;
		/// @brief The maximum distance to pass
		double boundDist;
		/// @brief The maximum value to collect
		double boundVar;
		/// @brief Whether only the shortest connection shall be found 
		boolean shortestOnly;
		
		
		/**
		 * @brief Constructor
		 * @param _parent The parent to get information from
		 * @param _needsPT Whether only entries which contain a public transport part shall be processed 
		 * @param _measure The routing measure to use
		 * @param _resultsProcessor The results processor to use
		 * @param _time The start time of routing
		 * @param _initMode The mode of transport to use at the begin
		 * @param _modes The available transport modes
		 * @param _boundNumber The maximum number of destinations to find
		 * @param _boundTT The maximum travel time to use
		 * @param _boundDist The maximum distance to pass
		 * @param _boundVar The maximum value to collect
		 * @param _shortestOnly Whether only the shortest connection shall be found 
		 */
		public ComputingThread(UrMoAccessibilityComputer _parent, boolean _needsPT,
				AbstractRouteWeightFunction _measure, DijkstraResultsProcessor _resultsProcessor,
				int _time, long _initMode, 
				long _modes, int _boundNumber, double _boundTT, 
				double _boundDist, double _boundVar, boolean _shortestOnly) {
			super();
			parent = _parent;
			resultsProcessor = _resultsProcessor;
			measure = _measure;
			needsPT = _needsPT;
			time = _time; 
			initMode = _initMode;
			modes = _modes;
			boundNumber = _boundNumber;
			boundTT = _boundTT;
			boundDist = _boundDist;
			boundVar = _boundVar;
			shortestOnly = _shortestOnly;
		}
		
		
		
		/**
		 * @brief Performs the computation
		 * 
		 * Iterates over edges or od-connections.
		 * Builds the paths, first, then uses them to generate the results.
		 */
		public void run() {
			try {
				if(parent.connections==null) {
					DBEdge e = null;
					do {
						e = parent.getNextStartingEdge();
						if(e==null) {
							continue;
						}
						/// TODO: recheck whether routing is needed per source
						DijkstraResult ret = BoundDijkstra.run(measure, time, e, initMode, modes, parent.nearestToEdges.keySet(), boundNumber, boundTT, boundDist, boundVar, shortestOnly);
						Vector<MapResult> fromObjects = parent.nearestFromEdges.get(e);
						for(MapResult mr : fromObjects) {
							resultsProcessor.process(mr, ret, needsPT, -1);
						}
					} while(e!=null&&!parent.hadError);
				} else {
					DBODRelationExt od = null;
					do {
						od = parent.getNextOD();
						if(od==null) {
							continue;
						}
						/// TODO: recheck whether routing is needed per source
						Set<DBEdge> destinations = new HashSet<>();
						destinations.add(od.toEdge);
						DijkstraResult ret = BoundDijkstra.run(measure, time, od.fromEdge, initMode, modes, destinations, boundNumber, boundTT, boundDist, boundVar, shortestOnly);
						resultsProcessor.process(od.fromMR, ret, needsPT, od.destination);
					} while(od!=null&&!parent.hadError);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	


	// --------------------------------------------------------
	// static methods
	// --------------------------------------------------------
	/**
	 * @brief Returns the parsed command line options
	 * 
	 *        It also checks whether the needed (mandatory) options are given
	 *        and whether the options have the proper type.
	 * @param args The list of arguments given to the application
	 * @return The parsed command line
	 */
	@SuppressWarnings("static-access")
	private static OptionsCont getCMDOptions(String[] args) {
		// set up options
		OptionsCont options = new OptionsCont();
		options.setHelpHeadAndTail("Urban Mobility Accessibility Computer (UrMoAC) v0.6\n  (c) German Aerospace Center (DLR), 2016-2021\n  https://github.com/DLR-VF/UrMoAC\n\nUsage:\n"
				+"  java -jar UrMoAC.jar --help\n"
				+"  java -jar UrMoAC.jar --from file;sources.csv --to file;destinations.csv\n    --net file;network.csv --od-output file;nm_output.csv\n    --mode bike --time 0\n", "");
		
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
		
		options.beginSection("Input Adaptation");
		options.add("from.filter", 'F', new Option_String());
		options.setDescription("from.filter", "Defines a filter for origins to load.");
		options.add("from.id", new Option_String("gid"));
		options.setDescription("from.id", "Defines the column name of the origins' ids.");
		options.add("from.geom", new Option_String("the_geom"));
		options.setDescription("from.geom", "Defines the column name of the origins' geometries.");
		options.add("to.filter", 'T', new Option_String());
		options.setDescription("to.filter", "Defines a filter for destinations to load.");
		options.add("to.id", new Option_String("gid"));
		options.setDescription("to.id", "Defines the column name of the destinations' ids.");
		options.add("to.geom", new Option_String("the_geom"));
		options.setDescription("to.geom", "Defines the column name of the destinations' geometries.");
		options.add("from-agg.filter", new Option_String());
		options.setDescription("from-agg.filter", "Defines a filter for origin aggregation areas to load.");
		options.add("from-agg.id", new Option_String("gid"));
		options.setDescription("from-agg.id", "Defines the column name of the origins aggregation areas' ids.");
		options.add("from-agg.geom", new Option_String("the_geom"));
		options.setDescription("from-agg.geom", "Defines the column name of the origins aggregation areas' geometries.");
		options.add("to-agg.filter", new Option_String());
		options.setDescription("to-agg.filter", "Defines a filter for destination aggregation areas to load.");
		options.add("to-agg.id", new Option_String("gid"));
		options.setDescription("to-agg.id", "Defines the column name of the destination aggregation areas' ids.");
		options.add("to-agg.geom", new Option_String("the_geom"));
		options.setDescription("to-agg.geom", "Defines the column name of the destination aggregation areas' geometries.");
		options.add("net.vmax", new Option_String("vmax"));
		options.setDescription("net.vmax", "Defines the column name of networks's vmax attribute.");
		options.add("keep-subnets", new Option_Bool());
		options.setDescription("keep-subnets", "When set, unconnected network parts are not removed.");
		
		options.beginSection("Weighting Options");
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
		options.add("clip-to-net", new Option_Bool());
		options.setDescription("clip-to-net", "When set, sources, destinations, and pt is clipped at the network boundaries.");
		options.add("measure", new Option_String());
		options.setDescription("measure", "The measure to use during the routing ['tt_mode', 'price_tt', 'interchanges_tt', 'maxinterchanges_tt'].");
		options.add("measure-param1", new Option_Double());
		options.setDescription("measure-param1", "First parameter of the chosen weight function.");
		options.add("measure-param2", new Option_Double());
		options.setDescription("measure-param2", "Second parameter of the chosen weight function.");
		
		options.beginSection("Public Transport Options");
		options.add("pt-boundary", new Option_String());
		options.setDescription("pt-boundary", "Defines the data source of the boundary for the PT offer.");
		options.add("date", new Option_String());
		options.setDescription("date", "The date for which the accessibilities shall be computed.");
		options.add("entrainment", 'E', new Option_String());
		options.setDescription("entrainment", "Data source for entrainment description.");
		options.add("pt-restriction", new Option_String());
		options.setDescription("pt-restriction", "Restrictions to usable GTFS carriers.");
		
		options.beginSection("Custom Mode Options");
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
		options.setDescription("od-output", "Defines the n:m output.");
		options.add("ext-od-output", new Option_String());
		options.setDescription("ext-od-output", "Defines the extended n:m output.");
		options.add("stat-od-output", new Option_String());
		options.setDescription("stat-od-output", "Defines the n:m statistics output.");
		options.add("interchanges-output", 'i', new Option_String());
		options.setDescription("interchanges-output", "Defines the interchanges output.");
		options.add("edges-output", 'e', new Option_String());
		options.setDescription("edges-output", "Defines the edges output.");
		options.add("pt-output", new Option_String());
		options.setDescription("pt-output", "Defines the public transport output.");
		options.add("direct-output", 'd', new Option_String());
		options.setDescription("direct-output", "Defines the direct output.");
		options.add("origins-to-road-output", new Option_String());
		options.setDescription("origins-to-road-output", "Defines the output of the mapping between sources and the network.");
		options.add("destinations-to-road-output", new Option_String());
		options.setDescription("destinations-to-road-output", "Defines the output of the mapping between destinations and the network.");
		options.add("subnets-output", new Option_String());
		options.setDescription("subnets-output", "Defines the output of subnets.");
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
		options.add("subnets-summary", new Option_Bool());
		options.setDescription("subnets-summary", "Prints a summary on found subnets.");
		options.add("save-config", new Option_String());
		options.setDescription("save-config", "Saves the set options as a configuration file.");
		options.add("save-template", new Option_String());
		options.setDescription("save-template", "Saves a configuration template to add options to.");
		options.add("help", '?', new Option_Bool());
		options.setDescription("help", "Prints the help screen.");

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
			System.err.println("Error: The 'from' parameter is missing.");
			check = false;
		}
		if(!options.isSet("to")) {
			System.err.println("Error: The 'to' parameter is missing.");
			check = false;
		}
		if(!options.isSet("net")) {
			System.err.println("Error: The 'net' parameter is missing.");
			check = false;
		}
		if(!options.isSet("mode")) {
			System.err.println("Error: The 'mode' parameter is missing.");
			check = false;
		}
		if(!options.isSet("time")) {
			System.err.println("Error: The 'time' parameter is missing.");
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
		if(options.isSet("measure")) {
			String t = options.getString("measure");
			if(!"tt_mode".equals(t)&&!"price_tt".equals(t)&&!"interchanges_tt".equals(t)&&!"maxinterchanges_tt".equals(t)) {
				System.err.println("Unknown measure '" + t + "'; allowed are: 'tt_mode', 'price_tt', 'interchanges_tt', and 'maxinterchanges_tt'.");
				check = false;
			}
		}
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
		String[] givenModeNames = null;
		// catching deprecated divider
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
			if(!options.isSet("measure-param"+(i+1))) {
				System.err.println("Error: value for route weighting function #"+(i+1)+" is missing; use --measure-param"+(i+1)+" <DOUBLE>.");
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
	 * @brief Initialises the tool, mainly by reading options
	 * 
	 * Reads options as defined in the command lines
	 * 
	 * @param[in] options The options to use
	 * @return Whether everything went good
	 * @throws IOException When accessing a file failed
	 * @throws ParseException 
	 */
	protected boolean init(OptionsCont options) throws IOException {
		verbose = options.getBool("verbose");
		// -------- modes
		// ------ set up and parse modes
		Modes.init();
		if (!options.isSet("mode")) {
			throw new IOException("At least one allowed mode must be given.");
		}
		Vector<Mode> modesV = getModes(options.getString("mode"));
		if (modesV == null) {
			throw new IOException("The mode(s) '" + options.getString("mode") + "' is/are not known.");
		}
		modes = Modes.getCombinedModeIDs(modesV);
		initMode = modesV.get(0).id;
		// ------ reset custom mode if used
		if((modes&Modes.getMode("custom").id)!=0) { // 
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
		int epsg;
		if(options.isSet("epsg")) {
			epsg = options.getInteger("epsg");
		} else {
			// automatic epsg-value
			epsg = InputReader.findUTMZone(options);
			if(epsg==-1) {
				System.out.println("Could not find a valid UTM-zone. Quitting");
				return false;
			} else {
				String utmZone;
				if(epsg>32600) { //northern hemisphere
					utmZone = Integer.toString(epsg%100)+"N";
				} else { // southern hemisphere
					utmZone = Integer.toString(epsg%100)+"S";
				}
				System.out.println("Using UTM-zone "+utmZone+", EPSG-code: " + epsg);
			}
		}
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
		DBNet net = NetLoader.loadNet(this, options.getString("net"), options.getString("net.vmax"), epsg, modes);
		if (verbose) System.out.println(" " + net.getNumEdges() + " edges loaded (" + net.getNodes().size() + " nodes)");
		if(!options.getBool("keep-subnets")) {
			if (verbose) System.out.println("Checking for connectivity...");
			Set<Set<DBEdge>> clusters = net.dismissUnconnectedEdges(options.getBool("subnets-summary"));
			if (options.isSet("subnets-output")) {
				OutputBuilder.writeSubnets("subnets-output", options, clusters);
			}
			if (verbose) System.out.println(" " + net.getNumEdges() + " remaining after removing unconnected ones.");
		}
		Geometry bounds = null;
		if(options.getBool("clip-to-net")) {
			bounds = net.getBounds();
		} else if (options.isSet("pt-boundary")) {
			bounds = InputReader.loadGeometry(options.getString("pt-boundary"), "pt-boundary", epsg);
		}

		// from
		if (verbose) System.out.println("Reading origin places");
		Layer fromLayer = InputReader.loadLayer(options, bounds, "from", "weight", dismissWeight, this, epsg); 
		if (verbose) System.out.println(" " + fromLayer.getObjects().size() + " origin places loaded");
		if (fromLayer.getObjects().size()==0) {
			hadError = true;
			return false;
		}
		// from aggregation
		Layer fromAggLayer = null;
		if (options.isSet("from-agg") && !options.getString("from-agg").equals("all")) {
			if (verbose) System.out.println("Reading origin aggregation zones");
			fromAggLayer = InputReader.loadLayer(options, bounds, "from-agg", null, true, this, epsg);
			if (verbose) System.out.println(" " + fromAggLayer.getObjects().size() + " origin aggregation geometries loaded");
		}
		// to
		if (verbose) System.out.println("Reading destination places");
		Layer toLayer = InputReader.loadLayer(options, bounds, "to", "variable", false, this, epsg);
		if (verbose) System.out.println(" " + toLayer.getObjects().size() + " destination places loaded");
		if (toLayer.getObjects().size()==0) {
			hadError = true;
			return false;
		}
		// to aggregation
		Layer toAggLayer = null;
		if (options.isSet("to-agg") && !options.getString("to-agg").equals("all")) {
			if (verbose) System.out.println("Reading sink aggregation zones");
			toAggLayer = InputReader.loadLayer(options, bounds, "to-agg", null, true, this, epsg); 
			if (verbose) System.out.println(" " + toAggLayer.getObjects().size() + " sink aggregation geometries loaded");
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
			GTFSLoader.load(options, bounds, net, entrainmentMap, epsg, options.getInteger("threads"), verbose);
			if (verbose) System.out.println(" loaded");
		}

		// explicit O/D-connections
		if (options.isSet("od-connections")) {
			if (verbose) System.out.println("Reading the explicite O/D connections");
			connections = InputReader.loadODConnections(options.getString("od-connections"));
			nextConnectionPointer = connections.iterator();
			if (verbose) System.out.println(" loaded");
		}

		// -------- compute (and optionally write) nearest edges
		// compute nearest edges
		if (verbose) System.out.println("Computing access from the origins to the network");
		NearestEdgeFinder nef1 = new NearestEdgeFinder(fromLayer.getObjects(), net, initMode);
		nearestFromEdges = nef1.getNearestEdges(false, options.getInteger("threads"));
		if (options.isSet("origins-to-road-output")) {
			OutputBuilder.writeEdgeAllocation("origins-to-road-output", options, nearestFromEdges, epsg);
		}
		if (verbose) System.out.println("Computing egress from the network to the destinations");
		NearestEdgeFinder nef2 = new NearestEdgeFinder(toLayer.getObjects(), net, modes);
		nearestToEdges = nef2.getNearestEdges(true, options.getInteger("threads"));
		if (options.isSet("destinations-to-road-output")) {
			OutputBuilder.writeEdgeAllocation("destinations-to-road-output", options, nearestToEdges, epsg);
		}

		// -------- build outputs
		@SuppressWarnings("rawtypes")
		Vector<Aggregator> aggregators = OutputBuilder.buildOutputs(options, fromLayer, fromAggLayer, toLayer, toAggLayer, epsg);
		DirectWriter dw = OutputBuilder.buildDirectOutput(options, epsg, nearestToEdges);
		time = options.getInteger("time");
		resultsProcessor = new DijkstraResultsProcessor(time, dw, aggregators, nearestFromEdges, nearestToEdges); 

		// -------- measure
		measure = new RouteWeightFunction_TT_Modes();
		if(options.isSet("measure")) {
			String t = options.getString("measure");
			if("price_tt".equals(t)) {
				measure = new RouteWeightFunction_Price_TT();
			} else if("interchanges_tt".equals(t)) {
				if(!checkParameterOptions(options, 2)) {
					hadError = true;
					return false;
				} 
				measure = new RouteWeightFunction_ExpInterchange_TT(options.getDouble("measure-param1"), options.getDouble("measure-param2"));
			} else if("maxinterchanges_tt".equals(t)) {
				if(!checkParameterOptions(options, 1)) {
					hadError = true;
					return false;
				}
				measure = new RouteWeightFunction_MaxInterchange_TT((int) options.getDouble("measure-param1"));
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
	 * @throws SQLException When accessing the database failed
	 * @throws IOException When accessing a file failed
	 * @throws ParseException When an option could not been parsed
	 */
	protected boolean run(OptionsCont options) throws IOException {
		int maxNumber = options.isSet("max-number") ? options.getInteger("max-number") : -1;
		double maxTT = options.isSet("max-tt") ? options.getDouble("max-tt") : -1;
		double maxDistance = options.isSet("max-distance") ? options.getDouble("max-distance") : -1;
		double maxVar = options.isSet("max-variable-sum") ? options.getDouble("max-variable-sum") : -1;
		boolean shortestOnly = options.getBool("shortest");
		boolean needsPT = options.getBool("requirespt");
		if (verbose) {
			if(connections==null) {
				System.out.println("Computing shortest paths between " + nearestFromEdges.size() + " origin and " + nearestToEdges.size() + " destination edges");
			} else {
				System.out.println("Computing shortest paths for " + connections.size() + " connections.");
			}
		}
		
		// initialise threads
		int numThreads = options.getInteger("threads");
		nextEdgePointer = nearestFromEdges.keySet().iterator();
		seenEdges = 0;
		Vector<Thread> threads = new Vector<>();
		for (int i=0; i<numThreads; ++i) {
			Thread t = new Thread(new ComputingThread(this, needsPT, measure, resultsProcessor, time, initMode, modes, maxNumber, maxTT, maxDistance, maxVar, shortestOnly));
			threads.add(t);
	        t.start();
		}
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

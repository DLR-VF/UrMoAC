/**
 * Copyright (c) 2016-2021 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * @author: Daniel.Krajzewicz@dlr.de
 * Licensed under the GNU General Public License v3.0
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
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.vividsolutions.jts.geom.Geometry;

import de.dlr.ivf.helper.options.OptionsHelper;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.NearestEdgeFinder;
import de.dlr.ivf.urmo.router.algorithms.routing.AbstractRoutingMeasure;
import de.dlr.ivf.urmo.router.algorithms.routing.BoundDijkstra;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.algorithms.routing.RoutingMeasure_ExpInterchange_TT;
import de.dlr.ivf.urmo.router.algorithms.routing.RoutingMeasure_MaxInterchange_TT;
import de.dlr.ivf.urmo.router.algorithms.routing.RoutingMeasure_Price_TT;
import de.dlr.ivf.urmo.router.algorithms.routing.RoutingMeasure_TT_Modes;
import de.dlr.ivf.urmo.router.gtfs.GTFSData;
import de.dlr.ivf.urmo.router.io.GTFSReader;
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
 * @author Daniel Krajzewicz (c) 2016 German Aerospace Center, Institute of Transport Research
 * @copyright (c) DLR 2016-2021
 */
public class UrMoAccessibilityComputer implements IDGiver {
	// --------------------------------------------------------
	// member variables
	// --------------------------------------------------------
	/// @brief A running id for the loaded objects
	private long runningID = 0;
	/// @brief A mapping from an edge to allocated sources
	HashMap<DBEdge, Vector<MapResult>> nearestFromEdges;
	/// @brief A mapping from an edge to allocated destinations
	HashMap<DBEdge, Vector<MapResult>> nearestToEdges;
	/// @brief The public transport system info
	GTFSData gtfs = null;
	/// @brief A point to the currently processed source edge
	Iterator<DBEdge> nextEdgePointer;
	/// @brief A counter for seen edges for reporting purposes
	long seenEdges = 0;
	/// @brief Whether this runs in verbose mode
	boolean verbose;
	/// @brief The starting edges
	Set<DBEdge> fromEdges; // TODO: check whether one can use nearestFromEdges
	/// @brief The list of stop edges
	Set<DBEdge> toEdges; // TODO: check whether one can use nearestToEdges
	/// @brief The route weight computation function
	AbstractRoutingMeasure measure; // TODO: rename to "AbstractWeightFunction" // TODO: add documentation on github
	/// @brief The results processor
	DijkstraResultsProcessor resultsProcessor;
	/// @brief Starting time of computation
	int time;
	/// @brief Allowed modes
	long modes;
	/// @brief Initial mode
	long initMode;

	
	
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
		AbstractRoutingMeasure measure;
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
				AbstractRoutingMeasure _measure, DijkstraResultsProcessor _resultsProcessor,
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
		 * Builds the paths, first, then uses them to generate the results.
		 */
		public void run() {
			try {
				DBEdge e = null;
				do {
					e = parent.getNextStartingEdge();
					if(e==null) {
						continue;
					}
					Vector<MapResult> fromObjects = parent.nearestFromEdges.get(e);
					for(MapResult mr : fromObjects) {
						DijkstraResult ret = BoundDijkstra.run(measure, time, mr, initMode, modes, parent.toEdges, boundNumber, boundTT, boundDist, boundVar, shortestOnly);
						resultsProcessor.process(mr, ret, needsPT);
					}
				} while(e!=null);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
				SQLException e2 = e.getNextException();
				while (e2 != null) {
					e2.printStackTrace();
					e2 = e2.getNextException();
				}
			}
		}
	}
	


	// --------------------------------------------------------
	// static main methods
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
	private static CommandLine getCMDOptions(String[] args) {
		HelpFormatter lvFormater = new HelpFormatter();
		// add options
		Options lvOptions = new Options();
		lvOptions.addOption(new Option("?", "help", false, "Prints the help screen."));
		lvOptions.addOption(new Option("v", "verbose", false, "Prints what is being done."));

		lvOptions.addOption(OptionBuilder.withLongOpt("from")
				.withDescription("Defines the data source of origins.").hasArg().create("f"));
		lvOptions.addOption(OptionBuilder.withLongOpt("from-filter")
				.withDescription("Defines a filter for origins to load.").hasArg().create("F"));
		lvOptions.addOption(OptionBuilder.withLongOpt("to")
				.withDescription("Defines the data source of destinations.").hasArg().create("t"));
		lvOptions.addOption(OptionBuilder.withLongOpt("to-filter")
				.withDescription("Defines a filter for destinations to load.").hasArg().create("T"));
		lvOptions.addOption(OptionBuilder.withLongOpt("weight")
				.withDescription("An optional weighting attribute for the origins.").hasArg().create("W"));
		lvOptions.addOption(OptionBuilder.withLongOpt("variable")
				.withDescription("An optional destinations' variable to collect.").hasArg().create("V"));
		lvOptions.addOption(OptionBuilder.withLongOpt("from-agg")
				.withDescription("Defines the data source of origin aggregation areas.").hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("to-agg")
				.withDescription("Defines the data source of destination aggregation areas.").hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("epsg")
				.withDescription("The EPSG projection to use.").withType(Number.class).hasArg().create());

		lvOptions.addOption(OptionBuilder.withLongOpt("net")
				.withDescription("Defines the road network to load.").hasArg().create("n"));
		lvOptions.addOption(new Option("subnets", false, "When set, unconnected network parts are not removed."));
		lvOptions.addOption(OptionBuilder.withLongOpt("traveltimes")
				.withDescription("Defines the data source of traveltimes.").hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("pt")
				.withDescription("Defines the GTFS-based public transport representation.").hasArg().create("p"));
		lvOptions.addOption(OptionBuilder.withLongOpt("pt-boundary")
				.withDescription("Defines the data source of the boundary for the PT offer.").hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("mode")
				.withDescription("The mode to use ['passenger', 'foot', 'bicycle'].").hasArg().create("m"));
		lvOptions.addOption(new Option("requirespt", false, "When set, only information that contains a PT part are stored."));
		lvOptions.addOption(OptionBuilder.withLongOpt("pt-restriction")
				.withDescription("Restrictions to usable GTFS carriers.").hasArg().create("P"));
		lvOptions.addOption(OptionBuilder.withLongOpt("entrainment")
				.withDescription("Data source for entrainment description.").hasArg().create("E"));
		lvOptions.addOption(OptionBuilder.withLongOpt("time").withDescription("The time the trips start at in seconds.")
				.withType(Number.class).hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("date").withDescription("The date for which the accessibilities shall be computed.")
				.hasArg().create());
		lvOptions.addOption(new Option("shortest", false, "Searches only one destination per origin."));
		lvOptions.addOption(OptionBuilder.withLongOpt("max-number").withDescription("The maximum number of destinations to visit.")
				.withType(Number.class).hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("max-distance").withDescription("The maximum distance to check.")
				.withType(Number.class).hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("max-tt").withDescription("The maximum travel time to check.")
				.withType(Number.class).hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("max-variable-sum")
				.withDescription("The maximum sum of variable's values to collect.").withType(Number.class).hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("measure")
				.withDescription("The measure to use during the routing ['tt_mode', 'price_tt', 'interchanges_tt', 'maxinterchanges_tt'].").hasArg()
				.create());

		lvOptions.addOption(OptionBuilder.withLongOpt("measure-param1").withDescription("First parameter of the chosen weight function.").withType(Number.class).hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("measure-param2").withDescription("Second parameter of the chosen weight function.").withType(Number.class).hasArg().create());
		
		
		lvOptions.addOption(OptionBuilder.withLongOpt("threads").withDescription("The number of threads.")
				.withType(Number.class).hasArg().create());
		
		lvOptions.addOption(OptionBuilder.withLongOpt("origins-to-road-output")
				.withDescription("Defines output of the mapping between from-objects to the road.")
				.hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("destinations-to-road-output")
				.withDescription("Defines output of the mapping between to-objects to the road.")
				.hasArg().create());
		lvOptions.addOption(
				OptionBuilder.withLongOpt("nm-output").withDescription("Defines the n:m output.").hasArg().create("o"));
		lvOptions.addOption(
				OptionBuilder.withLongOpt("ext-nm-output").withDescription("Defines the extended n:m output.").hasArg().create());
		lvOptions.addOption(
				OptionBuilder.withLongOpt("stat-nm-output").withDescription("Defines the n:m statistics output.").hasArg().create());
		lvOptions.addOption(
				OptionBuilder.withLongOpt("interchanges-output").withDescription("Defines the interchanges output.").hasArg().create("i"));
		lvOptions.addOption(
				OptionBuilder.withLongOpt("edges-output").withDescription("Defines the edges output.").hasArg().create("e"));
		lvOptions.addOption(
				OptionBuilder.withLongOpt("pt-output").withDescription("Defines the public transport output.").hasArg().create());
		lvOptions.addOption(
				OptionBuilder.withLongOpt("direct-output").withDescription("Defines the direct output.").hasArg().create("d"));
		lvOptions.addOption(new Option("dropprevious", false, "When set, previous output with the same name is replaced."));
		/*
		lvOptions.addOption(
				OptionBuilder.withLongOpt("output-steps").withDescription("Pseudo-steps.").withType(Number.class).hasArg().create());
				*/

		lvOptions.addOption(OptionBuilder.withLongOpt("from.id")
				.withDescription("Defines the column name of the origins' ids.").hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("to.id")
				.withDescription("Defines the column name of the destinations' ids.").hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("from.geom")
				.withDescription("Defines the column name of the origins' geometries.").hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("to.geom")
				.withDescription("Defines the column name of the destinations' geometries.").hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("from-agg.id")
				.withDescription("Defines the column name of the origins aggregations' ids.").hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("to-agg.id")
				.withDescription("Defines the column name of the destination aggregations' ids.").hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("from-agg.geom")
				.withDescription("Defines the column name of the origins aggregations' geometries.").hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("to-agg.geom")
				.withDescription("Defines the column name of the destination aggregations' geometries.").hasArg().create());
		
		// parse options
		CommandLine lvCmd = null;
		try {
			CommandLineParser lvParser = new BasicParser();
			lvCmd = lvParser.parse(lvOptions, args);
			if (lvCmd.hasOption("help")) {
				lvFormater.printHelp("UrMoAccessibilityComputer", lvOptions);
				return null;
			}
			boolean check = true;
			// TODO: add information about unset options
			check &= OptionsHelper.isSet(lvCmd, "from");
			check &= OptionsHelper.isSet(lvCmd, "to");
			check &= OptionsHelper.isSet(lvCmd, "net");
			check &= OptionsHelper.isSet(lvCmd, "mode");
			check &= OptionsHelper.isSet(lvCmd, "time");
			// TODO: clarify epsg usage check &= OptionsHelper.isSet(lvCmd, "epsg"); --can be unset
			// TODO: add information about double set options
			check &= OptionsHelper.isIntegerOrUnset(lvCmd, "epsg");
			check &= OptionsHelper.isIntegerOrUnset(lvCmd, "time");
			check &= OptionsHelper.isIntegerOrUnset(lvCmd, "max-number");
			check &= OptionsHelper.isDoubleOrUnset(lvCmd, "max-distance");
			check &= OptionsHelper.isDoubleOrUnset(lvCmd, "max-tt");
			check &= OptionsHelper.isDoubleOrUnset(lvCmd, "max-variable-sum");
			//
			if(lvCmd.hasOption("pt")) {
				if(OptionsHelper.isSet(lvCmd, "date")) {
					try {
						String date = lvCmd.getOptionValue("date", "");
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
				}
			}
			//
			if(lvCmd.hasOption("measure")) {
				String t = lvCmd.getOptionValue("measure", "");
				if(!"tt_mode".equals(t)&&!"price_tt".equals(t)&&!"interchanges_tt".equals(t)&&!"maxinterchanges_tt".equals(t)) {
					System.err.println("Unknown measure '" + t + "'; allowed are: 'tt_mode', 'price_tt', 'interchanges_tt', and 'maxinterchanges_tt'.");
					check = false;
				}
			}			
			if (!check) {
				return null;
			}
		} catch (ParseException pvException) {
			lvFormater.printHelp("UrMoAccessibilityComputer", lvOptions);
			System.err.println("Parse Error:" + pvException.getMessage());
		}
		return lvCmd;
	}

	
	
	/**
	 * @brief Parses the text representation to obtain the encoded modes of transport
	 * @param optionValue The text representation
	 * @return Encoded modes of transport
	 */
	private static Vector<Mode> getModes(String optionValue) {
		Vector<Mode> ret = new Vector<>();
		String[] r = optionValue.split(";");
		for(String r1 : r) {
			Mode m = Modes.getMode(r1);
			if(m==null) {
				// TODO: add error message
				return null;
			}
			ret.add(m);
		}
		if(ret.size()==0) {
			return null;
		}
		return ret;
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
	 * @param options The options to use
	 * @return Whether everything went good
	 * @throws SQLException When accessing the database failed
	 * @throws IOException When accessing a file failed
	 * @throws ParseException When an option could not been parsed
	 * @throws com.vividsolutions.jts.io.ParseException When a geometry could not been parsed
	 */
	protected boolean init(CommandLine options) throws SQLException, IOException, ParseException, com.vividsolutions.jts.io.ParseException {
		verbose = options.hasOption("verbose");
		// -------- mode
		Modes.init();
		Vector<Mode> modesV = getModes(options.getOptionValue("mode", "<unknown>"));
		if (modesV == null) {
			throw new IOException("The mode(s) '" + options.getOptionValue("mode", "") + "' is/are not known.");
		}
		modes = Modes.getCombinedModeIDs(modesV);
		initMode = modesV.get(0).id;
		// -------- projection
		int epsg;
		if(options.hasOption("epsg")) {
			epsg = ((Long) options.getParsedOptionValue("epsg")).intValue();
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
		// from
		if (verbose) System.out.println("Reading origin places");
		Layer fromLayer = InputReader.loadLayer(options, "from", "weight", this, epsg); 
		if (verbose) System.out.println(" " + fromLayer.getObjects().size() + " origin places loaded");
		if (fromLayer.getObjects().size()==0) {
			System.out.println("Quitting.");
			return false;
		}
		// from aggregation
		Layer fromAggLayer = null;
		if (options.hasOption("from-agg") && !options.getOptionValue("from-agg", "").equals("all")) {
			if (verbose) System.out.println("Reading origin aggregation zones");
			fromAggLayer = InputReader.loadLayer(options, "from-agg", null, this, epsg); 
			if (verbose) System.out.println(" " + fromAggLayer.getObjects().size() + " origin aggregation geometries loaded");
		}
		// to
		if (verbose) System.out.println("Reading destination places");
		Layer toLayer = InputReader.loadLayer(options, "to", "variable", this, epsg); 
		if (verbose) System.out.println(" " + toLayer.getObjects().size() + " destination places loaded");
		if (toLayer.getObjects().size()==0) {
			System.out.println("Quitting.");
			return false;
		}
		// to aggregation
		Layer toAggLayer = null;
		if (options.hasOption("to-agg") && !options.getOptionValue("to-agg", "").equals("all")) {
			if (verbose) System.out.println("Reading sink aggregation zones");
			fromAggLayer = InputReader.loadLayer(options, "to-agg", null, this, epsg); 
			if (verbose) System.out.println(" " + toAggLayer.getObjects().size() + " sink aggregation geometries loaded");
		}
		
		// net
		if (verbose) System.out.println("Reading the road network");
		DBNet net = NetLoader.loadNet(this, options.getOptionValue("net", ""), epsg, modes);
		if (verbose) System.out.println(" " + net.getNumEdges() + " edges loaded (" + net.getNodes().size() + " nodes)");
		net.pruneForModes(modes); // TODO (implement, add message)
		if(!options.hasOption("subnets")) {
			if (verbose) System.out.println("Checking for connectivity...");
			net.dismissUnconnectedEdges(false);
			if (verbose) System.out.println(" " + net.getNumEdges() + " remaining after removing unconnected ones.");
		}
		/*
		if (worker.verbose)
			System.out.println(" " + net.getNumEdges() + " remaining after prunning");
		*/

		// travel times
		if (options.hasOption("traveltimes")) {
			if (verbose) System.out.println("Reading the roads' travel times");
			NetLoader.loadTravelTimes(net, options.getOptionValue("traveltimes", ""), verbose);
		}
		
		// entrainment
		EntrainmentMap entrainmentMap = new EntrainmentMap();
		if (options.hasOption("entrainment")) {
			if (verbose) System.out.println("Reading entrainment table");
			entrainmentMap = InputReader.loadEntrainment(options);
			if (verbose) System.out.println(" " + entrainmentMap.carrier2carried.size() + " entrainment fields loaded");
		}
		
		// public transport network
		if (options.hasOption("pt")) {
			if (verbose) System.out.println("Reading the public transport network");
			Geometry bounds = null;
			if (options.hasOption("pt-boundary")) {
				bounds = InputReader.loadGeometry(options.getOptionValue("pt-boundary", ""), "pt-boundary", epsg);
			}
			gtfs = GTFSReader.load(options, bounds, net, entrainmentMap, epsg, verbose);
			if (verbose) System.out.println(" loaded");
		}

		// -------- compute (and optionally write) nearest edges
		// compute nearest edges
		if (verbose) System.out.println("Computing access from the origins to the network");
		NearestEdgeFinder nef1 = new NearestEdgeFinder(fromLayer.getObjects(), net, initMode);
		nearestFromEdges = nef1.getNearestEdges(false);
		if (options.hasOption("origins-to-road-output")) {
			OutputBuilder.writeEdgeAllocation(options.getOptionValue("origins-to-road-output", ""), nearestFromEdges, epsg, options.hasOption("dropprevious"));
		}
		if (verbose) System.out.println("Computing egress from the network to the destinations");
		NearestEdgeFinder nef2 = new NearestEdgeFinder(toLayer.getObjects(), net, initMode);
		nearestToEdges = nef2.getNearestEdges(true);
		if (options.hasOption("destinations-to-road-output")) {
			OutputBuilder.writeEdgeAllocation(options.getOptionValue("destinations-to-road-output", ""), nearestToEdges, epsg, options.hasOption("dropprevious"));
		}

		// -------- instantiate outputs
/*
		worker.aggregators = new HashMap<>();
		if (!"".equals(options.getOptionValue("nm-output", ""))) {
			String comment = buildComment(options);
			if ("".equals(options.getOptionValue("output-steps", ""))) {
				BasicWriter writer = buildNMOutput(options.getOptionValue("nm-output", ""));
				writer.addComment(comment);
				agg.addOutput(writer);
				worker.aggregators.put(-1d, agg);
			} else {
				// steps is hard coded
				int steps = (int) ((Double) options.getParsedOptionValue("output-steps")).doubleValue();
				steps = 12;
				for(int i=1; i<steps+1; ++i) {
					Aggregator nagg = agg.duplicate();
					BasicWriter writer = buildNMOutput2(options.getOptionValue("nm-output", ""), "_"+300*i);
					writer.addComment(comment);
					nagg.addOutput(writer);
					worker.aggregators.put((double) (300*i), nagg);
				}
			}
		}
		*/
		// -------- build outputs
		Vector<Aggregator> aggregators = OutputBuilder.buildOutputs(options, fromLayer, fromAggLayer, toLayer, toAggLayer);
		DirectWriter dw = OutputBuilder.buildDirectOutput(options, epsg, nearestToEdges);
		time = ((Long) options.getParsedOptionValue("time")).intValue();
		resultsProcessor = new DijkstraResultsProcessor(time, dw, aggregators, nearestFromEdges, nearestToEdges); 

		// -------- measure
		measure = new RoutingMeasure_TT_Modes();
		if(options.hasOption("measure")) {
			String t = options.getOptionValue("measure", "");
			if("price_tt".equals(t)) {
				measure = new RoutingMeasure_Price_TT();
			} else if("interchanges_tt".equals(t)) {
				measure = new RoutingMeasure_ExpInterchange_TT(
						((Double) options.getParsedOptionValue("measure-param1")).doubleValue(), 
						((Double) options.getParsedOptionValue("measure-param2")).doubleValue());
			} else if("maxinterchanges_tt".equals(t)) {
				measure = new RoutingMeasure_MaxInterchange_TT(
						(int) ((Long) options.getParsedOptionValue("measure-param1")).longValue());
			}
		}
		// done everything
		return true;
	}
	
	
	
	/**
	 * @brief Performs the computation
	 * 
	 * The limits and the number of threads to use are read from options.
	 * The computation threads are initialised and started.
	 * 
	 * @param options The options to use
	 * @return Whether everything went good
	 * @throws SQLException When accessing the database failed
	 * @throws IOException When accessing a file failed
	 * @throws ParseException When an option could not been parsed
	 */
	protected boolean run(CommandLine options) throws ParseException, SQLException, IOException {
		if (verbose) System.out.println("Computing shortest paths");
		fromEdges = nearestFromEdges.keySet();
		toEdges = nearestToEdges.keySet();
		int maxNumber = options.hasOption("max-number") ? ((Long) options.getParsedOptionValue("max-number")).intValue() : -1;
		double maxTT = options.hasOption("max-tt") ? ((Double) options.getParsedOptionValue("max-tt")).doubleValue() : -1;
		double maxDistance = options.hasOption("max-distance") ? ((Double) options.getParsedOptionValue("max-distance")).doubleValue() : -1;
		double maxVar = options.hasOption("max-variable-sum") ? ((Double) options.getParsedOptionValue("max-variable-sum")).doubleValue() : -1;
		boolean shortestOnly = options.hasOption("shortest");
		boolean needsPT = options.hasOption("requirespt");
		if (verbose) System.out.println(" between " + fromEdges.size() + " origin and " + toEdges.size() + " destination edges");
		
		// initialise threads
		int numThreads = options.hasOption("threads") ? ((Long) options.getParsedOptionValue("threads")).intValue() : 1;
		nextEdgePointer = fromEdges.iterator();
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
				e.printStackTrace();
			}
		}
		System.out.println(""); // progress ends
		resultsProcessor.finish();
		return true;
	}
	
	
	/**
	 * @brief The main method
	 * @param args Given command line arguments
	 */
	public static void main(String[] args) {
		// parse and check options
		CommandLine options = getCMDOptions(args);
		if (options == null) {
			return;
		}
		try {
			// set up the db connection
			UrMoAccessibilityComputer worker = new UrMoAccessibilityComputer();
			// initialise (load data and stuff)
			if(!worker.init(options)) {
				return;
			}
			// compute
			worker.run(options);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
			SQLException e2 = e.getNextException();
			while (e2 != null) {
				e2.printStackTrace();
				e2 = e2.getNextException();
			}
		} catch (com.vividsolutions.jts.io.ParseException e) {
			e.printStackTrace();
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		// -------- finish
		System.out.println("done.");
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
		if (verbose)
			System.out.print("\r " + seenEdges + " of " + fromEdges.size() + " edges");
		return nextEdge;
	}

	
	
	/**
	 * @brief Returns a running (auto-incremented) number
	 * @return A running number
	 */
	@Override
	public synchronized long getNextRunningID() {
		return ++runningID;
	}

	
}

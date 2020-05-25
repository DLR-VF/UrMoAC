/**
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * 
 * Copyright © 2016-2019 German Aerospace Center 
 * All rights reserved
 */
package de.dlr.ivf.urmo;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

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
import de.dlr.ivf.urmo.router.io.DBIOHelper;
import de.dlr.ivf.urmo.router.io.DBNetLoader;
import de.dlr.ivf.urmo.router.io.GTFSDBReader;
import de.dlr.ivf.urmo.router.modes.EntrainmentMap;
import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;
import de.dlr.ivf.urmo.router.output.Aggregator;
import de.dlr.ivf.urmo.router.output.DijkstraResultsProcessor;
import de.dlr.ivf.urmo.router.output.DirectWriter;
import de.dlr.ivf.urmo.router.output.EdgeMappingWriter;
import de.dlr.ivf.urmo.router.output.MeasurementGenerator;
import de.dlr.ivf.urmo.router.output.edge_use.EUMeasuresGenerator;
import de.dlr.ivf.urmo.router.output.edge_use.EUSingleResult;
import de.dlr.ivf.urmo.router.output.edge_use.EUWriter;
import de.dlr.ivf.urmo.router.output.interchanges.InterchangeMeasuresGenerator;
import de.dlr.ivf.urmo.router.output.interchanges.InterchangeSingleResult;
import de.dlr.ivf.urmo.router.output.interchanges.InterchangeWriter;
import de.dlr.ivf.urmo.router.output.od.ODMeasuresGenerator;
import de.dlr.ivf.urmo.router.output.od.ODSingleResult;
import de.dlr.ivf.urmo.router.output.od.ODWriter;
import de.dlr.ivf.urmo.router.output.odext.ODExtendedMeasuresGenerator;
import de.dlr.ivf.urmo.router.output.odext.ODExtendedWriter;
import de.dlr.ivf.urmo.router.output.odext.ODSingleExtendedResult;
import de.dlr.ivf.urmo.router.output.odstats.ODSingleStatsResult;
import de.dlr.ivf.urmo.router.output.odstats.ODStatsMeasuresGenerator;
import de.dlr.ivf.urmo.router.output.odstats.ODStatsWriter;
import de.dlr.ivf.urmo.router.output.ptod.PTODMeasuresGenerator;
import de.dlr.ivf.urmo.router.output.ptod.PTODSingleResult;
import de.dlr.ivf.urmo.router.output.ptod.PTODWriter;
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
 * @copyright (c) DLR 2016-2019
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
	Set<DBEdge> fromEdges;
	/// @brief The list of stop edges
	Set<DBEdge> toEdges;

	
	
	// --------------------------------------------------------
	// inner classes
	// --------------------------------------------------------
	/** @class ComputingThread
	 * 
	 * A thread which polls for new items, computes the accessibility and
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
		 * Builds tha paths, first, then uses them to generate the results.
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
	// main class methods
	// --------------------------------------------------------
	/**
	 * @brief Constructor
	 */
	public UrMoAccessibilityComputer() {
		super();
	}

	
	/**
	 * @brief Returns a running (auto-incremented) number
	 * @return A running number
	 */
	@Override
	public synchronized long getNextRunningID() {
		return ++runningID;
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
				.withDescription("Restrictions to usable GTFS modes.").hasArg().create("P"));
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

		lvOptions.addOption(OptionBuilder.withLongOpt("measure-param1").withDescription("!!!!.").withType(Number.class).hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("measure-param2").withDescription("!!!!.").withType(Number.class).hasArg().create());
		
		
		lvOptions.addOption(OptionBuilder.withLongOpt("threads").withDescription("The number of threads.")
				.withType(Number.class).hasArg().create());
		
		lvOptions.addOption(OptionBuilder.withLongOpt("origins-to-road-output")
				.withDescription("The name of the file to write the mapping between from-objects to the road to.")
				.hasArg().create());
		lvOptions.addOption(OptionBuilder.withLongOpt("destinations-to-road-output")
				.withDescription("The name of the file to write the mapping between to-objects to the road to.")
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
		lvOptions.addOption(
				OptionBuilder.withLongOpt("output-steps").withDescription("!!!Pseudo-steps.").withType(Number.class).hasArg().create());

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
		
		/*
		lvOptions.addOption(
				OptionBuilder.withLongOpt("n-output").withDescription("Define the n:-output.").hasArg().create());
		*/
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
			// !!! add information about unset options
			check &= OptionsHelper.isSet(lvCmd, "from");
			check &= OptionsHelper.isSet(lvCmd, "to");
			check &= OptionsHelper.isSet(lvCmd, "net");
			check &= OptionsHelper.isSet(lvCmd, "mode");
			check &= OptionsHelper.isSet(lvCmd, "time");
			//check &= OptionsHelper.isSet(lvCmd, "epsg"); --can be unset
			// !!! add information about double set options
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
			Modes.init();
			//
			worker.verbose = options.hasOption("verbose");
			boolean dropExistingTables = options.hasOption("dropprevious");

			// -------- mode
			Vector<Mode> modesV = getModes(options.getOptionValue("mode", "<unknown>"));
			if (modesV == null) {
				throw new IOException("The mode(s) '" + options.getOptionValue("mode", "") + "' is/are not known.");
			}
			long modes = Modes.getCombinedModeIDs(modesV);
			long initMode = modesV.get(0).id;

			// -------- loading
			int epsg;
			if(options.hasOption("epsg")) {
				epsg= ((Long) options.getParsedOptionValue("epsg")).intValue();
			}
			else {//automatic epsg-value
				String ep[] = DBIOHelper.parseOption(options.getOptionValue("from", ""));
				epsg = DBIOHelper.findUTMZone(ep[0], ep[1], ep[2], ep[3],options.getOptionValue("from.geom", "the_geom"));
				if(epsg==-1) {
					System.out.println("Could not find a valid UTM-Zone. Quitting");
					return;
				}
				else {
					String utmZone;
					if(epsg>32600) { //northern hemisphere
						utmZone = Integer.toString(epsg%100)+"N";
					}
					else { // southern hemisphere
						utmZone = Integer.toString(epsg%100)+"S";
					}
					System.out.println("Using UTM-Zone "+utmZone+", EPSG-code: "+epsg);
				}
			}
			
			// from
			if (worker.verbose)
				System.out.println("Reading origin places");
			String fd[] = DBIOHelper.parseOption(options.getOptionValue("from", ""));
			String fromFilter = options.getOptionValue("from-filter", "");
			String weight = options.getOptionValue("weight", "");
			Layer fromLayer = DBIOHelper.load(fd[0], fd[1], fd[2], fd[3], fromFilter, weight, 
					options.getOptionValue("from.id", "gid"), options.getOptionValue("from.geom", "the_geom"), 
					"from", worker, epsg);
			if (worker.verbose)
				System.out.println(" " + fromLayer.getObjects().size() + " origin places loaded");
			if (fromLayer.getObjects().size()==0) {
				System.out.println("Quitting.");
				return;
			}
			// from aggregation
			Layer fromAggLayer = null;
			if (options.hasOption("from-agg") && !options.getOptionValue("from-agg", "").equals("all")) {
				if (worker.verbose)
					System.out.println("Reading origin aggregation zones");
				String ad[] = DBIOHelper.parseOption(options.getOptionValue("from-agg", ""));
				fromAggLayer = DBIOHelper.load(ad[0], ad[1], ad[2], ad[3], "", "", 
						options.getOptionValue("from-agg.id", "gid"), options.getOptionValue("from-agg.geom", "the_geom"), 
						"from-agg", worker, epsg);
				if (worker.verbose)
					System.out.println(" " + fromAggLayer.getObjects().size() + " origin aggregation geometries loaded");
			}
			// to
			if (worker.verbose)
				System.out.println("Reading destination places");
			String td[] = DBIOHelper.parseOption(options.getOptionValue("to", ""));
			String toFilter = options.getOptionValue("to-filter", "");
			String var = options.getOptionValue("variable", "");
			Layer toLayer = DBIOHelper.load(td[0], td[1], td[2], td[3], toFilter, var,  
					options.getOptionValue("to.id", "gid"), options.getOptionValue("to.geom", "the_geom"), 
					"to", worker, epsg);
			if (worker.verbose)
				System.out.println(" " + toLayer.getObjects().size() + " destination places loaded");
			if (toLayer.getObjects().size()==0) {
				System.out.println("Quitting.");
				return;
			}
			// to aggregation
			Layer toAggLayer = null;
			if (options.hasOption("to-agg") && !options.getOptionValue("to-agg", "").equals("all")) {
				if (worker.verbose)
					System.out.println("Reading sink aggregation zones");
				String ad[] = DBIOHelper.parseOption(options.getOptionValue("to-agg", ""));
				toAggLayer = DBIOHelper.load(ad[0], ad[1], ad[2], ad[3], "", "", 
						options.getOptionValue("to-agg.id", "gid"), options.getOptionValue("to-agg.geom", "the_geom"), 
						"to-agg", worker, epsg);
				if (worker.verbose)
					System.out.println(" " + toAggLayer.getObjects().size() + " sink aggregation geometries loaded");
			}
			// net
			if (worker.verbose)
				System.out.println("Reading the road network");
			String nd[] = DBIOHelper.parseOption(options.getOptionValue("net", ""));
			DBNet net = DBNetLoader.loadNet(worker, nd[0], nd[1], nd[2], nd[3], epsg, modes);
			if (worker.verbose)
				System.out.println(" " + net.getEdges().size() + " edges loaded (" + net.getNodes().size() + " nodes)");
			net.pruneForModes(modes); // TODO (implement, add message)
			if(!options.hasOption("subnets")) {
				net.dismissUnconnectedEdges();
			}
			if (worker.verbose)
				System.out.println(" " + net.getEdges().size() + " remaining after prunning");
			// travel times
			if (options.hasOption("traveltimes")) {
				if (worker.verbose)
					System.out.println("Reading the roads' travel times");
				String ttd[] = DBIOHelper.parseOption(options.getOptionValue("traveltimes", ""));
				DBNetLoader.loadTravelTimes(net, ttd[0], ttd[1], ttd[2], ttd[3], worker.verbose);
			}
			// entrainment
			EntrainmentMap entrainmentMap = new EntrainmentMap();
			if (options.hasOption("entrainment")) {
				if (worker.verbose)
					System.out.println("Reading entrainment table");
				String ed[] = DBIOHelper.parseOption(options.getOptionValue("entrainment", ""));
				entrainmentMap = DBIOHelper.loadEntrainment(ed[0], ed[1], ed[2], ed[3], "");
				if (worker.verbose)
					System.out.println(" " + entrainmentMap.carrier2carried.size() + " entrainment fields loaded");
			}
			// public transport network
			if (options.hasOption("pt")) {
				if (worker.verbose)
					System.out.println("Reading the public transport network");
				Geometry bounds = null;
				if (options.hasOption("pt-boundary")) {
					String gd[] = DBIOHelper.parseOption(options.getOptionValue("pt-boundary", ""));
					bounds = DBIOHelper.loadGeometry(gd[0], gd[1], gd[2], gd[3], "the_geom", epsg);
				}
				String pd[] = DBIOHelper.parseOption(options.getOptionValue("pt", ""));
				worker.gtfs = GTFSDBReader.load(pd[0], pd[1], pd[2], pd[3], options.getOptionValue("pt-restriction", ""), options.getOptionValue("date", ""), bounds, net, entrainmentMap, epsg, worker.verbose);
				if (worker.verbose)
					System.out.println(" loaded");
			}

			// -------- compute (and optionally write) nearest edges
			// compute nearest edges
			if (worker.verbose)
				System.out.println("Computing access from the origins to the network");
			NearestEdgeFinder nef1 = new NearestEdgeFinder(fromLayer.getObjects(), net, initMode);
			worker.nearestFromEdges = nef1.getNearestEdges(false);
			if (options.hasOption("origins-to-road-output")) {
				writeEdgeAllocation(options.getOptionValue("origins-to-road-output", ""), worker.nearestFromEdges, epsg, dropExistingTables);
			}
			if (worker.verbose)
				System.out.println("Computing egress from the network to the destinations");
			NearestEdgeFinder nef2 = new NearestEdgeFinder(toLayer.getObjects(), net, initMode);
			worker.nearestToEdges = nef2.getNearestEdges(true);
			if (options.hasOption("destinations-to-road-output")) {
				writeEdgeAllocation(options.getOptionValue("destinations-to-road-output", ""), worker.nearestToEdges, epsg, dropExistingTables);
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
			// -------- !!!
			Vector<Aggregator> aggregators = new Vector<>();
			String comment = buildComment(options);
			if (!"".equals(options.getOptionValue("nm-output", ""))) {
				ODMeasuresGenerator mgNM = new ODMeasuresGenerator();
				AbstractResultsWriter<ODSingleResult> writer = buildNMOutput(options.getOptionValue("nm-output", ""), dropExistingTables);
				Aggregator<ODSingleResult> agg = buildAggregator(mgNM, options.hasOption("shortest"), 
						options.getOptionValue("from-agg", ""), options.getOptionValue("to-agg", ""),
						fromLayer, fromAggLayer, toLayer, toAggLayer, writer, comment);
				aggregators.add(agg);
			}
			if (!"".equals(options.getOptionValue("ext-nm-output", ""))) {
				ODExtendedMeasuresGenerator mg = new ODExtendedMeasuresGenerator();
				AbstractResultsWriter<ODSingleExtendedResult> writer = buildExtNMOutput(options.getOptionValue("ext-nm-output", ""), dropExistingTables);
				Aggregator<ODSingleExtendedResult> agg = buildAggregator(mg, options.hasOption("shortest"), 
						options.getOptionValue("from-agg", ""), options.getOptionValue("to-agg", ""),
						fromLayer, fromAggLayer, toLayer, toAggLayer, writer, comment);
				aggregators.add(agg);
			}
			if (!"".equals(options.getOptionValue("stat-nm-output", ""))) {
				ODStatsMeasuresGenerator mg = new ODStatsMeasuresGenerator();
				AbstractResultsWriter<ODSingleStatsResult> writer = buildStatNMOutput(options.getOptionValue("stat-nm-output", ""), dropExistingTables);
				Aggregator<ODSingleStatsResult> agg = buildAggregator(mg, options.hasOption("shortest"), 
						options.getOptionValue("from-agg", ""), options.getOptionValue("to-agg", ""),
						fromLayer, fromAggLayer, toLayer, toAggLayer, writer, comment);
				aggregators.add(agg);
			}
			if (!"".equals(options.getOptionValue("interchanges-output", ""))) {
				InterchangeMeasuresGenerator mg = new InterchangeMeasuresGenerator();
				AbstractResultsWriter<InterchangeSingleResult> writer = buildInterchangeOutput(options.getOptionValue("interchanges-output", ""), dropExistingTables);
				Aggregator<InterchangeSingleResult> agg = buildAggregator(mg, options.hasOption("shortest"), 
						options.getOptionValue("from-agg", ""), options.getOptionValue("to-agg", ""),
						fromLayer, fromAggLayer, toLayer, toAggLayer, writer, comment);
				aggregators.add(agg);
			}
			if (!"".equals(options.getOptionValue("edges-output", ""))) {
				EUMeasuresGenerator mg = new EUMeasuresGenerator();
				AbstractResultsWriter<EUSingleResult> writer = buildEUOutput(options.getOptionValue("edges-output", ""), dropExistingTables);
				Aggregator<EUSingleResult> agg = buildAggregator(mg, options.hasOption("shortest"), 
						options.getOptionValue("from-agg", ""), options.getOptionValue("to-agg", ""),
						fromLayer, fromAggLayer, toLayer, toAggLayer, writer, comment);
				aggregators.add(agg);
			}
			if (!"".equals(options.getOptionValue("pt-output", ""))) {
				PTODMeasuresGenerator mg = new PTODMeasuresGenerator();
				AbstractResultsWriter<PTODSingleResult> writer = buildPTODOutput(options.getOptionValue("pt-output", ""), dropExistingTables);
				Aggregator<PTODSingleResult> agg = buildAggregator(mg, options.hasOption("shortest"), 
						options.getOptionValue("from-agg", ""), options.getOptionValue("to-agg", ""),
						fromLayer, fromAggLayer, toLayer, toAggLayer, writer, comment);
				aggregators.add(agg);
			}
			DirectWriter dw = null;
			if (!"".equals(options.getOptionValue("direct-output", ""))) {
				dw = buildDirectOutput(options.getOptionValue("direct-output", ""), epsg, worker.nearestToEdges, dropExistingTables);
				dw.addComment(comment);
			}
			int time = ((Long) options.getParsedOptionValue("time")).intValue();
			DijkstraResultsProcessor resultsProcessor = new DijkstraResultsProcessor(time, dw, aggregators, worker.nearestFromEdges, worker.nearestToEdges); 

			// -------- measure
			AbstractRoutingMeasure measure = new RoutingMeasure_TT_Modes();
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
			
			// -------- compute 
			// TODO optional report on nearest edges
			// compute distances / travel times, save them
			if (worker.verbose)
				System.out.println("Computing shortest paths");
			worker.fromEdges = worker.nearestFromEdges.keySet();
			worker.toEdges = worker.nearestToEdges.keySet();
			int maxNumber = options.hasOption("max-number") ? ((Long) options.getParsedOptionValue("max-number")).intValue() : -1;
			double maxTT = options.hasOption("max-tt") ? ((Double) options.getParsedOptionValue("max-tt")).doubleValue() : -1;
			double maxDistance = options.hasOption("max-distance") ? ((Double) options.getParsedOptionValue("max-distance")).doubleValue() : -1;
			double maxVar = options.hasOption("max-variable-sum") ? ((Double) options.getParsedOptionValue("max-variable-sum")).doubleValue() : -1;
			boolean shortestOnly = options.hasOption("shortest");
			boolean needsPT = options.hasOption("requirespt");
			if (worker.verbose) {
				System.out.println(" between " + worker.fromEdges.size() + " origin and " + worker.toEdges.size() + " destination edges");
			}
			
			// initialise threads
			int numThreads = options.hasOption("threads") ? ((Long) options.getParsedOptionValue("threads")).intValue() : 1;
			worker.nextEdgePointer = worker.fromEdges.iterator();
			worker.seenEdges = 0;
			Vector<Thread> threads = new Vector<>();
			//System.out.println(""); // starting (progress follows)
			for (int i=0; i<numThreads; ++i) {
				Thread t = new Thread(new ComputingThread(worker, needsPT, measure, resultsProcessor, time, initMode, modes, maxNumber, maxTT, maxDistance, maxVar, shortestOnly));
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
	 * @brief Builds a comment string that shows the set options
	 * @param options The options to encode
	 * @return A comment string with set options
	 */
	private static String buildComment(CommandLine options) {
		StringBuffer sb = new StringBuffer();
		sb.append("Generated using UrMoAC with the following options:\n");
		Option[] args = options.getOptions();
		for(Option argO : args) {
			if(!"".equals(argO.getValue(""))) {
				String value = argO.getValue();
				value = value.replace("'", "''");
				if(value.indexOf("jdbc")>=0) {
					value = value.substring(0, value.lastIndexOf(";")+1) + "---";
				}
				sb.append("--").append(argO.getLongOpt()).append(' ').append(value).append('\n');
			}
		}
		return sb.toString();
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
				// !!! add error message
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
	 * @brief Builds a ODSingleResult-output
	 * @param d The output storage definition
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<ODSingleResult> buildNMOutput(String d, boolean dropPrevious) throws SQLException, IOException {
		String[] r = checkOutput(d, "nm-output");
		if (r[0].equals("db")) {
			return new ODWriter(r[1], r[2], r[3], r[4], dropPrevious);
		} else {
			return new ODWriter(r[1]);
		}
	}

	/**
	 * @brief Builds a ODSingleExtendedResult-output
	 * @param d The output storage definition
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<ODSingleExtendedResult> buildExtNMOutput(String d, boolean dropPrevious) throws SQLException, IOException {
		String[] r = checkOutput(d, "ext-nm-output");
		if (r[0].equals("db")) {
			return new ODExtendedWriter(r[1], r[2], r[3], r[4], dropPrevious);
		} else {
			return new ODExtendedWriter(r[1]);
		}
	}

	/**
	 * @brief Builds a ODSingleStatsResult-output
	 * @param d The output storage definition
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<ODSingleStatsResult> buildStatNMOutput(String d, boolean dropPrevious) throws SQLException, IOException {
		String[] r = checkOutput(d, "stat-nm-output");
		if (r[0].equals("db")) {
			return new ODStatsWriter(r[1], r[2], r[3], r[4], dropPrevious);
		} else {
			return new ODStatsWriter(r[1]);
		}
	}

	/**
	 * @brief Builds a InterchangeSingleResult-output
	 * @param d The output storage definition
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<InterchangeSingleResult> buildInterchangeOutput(String d, boolean dropPrevious) throws SQLException, IOException {
		String[] r = checkOutput(d, "interchanges-output");
		if (r[0].equals("db")) {
			return new InterchangeWriter(r[1], r[2], r[3], r[4], dropPrevious);
		} else {
			return new InterchangeWriter(r[1]);
		}
	}

	/**
	 * @brief Builds a EUSingleResult-output
	 * @param d The output storage definition
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<EUSingleResult> buildEUOutput(String d, boolean dropPrevious) throws SQLException, IOException {
		String[] r = checkOutput(d, "edges-output");
		if (r[0].equals("db")) {
			return new EUWriter(r[1], r[2], r[3], r[4], dropPrevious);
		} else {
			return new EUWriter(r[1]);
		}
	}
	
	/**
	 * @brief Builds a PTODSingleResult-output
	 * @param d The output storage definition
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<PTODSingleResult> buildPTODOutput(String d, boolean dropPrevious) throws SQLException, IOException {
		String[] r = checkOutput(d, "pt-output");
		if (r[0].equals("db")) {
			return new PTODWriter(r[1], r[2], r[3], r[4], dropPrevious);
		} else {
			return new PTODWriter(r[1]);
		}
	}
	
	/**
	 * @brief Builds a "direct"-output
	 * @param d The output storage definition
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	private static DirectWriter buildDirectOutput(String d, int rsid, HashMap<DBEdge, Vector<MapResult>> nearestToEdges, boolean dropPrevious) throws SQLException, IOException {
		String[] r = checkOutput(d, "direct-output");
		if (r[0].equals("db")) {
			return new DirectWriter(r[1], r[2], r[3], r[4], rsid, nearestToEdges, dropPrevious);
		} else {
			return new DirectWriter(r[1], rsid, nearestToEdges);
		}
	}
	

	// --------------------------------------------------------
	// static helper methods
	// --------------------------------------------------------
	/**
	 * @brief Writes the connections from objects to the road network
	 * 
	 *        Runs through the given objects. If the object is connected to the
	 *        road network, the written line contains the object's ID, the edge
	 *        ID, and coordinates of the object and the road position.
	 *        Otherwise, only the object ID is written the rest is filled with
	 *        -1.
	 * @param d Definition about where to write to
	 * @param nearestEdges The map of objects to road positions
	 * @throws IOException If the file cannot be written
	 * @throws SQLException
	 */
	private static void writeEdgeAllocation(String d, HashMap<DBEdge, Vector<MapResult>> nearestEdges, int epsg, boolean dropPrevious)
			throws IOException, SQLException {
		String[] r = checkOutput(d, "X-to-road-output");
		EdgeMappingWriter emw = null;
		if (r[0].equals("db")) {
			if(r.length!=5) {
				throw new IOException("False database definition; should be 'db;<connector_host>;<table>;<user>;<password>'.");
			}
			emw = new EdgeMappingWriter(r[1], r[2], r[3], r[4], epsg, dropPrevious);
		} else {
			emw = new EdgeMappingWriter(r[1]);
		}
		try {
			emw.writeResults(nearestEdges);
			emw.close();
		} catch (FactoryException | TransformException e) {
			e.printStackTrace();
		}
	}


	/**
	 * @brief Returns the next edge to process
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
	 * @brief Parses the definition of the output storage and verfies it
	 * @param input The definition of an output storage (database / file)
	 * @param outputName The name of the definition
	 * @return The split (and verified) definition
	 * @throws IOException When something fails
	 */
	private static String[] checkOutput(String input, String outputName) throws IOException {
		String[] r = input.split(";");
		if (r[0].equals("db")) {
			if(r.length!=5) {
				throw new IOException("False database definition for '" + outputName + "'\nshould be 'db;<connector_host>;<table>;<user>;<password>'.");
			}
		} else {
			if(r.length!=2) {
				throw new IOException("False file definition for '" + outputName + "'\nshould be 'file;<filename>'.");
			}
		}
		return r;
	}
	
	
	/**
	 * @brief Brief builds the results processing aggregator
	 * @param measuresGenerator The measures generator to use
	 * @param shortest Whether only the shortest connection shall be computed
	 * @param fromAgg The origins aggregation layer name
	 * @param toAgg The destinations aggregation layer name
	 * @param fromLayer The origins layer
	 * @param fromAggLayer The origins aggregation layer
	 * @param toLayer The destinations layer
	 * @param toAggLayer The destinations aggregation layer
	 * @param writer The writer to use
	 * @param comment The comment to add
	 * @return The built aggregator
	 * @throws SQLException When something fails
	 */
	private static <T extends AbstractSingleResult> Aggregator<T> buildAggregator(MeasurementGenerator measuresGenerator,
			boolean shortest, String fromAgg, String toAgg, 
			Layer fromLayer, Layer fromAggLayer, Layer toLayer, Layer toAggLayer,
			AbstractResultsWriter<T> writer, String comment) throws SQLException {
		Aggregator<T> agg = new Aggregator<T>(measuresGenerator, fromLayer, shortest);
		if (fromAggLayer != null) {
			agg.setOriginAggregation(fromLayer, fromAggLayer);
		} else if (fromAgg.equals("all")) {
			agg.sumOrigins();
		}
		if (toAggLayer != null) {
			agg.setDestinationAggregation(toLayer, toAggLayer);
		} else if (toAgg.equals("all")) {
			agg.sumDestinations();
		}
		agg.buildMeasurementsMap(fromLayer, toLayer);
		agg.addOutput(writer);
		writer.addComment(comment);
		return agg;
	}

	
}

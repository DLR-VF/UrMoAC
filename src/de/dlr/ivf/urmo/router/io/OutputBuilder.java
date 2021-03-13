/**
 * Copyright (c) 2016-2020 DLR Institute of Transport Research
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
package de.dlr.ivf.urmo.router.io;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;
import de.dlr.ivf.urmo.router.output.Aggregator;
import de.dlr.ivf.urmo.router.output.DirectWriter;
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
import de.dlr.ivf.urmo.router.shapes.Layer;

public class OutputBuilder {

	public static Vector<Aggregator> buildOutputs(CommandLine options, Layer fromLayer, Layer fromAggLayer, Layer toLayer, Layer toAggLayer) throws SQLException, IOException {
		Vector<Aggregator> aggregators = new Vector<>();
		boolean dropExistingTables = options.hasOption("dropprevious");
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
		return aggregators;
	}
	
	
	/**
	 * @brief Builds a "direct"-output
	 * @param !!!
	 * @param !!!
	 * @return The built output
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	public static DirectWriter buildDirectOutput(CommandLine options, int rsid, HashMap<DBEdge, Vector<MapResult>> nearestToEdges) throws SQLException, IOException {
		String d = options.getOptionValue("direct-output", "");
		if ("".equals(d)) {
			return null;
		}
		String[] r = Utils.checkDefinition(d, "direct-output");
		DirectWriter dw = null;
		if (r[0].equals("db")) {
			dw = new DirectWriter(r[1], r[2], r[3], r[4], rsid, nearestToEdges, options.hasOption("dropprevious"));
		} else {
			dw = new DirectWriter(r[1], rsid, nearestToEdges);
		}
		dw.addComment(buildComment(options));
		return dw;
	}



	/**
	 * @brief Builds a comment string that shows the set options
	 * @param options The options to encode
	 * @return A comment string with set options
	 */
	public static String buildComment(CommandLine options) {
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

	
	/**
	 * @brief Builds a ODSingleResult-output
	 * @param d The output storage definition
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws SQLException When something fails
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<ODSingleResult> buildNMOutput(String d, boolean dropPrevious) throws SQLException, IOException {
		String[] r = Utils.checkDefinition(d, "nm-output");
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
		String[] r = Utils.checkDefinition(d, "ext-nm-output");
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
		String[] r = Utils.checkDefinition(d, "stat-nm-output");
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
		String[] r = Utils.checkDefinition(d, "interchanges-output");
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
		String[] r = Utils.checkDefinition(d, "edges-output");
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
		String[] r = Utils.checkDefinition(d, "pt-output");
		if (r[0].equals("db")) {
			return new PTODWriter(r[1], r[2], r[3], r[4], dropPrevious);
		} else {
			return new PTODWriter(r[1]);
		}
	}
	
}

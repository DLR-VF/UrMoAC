/*
 * Copyright (c) 2016-2024
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
package de.dlr.ivf.urmo.router.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import de.dks.utils.options.OptionsCont;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.output.AbstractResultsWriter;
import de.dlr.ivf.urmo.router.output.AbstractSingleResult;
import de.dlr.ivf.urmo.router.output.Aggregator;
import de.dlr.ivf.urmo.router.output.DirectWriter;
import de.dlr.ivf.urmo.router.output.EdgeMappingWriter;
import de.dlr.ivf.urmo.router.output.MeasurementGenerator;
import de.dlr.ivf.urmo.router.output.NetClusterWriter;
import de.dlr.ivf.urmo.router.output.NetErrorsWriter;
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

/**
 * @class OutputBuilder
 * @brief Loads the road network stored in a database or files
 * @author Daniel Krajzewicz
 */
public class OutputBuilder {
	/** @brief Builds outputs as defined in the given options
	 * @param options The options to use for parsing
	 * @param fromLayer The origins
	 * @param fromAggLayer The origin aggregation data
	 * @param toLayer  The destinations
	 * @param toAggLayer The destination aggregation data
	 * @param epsg The projection
	 * @return Built output devices
	 * @throws IOException When something fails
	 */
	@SuppressWarnings("rawtypes")
	public static Vector<Aggregator> buildOutputs(OptionsCont options, Layer fromLayer, Layer fromAggLayer, 
			Layer toLayer, Layer toAggLayer, int epsg) throws IOException {
		Vector<Aggregator> aggregators = new Vector<>();
		boolean dropExistingTables = options.getBool("dropprevious");
		boolean aggAllFrom = options.isSet("from-agg") && options.getString("from-agg").equals("all");
		boolean aggAllTo = options.isSet("to-agg") && options.getString("to-agg").equals("all");
		int precision = options.getInteger("precision");
		String comment = options.getBool("comment") ? buildComment(options) : null;
		if (options.isSet("od-output")) {
			try {
				ODMeasuresGenerator mgNM = new ODMeasuresGenerator();
				AbstractResultsWriter<ODSingleResult> writer = buildNMOutput(options.getString("od-output"), precision, dropExistingTables);
				writer.createInsertStatement(epsg);
				Aggregator<ODSingleResult> agg = buildAggregator(mgNM, options.getBool("shortest"), 
						aggAllFrom, aggAllTo, fromLayer, fromAggLayer, toLayer, toAggLayer, writer, comment);
				aggregators.add(agg);
			} catch(IOException e) {
				throw new IOException("Exception '" + e.getMessage() + "' occurred while building the od-output.");
			}
		}
		if (options.isSet("ext-od-output")) {
			try {
				ODExtendedMeasuresGenerator mg = new ODExtendedMeasuresGenerator();
				AbstractResultsWriter<ODSingleExtendedResult> writer = buildExtNMOutput(options.getString("ext-od-output"), precision, dropExistingTables);
				writer.createInsertStatement(epsg);
				Aggregator<ODSingleExtendedResult> agg = buildAggregator(mg, options.getBool("shortest"), 
						aggAllFrom, aggAllTo, fromLayer, fromAggLayer, toLayer, toAggLayer, writer, comment);
				aggregators.add(agg);
			} catch(IOException e) {
				throw new IOException("Exception '" + e.getMessage() + "' occurred while building the ext-od-output.");
			}
		}
		if (options.isSet("stat-od-output")) {
			try {
				ODStatsMeasuresGenerator mg = new ODStatsMeasuresGenerator();
				AbstractResultsWriter<ODSingleStatsResult> writer = buildStatNMOutput(options.getString("stat-od-output"), precision, dropExistingTables);
				writer.createInsertStatement(epsg);
				Aggregator<ODSingleStatsResult> agg = buildAggregator(mg, options.getBool("shortest"), 
						aggAllFrom, aggAllTo, fromLayer, fromAggLayer, toLayer, toAggLayer, writer, comment);
				aggregators.add(agg);
			} catch(IOException e) {
				throw new IOException("Exception '" + e.getMessage() + "' occurred while building the stat-od-output.");
			}
		}
		if (options.isSet("interchanges-output")) {
			try {
				InterchangeMeasuresGenerator mg = new InterchangeMeasuresGenerator();
				AbstractResultsWriter<InterchangeSingleResult> writer = buildInterchangeOutput(options.getString("interchanges-output"), precision, dropExistingTables);
				writer.createInsertStatement(epsg);
				Aggregator<InterchangeSingleResult> agg = buildAggregator(mg, options.getBool("shortest"), 
						aggAllFrom, aggAllTo, fromLayer, fromAggLayer, toLayer, toAggLayer, writer, comment);
				aggregators.add(agg);
			} catch(IOException e) {
				throw new IOException("Exception '" + e.getMessage() + "' occurred while building the interchanges-output.");
			}
		}
		if (options.isSet("edges-output")) {
			try {
				EUMeasuresGenerator mg = new EUMeasuresGenerator();
				AbstractResultsWriter<EUSingleResult> writer = buildEUOutput(options.getString("edges-output"), precision, dropExistingTables);
				writer.createInsertStatement(epsg);
				Aggregator<EUSingleResult> agg = buildAggregator(mg, options.getBool("shortest"), 
						aggAllFrom, aggAllTo, fromLayer, fromAggLayer, toLayer, toAggLayer, writer, comment);
				aggregators.add(agg);
			} catch(IOException e) {
				throw new IOException("Exception '" + e.getMessage() + "' occurred while building the edges-output.");
			}
		}
		if (options.isSet("pt-output")) {
			try {
				PTODMeasuresGenerator mg = new PTODMeasuresGenerator();
				AbstractResultsWriter<PTODSingleResult> writer = buildPTODOutput(options.getString("pt-output"), precision, dropExistingTables);
				writer.createInsertStatement(epsg);
				Aggregator<PTODSingleResult> agg = buildAggregator(mg, options.getBool("shortest"), 
						aggAllFrom, aggAllTo, fromLayer, fromAggLayer, toLayer, toAggLayer, writer, comment);
				aggregators.add(agg);
			} catch(IOException e) {
				throw new IOException("Exception '" + e.getMessage() + "' occurred while building the pt-output.");
			}
		}
		return aggregators;
	}
	
	
	/** @brief Builds a "direct" output
	 * @param options The options that include the output definition
	 * @param epsg Used projection
	 * @param nearestToEdges Information about the destination mapping
	 * @return The direct output device
	 * @throws IOException When something fails
	 */
	public static DirectWriter buildDirectOutput(OptionsCont options, int epsg, HashMap<DBEdge, Vector<MapResult>> nearestToEdges) throws IOException {
		if (!options.isSet("direct-output")) {
			return null;
		}
		try {
			int precision = options.getInteger("precision");
			String d = options.getString("direct-output");
			Utils.Format format = Utils.getFormat(d);
			String[] inputParts = Utils.getParts(format, d, "direct-output");
			DirectWriter dw = new DirectWriter(format, inputParts, precision, options.getBool("dropprevious"), epsg);
			dw.createInsertStatement(epsg);
			if(options.getBool("comment")) {
				dw.addComment(buildComment(options));
			}
			return dw;
		} catch(IOException e) {
			throw new IOException("Exception '" + e.getMessage() + "' occurred while building the direct-output.");
		}
	}

	
	/**
	 * @brief Writes the connections from objects to the road network
	 * 
	 * @param outputName The name of the output
	 * @param options The options to retrieve parameter from
	 * @param nearestEdges The map of objects to road positions
	 * @param epsg Used projection
	 * @throws IOException When something fails
	 */
	public static void writeEdgeAllocation(String outputName, OptionsCont options, HashMap<DBEdge, Vector<MapResult>> nearestEdges, int epsg) throws IOException {
		int precision = options.getInteger("precision");
		boolean dropPrevious = options.getBool("dropprevious");
		String d = options.getString(outputName);
		Utils.Format format = Utils.getFormat(d);
		String[] inputParts = Utils.getParts(format, d, outputName);
		try {
			EdgeMappingWriter emw = new EdgeMappingWriter(format, inputParts, precision, dropPrevious, epsg);
			emw.createInsertStatement(epsg);
			if(options.getBool("comment")) {
				emw.addComment(buildComment(options));
			}
			emw.writeResults(nearestEdges);
			emw.close();
		} catch(IOException e) {
			throw new IOException("Exception '" + e.getMessage() + "' occurred while building the " + outputName + ".");
		}
	}


	/**
	 * @brief Writes information about subnets
	 *
	 * @param outputName The name of the output
	 * @param options The options to retrieve parameter from
	 * @param clusters The edge clusters to report
	 * @throws IOException When something fails
	 */
	public static void writeSubnets(String outputName, OptionsCont options, HashMap<Integer, Set<DBEdge>> clusters) throws IOException {
		boolean dropPrevious = options.getBool("dropprevious");
		String d = options.getString(outputName);
		Utils.Format format = Utils.getFormat(d);
		String[] inputParts = Utils.getParts(format, d, outputName);
		NetClusterWriter emw = new NetClusterWriter(format, inputParts, dropPrevious);
		emw.createInsertStatement(0);
		if(options.getBool("comment")) {
			emw.addComment(buildComment(options));
		}
		emw.writeClusters(clusters);
		emw.close();
	}

	
	public static NetErrorsWriter buildNetErrorsWriter(String d, boolean dropPrevious) throws IOException {
		Utils.Format format = Utils.getFormat(d);
		String[] inputParts = Utils.getParts(format, d, "pt-output");
		return new NetErrorsWriter(format, inputParts, dropPrevious);
	}
	
	

	/**
	 * @brief Builds a comment string that shows the set options
	 * @param options The options to encode
	 * @return A comment string with set options
	 */
	private static String buildComment(OptionsCont options) {
		StringBuffer sb = new StringBuffer();
		sb.append("Generated using UrMoAC with the following options:\n");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    String utf8 = StandardCharsets.UTF_8.name();
		try {
			PrintStream ps = new PrintStream(baos, true, utf8);
			
	    	Vector<String> optionNames = options.getSortedOptionNames();
	        for(Iterator<String> i=optionNames.iterator(); i.hasNext(); ) { 
	            String name = i.next();
	            if(!options.isSet(name)||options.isDefault(name)) {
	                continue;
	            }
	            Vector<String> synonyms = options.getSynonyms(name);
	            name = synonyms.elementAt(0);
	            ps.print(name);
	            String value = options.getValueAsString(name);
	            if(value.contains("jdbc:postgresql:")) {
	            	if(value.contains(";")&&!value.contains(",")) {
	            		value = value.substring(0, value.lastIndexOf(';')+1) + "xxx";
	            	} else {
	            		value = value.substring(0, value.lastIndexOf(',')+1) + "xxx";
	            	}
	            }
	            ps.print(": " + value );
	            ps.println();
	        }
		    sb.append(baos.toString(utf8));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	
	/**
	 * @brief Brief builds the results processing aggregator
	 * @param measuresGenerator The measures generator to use
	 * @param shortest Whether only the shortest connection shall be computed
	 * @param aggAllFrom Whether all origins shall be aggregated
	 * @param aggAllTo Whether all destinations shall be aggregated
	 * @param fromLayer The origins layer
	 * @param fromAggLayer The origins aggregation layer
	 * @param toLayer The destinations layer
	 * @param toAggLayer The destinations aggregation layer
	 * @param writer The writer to use
	 * @param comment The comment to add
	 * @return The built aggregator
	 * @throws IOException When something fails
	 */
	@SuppressWarnings("rawtypes")
	private static <T extends AbstractSingleResult> Aggregator<T> buildAggregator(MeasurementGenerator measuresGenerator,
			boolean shortest, boolean aggAllFrom, boolean aggAllTo, 
			Layer fromLayer, Layer fromAggLayer, Layer toLayer, Layer toAggLayer,
			AbstractResultsWriter<T> writer, String comment) throws IOException {
		@SuppressWarnings("unchecked")
		Aggregator<T> agg = new Aggregator<T>(measuresGenerator, fromLayer, shortest);
		if (fromAggLayer != null) {
			agg.setOriginAggregation(fromLayer, fromAggLayer);
		} else if (aggAllFrom) {
			agg.sumOrigins();
		}
		if (toAggLayer != null) {
			agg.setDestinationAggregation(toLayer, toAggLayer);
		} else if (aggAllTo) {
			agg.sumDestinations();
		}
		agg.buildMeasurementsMap(fromLayer, toLayer);
		agg.addOutput(writer);
		if(comment!=null) {
			writer.addComment(comment);
		}
		return agg;
	}	

	
	/**
	 * @brief Builds an ODWriter (an ODSingleResult-output)
	 * @param d The output storage definition
	 * @param precision The precision to use when writing to a file
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<ODSingleResult> buildNMOutput(String d, int precision, boolean dropPrevious) throws IOException {
		Utils.Format format = Utils.getFormat(d);
		String[] inputParts = Utils.getParts(format, d, "od-output");
		return new ODWriter(format, inputParts, precision, dropPrevious);
	}
	

	/**
	 * @brief Builds an ODExtendedWriter (an ODSingleExtendedResult-output)
	 * @param d The output storage definition
	 * @param precision The precision to use when writing to a file
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<ODSingleExtendedResult> buildExtNMOutput(String d, int precision, boolean dropPrevious) throws IOException {
		Utils.Format format = Utils.getFormat(d);
		String[] inputParts = Utils.getParts(format, d, "ext-od-output");
		return new ODExtendedWriter(format, inputParts, precision, dropPrevious);
	}
	

	/**
	 * @brief Builds an ODStatsWriter (an ODSingleStatsResult-output)
	 * @param d The output storage definition
	 * @param precision The precision to use when writing to a file
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<ODSingleStatsResult> buildStatNMOutput(String d, int precision, boolean dropPrevious) throws IOException {
		Utils.Format format = Utils.getFormat(d);
		String[] inputParts = Utils.getParts(format, d, "stat-od-output");
		return new ODStatsWriter(format, inputParts, precision, dropPrevious);
	}
	

	/**
	 * @brief Builds an InterchangeWriter (an InterchangeSingleResult-output)
	 * @param d The output storage definition
	 * @param precision The precision to use when writing to a file
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<InterchangeSingleResult> buildInterchangeOutput(String d, int precision, boolean dropPrevious) throws IOException {
		Utils.Format format = Utils.getFormat(d);
		String[] inputParts = Utils.getParts(format, d, "interchanges-output");
		return new InterchangeWriter(format, inputParts, precision, dropPrevious);
	}
	

	/**
	 * @brief Builds an EUWriter (an EUSingleResult-output)
	 * @param d The output storage definition
	 * @param precision The precision to use when writing to a file
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<EUSingleResult> buildEUOutput(String d, int precision, boolean dropPrevious) throws IOException {
		Utils.Format format = Utils.getFormat(d);
		String[] inputParts = Utils.getParts(format, d, "edges-output");
		return new EUWriter(format, inputParts, precision, dropPrevious);
	}
	
	
	/**
	 * @brief Builds a PTODWriter (a PTODSingleResult-output)
	 * @param d The output storage definition
	 * @param precision The precision to use when writing to a file
	 * @param dropPrevious Whether a prior database shall be dropped
	 * @return The built output
	 * @throws IOException When something fails
	 */
	private static AbstractResultsWriter<PTODSingleResult> buildPTODOutput(String d, int precision, boolean dropPrevious) throws IOException {
		Utils.Format format = Utils.getFormat(d);
		String[] inputParts = Utils.getParts(format, d, "pt-output");
		return new PTODWriter(format, inputParts, precision, dropPrevious);
	}
	
}

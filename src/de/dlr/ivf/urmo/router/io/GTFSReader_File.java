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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.gtfs.GTFSData;
import de.dlr.ivf.urmo.router.gtfs.GTFSRoute;
import de.dlr.ivf.urmo.router.gtfs.GTFSStop;
import de.dlr.ivf.urmo.router.gtfs.GTFSStopTime;
import de.dlr.ivf.urmo.router.gtfs.GTFSTrip;
import de.dlr.ivf.urmo.router.shapes.DBNet;

/**
 * @class GTFSReader_File
 * @brief Reads a GTFS plan from files
 * @author Daniel Krajzewicz
 */
public class GTFSReader_File extends AbstractGTFSReader {
	/// @brief The prefix of the tables
	private String filePrefix;
	/// @brief The bounding geometry
	private Geometry _bounds;

	
	/** @class GTFSFileParser
	 * @brief Helper class for reading a GTFS csv-file
	 */
	public class GTFSFileParser {
		/// @brief The name of the file for reporting
		private String _file; 
		/// @brief The CSVReader to use
		private CSVReader _csvReader;
		/// @brief Mapping from column names to their indices
		private HashMap<String, Integer> header2index = new HashMap<>();
		/// @brief The last read line
		private String _currentLine[] = null;
		
		
		/** @brief Constructor
		 * @param file The file to read
		 * @throws FileNotFoundException If something fails
		 */
		public GTFSFileParser(String file) throws FileNotFoundException {
			_file = file + ".txt";
			BufferedReader br = new BufferedReader(new FileReader(_file));
			CSVParser parser = new CSVParserBuilder().withSeparator(',').withIgnoreQuotations(false).build();
			_csvReader = new CSVReaderBuilder(br).withSkipLines(0).withCSVParser(parser).build();
		}
		
		
		/** @brief Reads the next entry
		 * 
		 * Parses the line as column names if it's the first line
		 * 
		 * @return Whether a further line have been read
		 * @throws IOException If something fails
		 */
		public boolean readNext() throws IOException {
			try {
				_currentLine = _csvReader.readNext();
				if(_currentLine==null) {
					return false;
				}
				// parse header
				if(header2index.size()==0) {
					for(int i=0; i<_currentLine.length; ++i) {
						header2index.put(_currentLine[i], i);
					}
					_currentLine = _csvReader.readNext();
					if(_currentLine==null) {
						return false;
					}
				}
				// parse lines
				return true;
			} catch (CsvValidationException e) {
				throw new IOException(e);
			}
		}
		
		
		/** @brief Returns the value of the named field
		 * @param col The name of the column to return the value of
		 * @return The value of the named column
		 * @throws IOException If something fails
		 */
		public String getField(String col) throws IOException {
			if(!header2index.containsKey(col)) {
				throw new IOException ("The field '" + col + "' is missing in '" + _file + "'.");
			}
			int idx = header2index.get(col);
			if(_currentLine.length<idx) {
				throw new IOException ("Broken (too short) line in '" + _file + "'.");
			}
			return _currentLine[idx];
		}
		
		
		/** @brief Closes the reader
		 * @throws IOException If something fails
		 */
		public void close() throws IOException {
			_csvReader.close();
		}
		
	}
	
	
	
	/** @brief Constructor
	 * @param net The network to use
	 * @param epsg The used projection
	 * @param date The date to read the GTFS information for
     * @param allowedCarrier List of allowed carriers (todo: recheck)
     */
	public GTFSReader_File(DBNet net, int epsg, String date, Vector<Integer> allowedCarrier) {
		super(net, epsg, date, allowedCarrier);
	}

	
	/// @brief Implemented abstract methods
	/// @{
	
	/** @brief Initialises the reader
	 * @param format The format the reader uses
	 * @param inputParts The input access definition
	 * @param bounds The bounding box to use
	 * @throws IOException If something fails
	 */
	protected void init(Utils.Format format, String[] inputParts, Geometry bounds) throws IOException {
		_bounds = bounds;
		filePrefix = inputParts[0];
	}
	
	
	/** @brief Reads stops
	 * @param stops The mapping from an internal ID to the stop to fill
	 * @param id2stop The mapping from the GTFS stop ID to the stop to fill
	 * @param stopsV The list of stops to fill
	 * @throws IOException If something fails
	 */
	protected void readStops(HashMap<Long, GTFSStop> stops, HashMap<String, GTFSStop> id2stop, Vector<EdgeMappable> stopsV) throws IOException {
		// build transformer
		MathTransform transform = null;
		try {
			CoordinateReferenceSystem dataCRS = CRS.decode("EPSG:4326");
	        CoordinateReferenceSystem worldCRS = CRS.decode("EPSG:" + _epsg);
	        boolean lenient = true; // allow for some error due to different datums
	        transform = CRS.findMathTransform(dataCRS, worldCRS, lenient);		
		} catch (FactoryException e) {
			throw new IOException(e);
		}
		GTFSFileParser parser = new GTFSFileParser(filePrefix + "stops");
		while(parser.readNext()) {
			String id = parser.getField("stop_id");
			if(id2stop.containsKey(id)) {
				System.out.println("Warning: stop " + id + " already exists; skipping.");
				continue;
			}
			String lat = parser.getField("stop_lat");
			String lon = parser.getField("stop_lon");
			// todo: check boundary
			try {
				Coordinate c = new Coordinate(Double.parseDouble(lat), Double.parseDouble(lon));
				Point p = _net.getGeometryFactory().createPoint(c);
				p = (Point) JTS.transform(p, transform);
				if(_bounds!=null&&!_bounds.contains(p)) {
					continue;
				}
				c.x = p.getX();
				c.y = p.getY();
				// !!! projection
				GTFSStop stop = new GTFSStop(_net.getNextID(), id, c, _net.getGeometryFactory().createPoint(c)); // !!! new id - the nodes should have a new id as well
				if(!_net.addNode(stop, stop.mid)) {
					throw new IOException("A node with id '" + stop.getID() + "' already exists.");
				}
				stops.put(stop.getID(), stop);
				id2stop.put(stop.mid, stop);
				stopsV.add(stop);
			} catch (NumberFormatException | MismatchedDimensionException | TransformException e) {
				throw new IOException(e);
			}
		}
		parser.close();
	}
	

	/** @brief Reads routes
	 * @param routes The mapping from ID to route to fill
	 * @throws IOException If something fails
	 */
	protected void readRoutes(HashMap<String, GTFSRoute> routes) throws IOException {
		GTFSFileParser parser = new GTFSFileParser(filePrefix + "routes");
		while(parser.readNext()) {
			try {
				GTFSRoute route = new GTFSRoute(parser.getField("route_id"), parser.getField("route_short_name"), Integer.parseInt(parser.getField("route_type")));
				if(_allowedCarrier.size()==0 || _allowedCarrier.contains(route.type)) {
					routes.put(parser.getField("route_id"), route);
				}
			} catch (NumberFormatException e) {
				throw new IOException(e);
			}
		}
		parser.close();
	}
	
	
	/** @brief Reads services
	 * @param dateI The integer representation of the date (todo: recheck)
	 * @param dayOfWeek The day of the week
	 * @param services The set of services to fill
	 * @throws IOException If something fails
	 */
	protected void readServices(int dateI, int dayOfWeek, Set<String> services) throws IOException {
		if(dateI==0) {
			return;
		}
		// calendar
		GTFSFileParser parser = new GTFSFileParser(filePrefix + "calendar");
		while(parser.readNext()) {
			int dateBI = parseDate(parser.getField("start_date"));
			int dateEI = parseDate(parser.getField("end_date"));
			if(dateBI>dateI||dateEI<dateI) {
				continue;
			}
			// 
			try {
				if(Integer.parseInt(parser.getField(weekdays[dayOfWeek]))!=0) {
					services.add(parser.getField("service_id"));
				}
			} catch (NumberFormatException e) {
				throw new IOException(e);
			}
		}
		parser.close();
		// 
		File f = new File(filePrefix + "calendar_dates.txt");
		if(!f.exists() || f.isDirectory()) {
			return;
		}
		parser = new GTFSFileParser(filePrefix + "calendar_dates");
		while(parser.readNext()) {
			int dateCI = parseDate(parser.getField("date"));
			if(dateCI!=dateI) {
				continue;
			}
			try {
				int et = Integer.parseInt(parser.getField("exception_type"));
				String service_id = parser.getField("service_id"); 
				if(et==1) {
					services.add(service_id);
				} else if(et==2) {
					services.remove(service_id);
				} else {
					throw new IOException("Unkonwn exception type in " + filePrefix + "_calendar_dates.");
				}
			} catch (NumberFormatException e) {
				throw new IOException(e);
			}
		}
		parser.close();
	}
	
	
	/** @brief Reads trips
	 * @param services The read services
	 * @param routes The read routes
	 * @param dateI The integer representation of the date (todo: recheck)
	 * @param trips The mapping of IDs to trips to fill
	 * @throws IOException If something fails
	 */
	protected void readTrips(Set<String> services, HashMap<String, GTFSRoute> routes, int dateI, HashMap<String, GTFSTrip> trips) throws IOException {
		GTFSFileParser parser = new GTFSFileParser(filePrefix + "trips");
		while(parser.readNext()) {
			String service_id = parser.getField("service_id");
			if(dateI!=0&&!services.contains(service_id)) {
				continue;
			}
			String route_id = parser.getField("route_id");
			if(!routes.containsKey(route_id)) {
				continue;
			}
			GTFSTrip trip = new GTFSTrip(parser.getField("trip_id"), routes.get(route_id));
			trips.put(parser.getField("trip_id"), trip);
		}
		parser.close();
	}
	
	
	/** @brief Reads stop times
	 * @param ret The GTFS container to get additional information from
	 * @param trips The read trips
	 * @param id2stop The read stops
	 * @param verbose Whether additional information shall be printed
	 * @throws IOException If something fails
	 */
	protected void readStopTimes(GTFSData ret, HashMap<String, GTFSTrip> trips, HashMap<String, GTFSStop> id2stop, boolean verbose) throws IOException {
		GTFSFileParser parser = new GTFSFileParser(filePrefix + "stop_times");
		Vector<GTFSStopTime> stopTimes = new Vector<>();
		int abs = 0;
		int err = 0;
		String lastTripID = null;
		while(parser.readNext()) { // !!! sort?
			String tripID = parser.getField("trip_id");
			if(!trips.containsKey(tripID)) {
				continue;
			}
			String stop_id = parser.getField("stop_id");
			if(lastTripID!=null&&!tripID.equals(lastTripID)) {
				err += ret.recheckTimesAndInsert(lastTripID, stopTimes, id2stop);
				abs += stopTimes.size() - 1;
				stopTimes.clear();
			}
			lastTripID = tripID;
			String arrivalTimeS = parser.getField("arrival_time");
			String departureTimeS = parser.getField("departure_time");
			int arrivalTime, departureTime;
			if(arrivalTimeS.indexOf(':')>=0) {
				arrivalTime = parseTime(arrivalTimeS);
				departureTime = parseTime(departureTimeS);
			} else {
				arrivalTime = Integer.parseInt(arrivalTimeS);
				departureTime = Integer.parseInt(departureTimeS);
			}
			GTFSStopTime stopTime = new GTFSStopTime(tripID, arrivalTime, departureTime, stop_id);
			stopTimes.add(stopTime);
		}
		err += ret.recheckTimesAndInsert(lastTripID, stopTimes, id2stop);
		abs += stopTimes.size() - 1;
		stopTimes.clear();
		ret.sortConnections();
		if(verbose) System.out.println("  " + abs + " connections found of which " + err + " were erroneous");
		parser.close();
	}
	

	/** @brief Reads transfer times
	 * @param trips The read trips
	 * @param id2stop The read stops
	 * @param verbose Whether additional information shall be printed
	 * @throws IOException If something fails
	 */
	protected void readTransfers(HashMap<String, GTFSTrip> trips, HashMap<String, GTFSStop> id2stop, boolean verbose) throws IOException {
		File f = new File(filePrefix + "transfers.txt");
		if(!f.exists() || f.isDirectory()) {
			return;
		}
		if(verbose) System.out.println(" ... reading transfer times ...");
		GTFSFileParser parser = new GTFSFileParser(filePrefix + "transfers");
		while(parser.readNext()) {
			String fromStop = parser.getField("from_stop_id");
			GTFSStop stop = id2stop.get(fromStop);
			if(stop==null) {
				// may be out of the pt-boundary
				continue;
			}
			String toStop = parser.getField("to_stop_id");
			if(!fromStop.equals(toStop)) {
				continue;
			}
			try {
				if(Integer.parseInt(parser.getField("transfer_type"))!=2) {
					continue;
				}
				GTFSTrip t1 = trips.get(parser.getField("from_trip_id"));
				GTFSTrip t2 = trips.get(parser.getField("to_trip_id"));
				if(t1!=null&&t2!=null) {
					stop.setInterchangeTime(t1, t2, (double) Integer.parseInt(parser.getField("min_transfer_time")));
				}
			} catch (NumberFormatException e) {
				throw new IOException(e);
			}
		}		
		parser.close();
	}
	
	
	/** @brief Closes the connection / file
	 * @throws IOException If something fails
	 */
	protected void close() throws IOException {
	}
	
	/// @}
	
}

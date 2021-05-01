package de.dlr.ivf.urmo;

import com.vividsolutions.jts.io.ParseException;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.NearestEdgeFinder;
import de.dlr.ivf.urmo.router.algorithms.routing.AbstractRouteWeightFunction;
import de.dlr.ivf.urmo.router.algorithms.routing.BoundDijkstra;
import de.dlr.ivf.urmo.router.algorithms.routing.DijkstraResult;
import de.dlr.ivf.urmo.router.algorithms.routing.RouteWeightFunction_TT_Modes;
import de.dlr.ivf.urmo.router.io.InputReader;
import de.dlr.ivf.urmo.router.io.PipedWriter;
import de.dlr.ivf.urmo.router.output.DijkstraResultsProcessor;
import de.dlr.ivf.urmo.router.output.odext.ODExtendedMeasuresGenerator;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.shapes.IDGiver;
import de.dlr.ivf.urmo.router.shapes.Layer;
import org.apache.commons.cli.CommandLine;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Vector;
import java.util.function.Supplier;

public class UrmoWorker implements Runnable{

    private final PipedWriter writer;
    private final Supplier<Integer> taz_supplier;
    private final String name;
    private final NearestEdgeFinder edge_finder;
    private final CommandLine options;
    private final int epsg;
    private final IntraTazComputer idGiver;
    private final int boundNumber;
    private final double boundTT;
    private final long modes;
    private final double boundDist;
    private final double boundVar;
    private final boolean shortestOnly;
    private final boolean needsPT = false;

    private AbstractRouteWeightFunction measure = new RouteWeightFunction_TT_Modes();
    private final int time;
    private final long initMode;

    public UrmoWorker(String name, PipedWriter writer, Supplier<Integer> taz_supplier, NearestEdgeFinder edge_finder,
                      CommandLine options, int epsg, IntraTazComputer idGiver,
                      int _time, long _initMode, long _modes, int _boundNumber, double _boundTT,
                      double _boundDist, double _boundVar, boolean _shortestOnly){
        
        this.writer = writer;
        this.taz_supplier = taz_supplier;
        this.name = name;
        this.edge_finder = edge_finder;
        this.options = options;
        this.epsg = epsg;
        this.idGiver = idGiver;

        this.time = _time;
        this.initMode = _initMode;
        this.boundNumber = _boundNumber;
        this.boundTT = _boundTT;
        this.modes = _modes;
        this.boundDist = _boundDist;
        this.boundVar = _boundVar;
        this.shortestOnly = _shortestOnly;
    }




    @Override
    public void run() {

        Integer taz;

        int calculated_taz_count = 0;

        Layer fromLayer = null;

        ODExtendedMeasuresGenerator measurements_generator = new ODExtendedMeasuresGenerator();

        System.out.println(name+" is initializing R-Tree...");
        edge_finder.init();

        while((taz = taz_supplier.get()) != null){

            LocalDateTime now = LocalDateTime.now();

            try {

                System.out.println(this.name+" loading next layer for TAZ: "+taz);
                fromLayer = InputReader.loadLayerWithFilter(options, "from", null, this.idGiver, epsg, "wq_gid = "+taz.toString() +" AND geocode_key = 'GEOCODES_2017'");

                System.out.println("Computing access from the origins to the network for taz: "+taz);

                edge_finder.setSource(fromLayer.getObjects());

                Map<DBEdge, Vector<MapResult>> nearestFromEdges = this.edge_finder.getNearestEdges(true, fromLayer.getObjects());
                Map<DBEdge, Vector<MapResult>> nearestToEdges = nearestFromEdges; //this.edge_finder.getNearestEdges(true, fromLayer.getObjects());

                int time = ((Long) options.getParsedOptionValue("time")).intValue();

                DijkstraResultsProcessor resultsProcessor = new DijkstraResultsProcessor(time, nearestFromEdges, nearestToEdges, measurements_generator);

                System.out.println(name+" computing taz: "+taz+" between "+nearestFromEdges.keySet().size()+ " starting and end edges... | "+calculated_taz_count+" taz computed, total remaining: "+idGiver.getRemainingCount());

                for(DBEdge e : nearestFromEdges.keySet()){
                    DijkstraResult ret = BoundDijkstra.run(measure, time, e, initMode, modes, nearestToEdges.keySet(), boundNumber, boundTT, boundDist, boundVar, shortestOnly);
                    Vector<MapResult> fromObjects = nearestFromEdges.get(e);
                    fromObjects.stream().map(from_map_result -> resultsProcessor.processAndGetEXTODResults(from_map_result,ret)).forEach(writer::writeResults);
                }
                calculated_taz_count++;
                System.out.println(name+" needed "+Duration.between(now,LocalDateTime.now()).toMinutes()+
                        " minutes to compute taz: "+taz+" with "+nearestFromEdges.size()+" start and destination edges...");

            } catch (SQLException throwables) {
                throwables.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (org.apache.commons.cli.ParseException e) {
                e.printStackTrace();
            }

            calculated_taz_count++;

        }

        System.out.println(name+ " is calling it a day...");

    }
}

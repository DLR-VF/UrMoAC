package de.dlr.ivf.urmo.router.spring.routing;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.MapResult;
import de.dlr.ivf.urmo.router.algorithms.edgemapper.NearestEdgeFinder;
import de.dlr.ivf.urmo.router.shapes.DBEdge;
import de.dlr.ivf.urmo.router.spring.configuration.FerryConfiguration;
import de.dlr.ivf.urmo.router.spring.configuration.RequestConfiguration;
import de.dlr.ivf.urmo.router.spring.mode.AlternativeMode;
import de.dlr.ivf.urmo.router.spring.model.SimpleEdgeMappable;
import de.dlr.ivf.urmo.router.spring.model.TravelStage;
import de.dlr.ivf.urmo.router.spring.net.Edge;
import de.dlr.ivf.urmo.router.spring.net.NetService;
import de.dlr.ivf.urmo.router.spring.net.Node;
import de.dlr.ivf.urmo.router.spring.util.WGSDistanceCalculator;
import jakarta.annotation.PostConstruct;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.jgrapht.GraphPath;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RoutingService {

    @Autowired
    private NetService netService;

    @Autowired
    private RequestConfiguration requestConfiguration;

    @Autowired
    private FerryConfiguration ferryConfiguration;

    private MapResult firstFerryTerminalMapping;
    private MapResult secondFerryTerminalMapping;

    private Coordinate firstFerryCoordinate;
    private Coordinate secondFerryCoordinate;

    private double distanceFirstSecondTerminal;

    private NearestEdgeFinder edgeFinder;

    private MathTransform transformation;

    private GeometryFactory inputGeomFactory;

    private Router router;

    private final System.Logger logger = System.getLogger(RoutingService.class.getName());

    @PostConstruct
    public void init() throws FactoryException, TransformException {

        logger.log(Level.INFO, "Initializing NearestEdgeFinder...");
        this.edgeFinder = new NearestEdgeFinder(netService.getDbNetwork(), 30);

        logger.log(Level.INFO,"Initializing Router...");
        this.router = new Router(netService.getModeDependentGraphs());

        logger.log(Level.INFO, "Initializing coordinate transformation function...");
        this.inputGeomFactory = new GeometryFactory(new PrecisionModel(), requestConfiguration.epsg());

        CoordinateReferenceSystem sourceCrs = CRS.decode("EPSG:"+requestConfiguration.epsg(), true);
        CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:"+netService.getNetEpsg(), true);
        this.transformation = CRS.findMathTransform(sourceCrs, targetCrs);

        //initialize the ferry station mappings
        long modeWalkBike = 0 | AlternativeMode.FOOT.getUrmoModeId() | AlternativeMode.BIKESHARING.getUrmoModeId();
        this.firstFerryTerminalMapping = findNearestEdge(ferryConfiguration.firstTerminal(), modeWalkBike);
        this.secondFerryTerminalMapping = findNearestEdge(ferryConfiguration.secondTerminal(), modeWalkBike);
        this.firstFerryCoordinate = ferryConfiguration.firstTerminal();
        this.secondFerryCoordinate = ferryConfiguration.secondTerminal();

        WGSDistanceCalculator distanceCalculator = new WGSDistanceCalculator(ferryConfiguration.firstTerminal());
        this.distanceFirstSecondTerminal = distanceCalculator.getDistance(ferryConfiguration.secondTerminal());
    }

    public Optional<Map<TravelStage, RoutingResult>> generateRoute(Coordinate start, Coordinate end, AlternativeMode mode) {

        long alternativeModeUrmoId = mode.getUrmoModeId();

        //foot should be the default access mode but nonetheless a bike might also be used
        long modes = AlternativeMode.FOOT.getUrmoModeId() | (alternativeModeUrmoId > 0 ? alternativeModeUrmoId : AlternativeMode.BIKESHARING.getUrmoModeId());

        MapResult startMappingResult = findNearestEdge(start, modes);
        MapResult endMappingResult = findNearestEdge(end, modes);

        if(startMappingResult == null || endMappingResult == null){
            return Optional.empty();
        }

        Map<TravelStage,RoutingResult> result = switch (mode){
            case FERRY -> computeFerryRoute(start, end, startMappingResult, endMappingResult);
            default -> computeSimpleModeRoute(startMappingResult, endMappingResult, mode);

        };

        for(RoutingResult routingResult : result.values()){
            if(routingResult == null){
                return Optional.empty();
            }
        }

        return Optional.of(result);
    }

    private MapResult findNearestEdge(Coordinate coordinate, long modes){

        //convert coordinate first to a projected coordinate system and find the nearest edges to start and end
        Geometry transformedCoordinate = null;
        try {
            transformedCoordinate = JTS.transform(inputGeomFactory.createPoint(coordinate), transformation);
        } catch (TransformException e) {
            throw new IllegalArgumentException("Unable to re-project point coordinate.",e);
        }
        EdgeMappable edgeMappable = new SimpleEdgeMappable(transformedCoordinate.getCentroid());

        return edgeFinder.getNearestEdge(edgeMappable, modes);
    }

    private GraphPath<Node,Edge> findRoute(MapResult start, MapResult end, AlternativeMode mode){

        Node endNodeStartEdge = netService.getNodeById(start.edge.to.id);
        Node endNodeEndEdge = netService.getNodeById(end.edge.to.id);
        Node startNodeStartEdge = netService.getNodeById(start.edge.from.id);
        Node startNodeEndEdge = netService.getNodeById(end.edge.from.id);

        GraphPath<Node, Edge> result = router.route(endNodeStartEdge, endNodeEndEdge, mode);

        //no route has been found so start from the start node of the starting edge
        if(result == null){
            result = router.route(startNodeStartEdge, endNodeEndEdge, mode);
        }
        //no route found so swap end nodes
        if(result == null){
            result = router.route(endNodeStartEdge, startNodeEndEdge, mode);
        }
        //now both starting and end node are swapped
        if(result == null){
            result = router.route(startNodeStartEdge, startNodeEndEdge, mode);
        }
        return result;
    }

    private MapResult closestFerryTerminalMapping(Coordinate coordinate){
        WGSDistanceCalculator distanceCalculator = new WGSDistanceCalculator(coordinate);

        if(distanceCalculator.getDistance(firstFerryCoordinate) <= distanceCalculator.getDistance(secondFerryCoordinate)){
            return this.firstFerryTerminalMapping;
        }else{
            return this.secondFerryTerminalMapping;
        }
    }

    private Map<TravelStage, RoutingResult> computeFerryRoute(Coordinate start, Coordinate end, MapResult startMappingResult, MapResult endMappingResult){


        MapResult startFerryTerminalMapping = closestFerryTerminalMapping(start);
        MapResult endFerryTerminalMapping = closestFerryTerminalMapping(end);

        Map<TravelStage, RoutingResult> result = new HashMap<>();

        AlternativeMode accessMode = AlternativeMode.FOOT;
        GraphPath<Node,Edge> accessPath = findRoute(startMappingResult, startFerryTerminalMapping, accessMode);
        GraphPath<Node,Edge> egressPath = findRoute(endFerryTerminalMapping, endMappingResult, accessMode);

        RoutingResult accessResult = routingResultFromPath(accessMode, accessPath, startMappingResult, startFerryTerminalMapping);
        RoutingResult egressResult = routingResultFromPath(accessMode, egressPath, endFerryTerminalMapping, endMappingResult);

        result.put(TravelStage.ACCESS, accessResult);
        result.put(TravelStage.EGRESS, egressResult);
        result.put(TravelStage.MAIN, new RoutingResult(AlternativeMode.FERRY,null,distanceFirstSecondTerminal, computeTravelTime(distanceFirstSecondTerminal, AlternativeMode.FERRY.getSpeed())));
        return result;
    }

    private Map<TravelStage, RoutingResult> computeSimpleModeRoute(MapResult startMappingResult,
                                                                   MapResult endMappingResult,
                                                                   AlternativeMode mode) {

        Map<TravelStage, RoutingResult> result = new HashMap<>();

        AlternativeMode accessMode = AlternativeMode.FOOT;
        double accessDistance = startMappingResult.dist;
        double egressDistance = endMappingResult.dist;

        result.put(TravelStage.ACCESS, new RoutingResult(accessMode,null, accessDistance, accessDistance * accessMode.getSpeed()));
        result.put(TravelStage.EGRESS, new RoutingResult(accessMode, null, egressDistance, egressDistance * accessMode.getSpeed()));

        GraphPath<Node,Edge> route = findRoute(startMappingResult, endMappingResult, mode);

        RoutingResult routingResult = routingResultFromPath(mode, route, startMappingResult, endMappingResult);

        //adjust travelDistance according to the direction of movement on access and egress edge
        result.put(TravelStage.MAIN, routingResult);
        return result;
    }

    /**
     *
     * @param mode
     * @param route
     * @param startMapResult
     * @param endMapResult
     * @return a RoutingResult or null if the path is either null or contains less than 2 vertices
     */
    private RoutingResult routingResultFromPath(AlternativeMode mode, GraphPath<Node, Edge> route, MapResult startMapResult, MapResult endMapResult) {

        if(route == null){
            return null;
        }

        List<Node> routeVertices = route.getVertexList();
        if(routeVertices.size() < 2){
            double dist = startMapResult.dist+ endMapResult.dist;
            return new RoutingResult(mode,route,dist,dist / mode.getSpeed());
        }

        //calculate travel time and distance
        double travelTime = 0;
        double distance = 0;

        for(Edge edge : route.getEdgeList()){

            distance += edge.length();
            travelTime += computeModeTravelTimeOnEdge(edge, mode);
        }

        //adjust travel distance as well as travel time based on mapped access and egress edge and the
        //direction of movement along those edges
        Node startNode = routeVertices.get(0);
        Node adjecentStartNode = routeVertices.get(1);
        double deltaStartDistance = computeAccessingEdgeDistance(startMapResult, adjecentStartNode, startNode);

        Node endNode = routeVertices.get(routeVertices.size()-1);
        Node adjecentEndNode = routeVertices.get(routeVertices.size()-2);
        double deltaEndDistance = computeAccessingEdgeDistance(endMapResult, adjecentEndNode, endNode);

        double finalDistance = distance + deltaStartDistance + deltaEndDistance;

        double deltaStartTravelTime = computeBoundedTravelTime(deltaStartDistance, mode, startMapResult.edge.vmax);
        double deltaEndTravelTime = computeBoundedTravelTime(deltaEndDistance, mode, endMapResult.edge.vmax);
        double finalTravelTime = travelTime + deltaStartTravelTime + deltaEndTravelTime;


        return new RoutingResult(mode,route,finalDistance,finalTravelTime);
    }

    private double computeTravelDistance(GraphPath<Node,Edge> path){
        if(path == null){
            return -1;
        }
        return path.getEdgeList().stream().mapToDouble(Edge::length).sum();
    }

    private double computeModeTravelTimeOnEdge(Edge edge, AlternativeMode mode){
        return computeBoundedTravelTime(edge.length(), mode, edge.maxSpeed());
    }

    private double computeTravelTime(double distance, double speed){
        return distance / speed;
    }

    private double computeBoundedTravelTime(double distance, AlternativeMode mode, double upperBound){
        return upperBound > mode.getSpeed()
                ? computeTravelTime(distance, mode.getSpeed())
                : computeTravelTime(distance, upperBound);
    }

    private RoutingResult adjustAccessEgress(MapResult startMapResult, MapResult endMapResult, RoutingResult routingResult){
        AlternativeMode mode = routingResult.mode();
        long startMappingEdgeStartNode = startMapResult.edge.from.id;
        long startMappingEdgeEndNode = startMapResult.edge.to.id;

        Edge startMappingEdge = netService.getEdge(startMappingEdgeStartNode, startMappingEdgeEndNode, mode);


        List<Node> routeVertexes = routingResult.path().getVertexList();

        if(routeVertexes.size() < 2){
            return routingResult;
        }

        Node startNode = routeVertexes.get(0);
        Node adjecentStartNode = routeVertexes.get(1);
        double deltaStartDistance = computeAccessingEdgeDistance(startMapResult, adjecentStartNode, startNode);

        Node endNode = routeVertexes.get(routeVertexes.size()-1);
        Node adjecentEndNode = routeVertexes.get(routeVertexes.size()-2);
        double deltaEndDistance = computeAccessingEdgeDistance(endMapResult, adjecentEndNode, endNode);

        double finalDistance = routingResult.distance() + deltaStartDistance + deltaEndDistance;

        double deltaStartTravelTime = computeBoundedTravelTime(deltaStartDistance, mode, startMapResult.edge.vmax);
        double deltaEndTravelTime = computeBoundedTravelTime(deltaEndDistance, mode, endMapResult.edge.vmax);
        double finalTravelTime = routingResult.travelTime() + deltaStartTravelTime + deltaEndTravelTime;


        return new RoutingResult(mode,routingResult.path(),finalDistance,finalTravelTime);
    }

    private double computeAccessingEdgeDistance(MapResult mapResult, Node adjecentNode, Node accessNode) {

        DBEdge mappingEdge = mapResult.edge;
        long mappingEdgeStartNode = mappingEdge.from.id;
        long mappingEdgeEndNode = mappingEdge.to.id;

        //the mapping edge has been traversed in the opposite direction so there exists an opposite edge
        if(mappingEdgeStartNode == adjecentNode.id() || mappingEdgeEndNode == adjecentNode.id()) {
            if(mappingEdgeStartNode == accessNode.id()){
                return -mapResult.pos;
            }else{
                return mapResult.pos - mappingEdge.length;
            }
        }else {//
            if (mappingEdgeStartNode == accessNode.id()) {
                return mapResult.pos;
            } else {
                return mappingEdge.length - mapResult.pos;
            }
        }
    }
}

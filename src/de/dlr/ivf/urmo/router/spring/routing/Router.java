package de.dlr.ivf.urmo.router.spring.routing;

import de.dlr.ivf.urmo.router.spring.mode.AlternativeMode;
import de.dlr.ivf.urmo.router.spring.net.Edge;
import de.dlr.ivf.urmo.router.spring.net.Node;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;

import java.util.Map;
import java.util.stream.Collectors;

public class Router {
    private final Map<AlternativeMode, DijkstraShortestPath<Node,Edge>> modeRouters;
    public Router(Map<AlternativeMode, Graph<Node, Edge>> modeGraphs){
        this.modeRouters = modeGraphs.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new DijkstraShortestPath<>(entry.getValue())
        ));
    }

    public GraphPath<Node, Edge> route(Node nodeFrom, Node nodeTo, AlternativeMode mode){
        var modeRouter = modeRouters.get(mode);
        return modeRouter.getPath(nodeFrom, nodeTo);
    }
}

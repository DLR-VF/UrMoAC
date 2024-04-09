package de.dlr.ivf.urmo.router.spring.routing;

import de.dlr.ivf.urmo.router.spring.mode.AlternativeMode;
import de.dlr.ivf.urmo.router.spring.net.Edge;
import de.dlr.ivf.urmo.router.spring.net.Node;
import org.jgrapht.GraphPath;

public record RoutingResult(AlternativeMode mode, GraphPath<Node, Edge> path, double distance, double travelTime) {}

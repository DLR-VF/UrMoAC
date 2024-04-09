package de.dlr.ivf.urmo.router.spring.model;

import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.spring.mode.AlternativeMode;
import de.dlr.ivf.urmo.router.spring.net.Edge;
import de.dlr.ivf.urmo.router.spring.net.Node;
import org.jgrapht.Graph;

import java.util.Map;

public record NetWrapper(DBNet urmoNet, Map<AlternativeMode, Graph<Node, Edge>> jGraphNet, Map<Long, Node> nodeIdMap) {
}

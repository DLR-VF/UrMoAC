package de.dlr.ivf.urmo.router.spring.net;

import de.dlr.ivf.urmo.router.io.NetLoader;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.spring.configuration.NetConfiguration;
import de.dlr.ivf.urmo.router.spring.mode.AlternativeMode;
import de.dlr.ivf.urmo.router.spring.model.NetWrapper;
import jakarta.annotation.PostConstruct;
import org.jgrapht.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NetService {
    @Autowired
    private NetConfiguration netConfiguration;

    private DBNet network;

    private Map<AlternativeMode, Graph<Node, Edge>> modeGraphs;
    private Map<Long, Node> nodeIdMap;

    public DBNet getDbNetwork() {
        return network;
    }


    @PostConstruct
    public void init() throws Exception {

        NetWrapper netWrapper = NetLoader.newJGraphTNet(netConfiguration.networkPath().toString(), netConfiguration.minGraphSize());
        this.network = netWrapper.urmoNet();
        this.nodeIdMap = netWrapper.nodeIdMap();
        this.modeGraphs = netWrapper.jGraphNet();
    }

    public Graph<Node,Edge> getModeDependentNet(AlternativeMode mode){
        return modeGraphs.get(mode);
    }

    public Map<AlternativeMode,Graph<Node,Edge>> getModeDependentGraphs(){
        return modeGraphs;
    }

    public Node getNodeById(long id){
        return nodeIdMap.get(id);
    }

    public Edge getEdge(long fromNodeId, long toNodeId, AlternativeMode mode){
        Graph<Node,Edge> network = modeGraphs.get(mode);

        return network.getEdge(nodeIdMap.get(fromNodeId), nodeIdMap.get(toNodeId));
    }

    public int getMinGraphSize(){
        return netConfiguration.minGraphSize();
    }

    public int getNetEpsg(){return netConfiguration.epsg();}
}

package de.dlr.ivf.urmo.router.spring.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("urmoac.net")
public class NetworkProperty {

    private Path network;
    private int epsg;

    public int getEpsg() {
        return epsg;
    }

    public void setEpsg(int epsg) {
        this.epsg = epsg;
    }

    public Path getNetwork() {
        return network;
    }

    public void setNetwork(Path network) {
        this.network = network;
    }
}

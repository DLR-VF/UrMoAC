package de.dlr.ivf.urmo.router.spring.configuration;

import org.locationtech.jts.geom.Coordinate;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mode.ferry")
public record FerryConfiguration(int maxAccessEgressDistanceToTerminals, Coordinate firstTerminal, Coordinate secondTerminal){}

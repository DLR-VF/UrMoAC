package de.dlr.ivf.urmo.router.spring.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "request")
public record RequestConfiguration(int epsg) {
}

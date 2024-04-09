package de.dlr.ivf.urmo.router.spring.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "net")
public record NetConfiguration(Path networkPath, int minGraphSize, int epsg) {}

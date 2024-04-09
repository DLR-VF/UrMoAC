package de.dlr.ivf.urmo.router.spring.model;

import org.locationtech.jts.geom.Coordinate;

public record AccessibleNode(Coordinate destination, double distance) {
}

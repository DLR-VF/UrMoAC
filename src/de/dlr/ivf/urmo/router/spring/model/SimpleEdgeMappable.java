package de.dlr.ivf.urmo.router.spring.model;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.EdgeMappable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

public record SimpleEdgeMappable(Point point) implements EdgeMappable {

    @Override
    public Point getPoint() {
        return point;
    }

    @Override
    public Geometry getGeometry() {
        return point.getCentroid();
    }
}

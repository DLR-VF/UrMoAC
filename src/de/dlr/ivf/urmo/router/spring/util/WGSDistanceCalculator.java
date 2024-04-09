package de.dlr.ivf.urmo.router.spring.util;

import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;

public class WGSDistanceCalculator {

    private final Coordinate startCoordinate;

    private final Object syncObject = new Object();
    private final GeodeticCalculator calc4326;

    public WGSDistanceCalculator(Coordinate startCoordinate){
        this.startCoordinate = startCoordinate;
        this.calc4326 = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
    }

    public double getDistance(Coordinate endCoordinate){
        synchronized (syncObject) {
            calc4326.setStartingGeographicPoint(startCoordinate.getX(), startCoordinate.getY());
            calc4326.setDestinationGeographicPoint(endCoordinate.getX(), endCoordinate.getY());

            return calc4326.getOrthodromicDistance();
        }
    }
}

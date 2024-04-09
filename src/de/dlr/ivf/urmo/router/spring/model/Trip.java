package de.dlr.ivf.urmo.router.spring.model;


import de.dlr.ivf.urmo.router.spring.mode.AlternativeMode;
import lombok.Getter;
import org.locationtech.jts.geom.Point;

@Getter
public class Trip {

    private final AlternativeMode mode;
    private final Point endCoord;
    private final Point startCoord;

    public Trip(Point startCoord, Point endCoord, AlternativeMode mode){
        this.startCoord = startCoord;
        this.endCoord = endCoord;
        this.mode = mode;
    }
}

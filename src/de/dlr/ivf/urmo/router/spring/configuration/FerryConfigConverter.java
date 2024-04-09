package de.dlr.ivf.urmo.router.spring.configuration;

import org.locationtech.jts.geom.Coordinate;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class FerryConfigConverter implements Converter<String, Coordinate> {
    @Override
    public Coordinate convert(String source) {

        String[] coordinates = source.split(",");

        if(coordinates.length != 2){
            throw new IllegalArgumentException("Invalid coordinates for: "+source+". Allowed format: longitude,latitude");
        }

        double lon = Double.parseDouble(coordinates[0].trim());
        double lat = Double.parseDouble(coordinates[1].trim());

        return new Coordinate(lon,lat);
    }
}

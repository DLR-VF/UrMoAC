package de.dlr.ivf.urmo.router.spring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UrMoAcResultResponse {

    @JsonProperty("mode")
    private final String mode;

    @JsonProperty("distance")
    private final double distance;

    @JsonProperty("traveltime")
    private final double traveltime;

    @JsonProperty("co2")
    private final double co2;

}

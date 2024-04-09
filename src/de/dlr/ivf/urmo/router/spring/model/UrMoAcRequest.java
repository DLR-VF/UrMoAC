package de.dlr.ivf.urmo.router.spring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UrMoAcRequest {

    @JsonProperty("uuid")
    private final String uuid;
    @JsonProperty("requestId")
    private final long requestId;
    @JsonProperty("startTime")
    private final String startTime;
    @JsonProperty("lonStart")
    private final double lonStart;
    @JsonProperty("latStart")
    private final double latStart;

    @JsonProperty("lonEnd")
    private final double lonEnd;

    @JsonProperty("latEnd")
    private final double latEnd;
}

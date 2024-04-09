package de.dlr.ivf.urmo.router.spring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class UrMoAcResultResponse {
    /**
     * unique identifier of an agent
     */
    @JsonProperty("uuid")
    private String uuid;

    /**
     * internally generated request id
     */
    @JsonProperty("requestId")
    private long requestId;

    /**
     * start time for trip search
     */
    @JsonProperty("startTime")
    private String startTime;

    /**
     * The mode offered
     */
    @JsonProperty("mode")
    private String mode;

    /**
     * wrapper for travel time of trip
     */
    @JsonProperty("travelTime")
    private TravelTime traveltime;

    /**
     * total travel time in seconds
     */
    @JsonProperty("totalTravelTime")
    private int totalTravelTime;

    /**
     * calculated co2 footprint
     */
    @JsonProperty("co2")
    private double co2;

    /**
     * calories consumption
     */
    @JsonProperty("kcal")
    private int kcal;

    /**
     * cost in Euro
     */
    @JsonProperty("prize")
    private double prize;
}

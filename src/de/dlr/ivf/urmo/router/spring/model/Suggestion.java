package de.dlr.ivf.urmo.router.spring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * A representation of a travel offer.
 */
@ToString
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class Suggestion {

    /**
     * The mode offered
     */
    @JsonProperty("mode")
    private String mode;

    /**
     * total travel time of trip
     */
    @JsonProperty("travelTime")
    private int travelTime;

    /**
     * start time of trip
     */
    @JsonProperty("startTime")
    private String startTime;

    /**
     * duration in minutes to access the mode from start location, e.g.: walk to pt station
     */
    @JsonProperty("accessTime")
    private int accessTime;

    /**
     * duration in minutes to get from exiting mode to destination location
     */
    @JsonProperty("egressTime")
    private int egressTime;

    /**
     * count of vehicle transition during trip
     */
    @JsonProperty("transitionCount")
    private int transitionCount;

    /**
     * total transition duration in minutes between vehicles
     */
    @JsonProperty("transitionTime")
    private int transitionTime;

    /**
     * cost in Euro for the whole trip
     */
    @JsonProperty("cost")
    private double cost;

    /**
     * this is either a CO2 or a kcal metric depending on scientific group
     */
    @JsonProperty("payload")
    private double payload;
}

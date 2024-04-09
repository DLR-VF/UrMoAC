package de.dlr.ivf.urmo.router.spring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
public class TravelTime{
    /**
     * duration in seconds to access the mode from start location, e.g.: walk to pt station
     */
    @JsonProperty("accessTime")
    private int accessTime;

    /**
     * total travel time in seconds of trip
     */
    @JsonProperty("travelTime")
    private int travelTime;

    /**
     * duration in seconds to get from exiting mode to destination location
     */
    @JsonProperty("egressTime")
    private int egressTime;
}

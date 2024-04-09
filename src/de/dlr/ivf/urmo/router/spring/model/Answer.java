package de.dlr.ivf.urmo.router.spring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@ToString
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class Answer {
    /**
     * unique identifier of an agent
     */
    @JsonProperty("uuid")
    private String uuid;

    /**
     * start time for trip search
     */
    @JsonProperty("startTime")
    private String startTime;

    /**
     * all trip suggestions offered inside mobile app
     */
    @JsonProperty("suggestions")
    private List<Suggestion> suggestions;

    /**
     * chosen suggestion by the agent
     */
    @JsonProperty("pick")
    private Suggestion pick;

    /**
     * original alternative requested by 'route-request' endpoint
     */
    @JsonProperty("urMoAcResponse")
    private UrMoAcResultResponse urMoAcResponse;

    /**
     * flag whether the agent would have chosen the alternative offer
     */
    @JsonProperty("wouldUseAlternative")
    private boolean wouldUseAlternative;
}

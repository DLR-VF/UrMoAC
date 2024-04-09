package de.dlr.ivf.urmo.router.spring.mode;

import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.spring.configuration.FerryConfiguration;
import jakarta.annotation.PostConstruct;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ModeService {
    @Autowired
    private FerryConfiguration ferryConfiguration;

    @Autowired
    private ModeSetBuilder modeSetBuilder;

    private long modes;

    public long getModes() {
        return modes;
    }


    @PostConstruct
    public void init() {
        modes = Modes.getCombinedModeIDs(Modes.modes);
    }

    public AlternativeMode generateRandom(Coordinate from, Coordinate to){
        List<AlternativeMode> alternativeModes = modeSetBuilder.generateModeSet(from, to);

        return alternativeModes.get(ThreadLocalRandom.current().nextInt(alternativeModes.size()));
    }
}

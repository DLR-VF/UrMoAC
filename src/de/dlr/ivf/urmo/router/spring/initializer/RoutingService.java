package de.dlr.ivf.urmo.router.spring.initializer;

import de.dlr.ivf.urmo.router.algorithms.edgemapper.NearestEdgeFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoutingService implements CommandLineRunner {

    @Autowired
    private NetService network;

    @Autowired
    private ModeService modeService;

    private NearestEdgeFinder edgeFinder;

    @Override
    public void run(String... args) throws Exception {
        this.edgeFinder = new NearestEdgeFinder(network.getNetwork(), modeService.getModes());
    }
}

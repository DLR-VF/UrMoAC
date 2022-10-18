package de.dlr.ivf.urmo.router.spring.initializer;

import de.dlr.ivf.urmo.router.io.NetLoader;
import de.dlr.ivf.urmo.router.shapes.DBNet;
import de.dlr.ivf.urmo.router.shapes.IDGiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class NetService implements CommandLineRunner, IDGiver {
    private long runningID = 0;
    @Value("${urmoac.net.network}")
    private Path networkPath;

    @Autowired
    ModeService modeService;

    private DBNet network;

    public DBNet getNetwork() {
        return network;
    }

    @Override
    public void run(String... args) throws Exception {
        this.network = NetLoader.loadNetFromCSVFile(this, networkPath.toString(),modeService.getModes());
        network.pruneForModes(modeService.getModes());
    }

    @Override
    public long getNextRunningID() {
        return ++runningID;
    }

    @Override
    public void hadExternID(long id) {
        runningID = Math.max(runningID, id+1);

    }
}

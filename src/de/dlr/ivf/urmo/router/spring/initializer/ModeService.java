package de.dlr.ivf.urmo.router.spring.initializer;

import de.dlr.ivf.urmo.router.modes.Mode;
import de.dlr.ivf.urmo.router.modes.Modes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

@Component
public class ModeService implements CommandLineRunner {
    @Value("#{'${urmoac.mode}'.split(';')}")
    List<String> modeParam;

    private long modes;

    private Vector<Mode> modesV;

    public long getModes() {
        return modes;
    }

    public Vector<Mode> getModesV() {
        return modesV;
    }

    @Override
    public void run(String... args) throws Exception {
        Modes.init();
        modesV = modeParam.stream()
                .map(Modes::getMode)
                .collect(Collectors.toCollection(Vector::new));

        modes = Modes.getCombinedModeIDs(modesV);
        //initMode = modesV.get(0).id;

    }
}

package de.dlr.ivf.urmo.router.spring.mode;

import de.dlr.ivf.urmo.router.spring.configuration.FerryConfiguration;
import de.dlr.ivf.urmo.router.spring.model.AccessibleNode;
import de.dlr.ivf.urmo.router.spring.util.WGSDistanceCalculator;
import jakarta.annotation.PostConstruct;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@NoArgsConstructor
@Component
public class ModeSetBuilder {

    @Autowired
    FerryConfiguration ferryConfiguration;
    private Collection<Coordinate> ferryStructureCoordinates;
    public List<AlternativeMode> generateModeSet(Coordinate start, Coordinate end){

        //these modes are always available
        List<AlternativeMode> availableAlternativeModes = new ArrayList<>(4);
        availableAlternativeModes.add(AlternativeMode.BIKESHARING);
        availableAlternativeModes.add(AlternativeMode.CARPOOLING);
        availableAlternativeModes.add(AlternativeMode.CALLABUS);

        if(isFerryFeasible(start,end)){
            availableAlternativeModes.add(AlternativeMode.FERRY);
        }

        return availableAlternativeModes;
    }

    private boolean isFerryFeasible(Coordinate start, Coordinate end) {

        Optional<AccessibleNode> accessibleStartTerminal = getAccessibleNode(start);
        Optional<AccessibleNode> accessibleEndTerminal = getAccessibleNode(end);

        if(accessibleStartTerminal.isEmpty() || accessibleEndTerminal.isEmpty()){
            return false;
        }
        AccessibleNode startAccess = accessibleStartTerminal.get();
        AccessibleNode endAccess = accessibleEndTerminal.get();

        return !startAccess.destination().equals(endAccess.destination());
    }

    private Optional<AccessibleNode> getAccessibleNode(Coordinate start) {
        WGSDistanceCalculator distanceCalculator = new WGSDistanceCalculator(start);

        Collection<AccessibleNode> accessibleNodes = new ArrayList<>();

        for(Coordinate ferryStructureCoordinate : ferryStructureCoordinates) {
            double distance = distanceCalculator.getDistance(ferryStructureCoordinate);
            if (distance <= ferryConfiguration.maxAccessEgressDistanceToTerminals()) {
                accessibleNodes.add(new AccessibleNode(ferryStructureCoordinate, distance));
            }
        }
        return accessibleNodes.stream().min(Comparator.comparingDouble(AccessibleNode::distance));
    }

    @PostConstruct
    public void init(){
        ferryStructureCoordinates = List.of(ferryConfiguration.firstTerminal(), ferryConfiguration.secondTerminal());
    }
}

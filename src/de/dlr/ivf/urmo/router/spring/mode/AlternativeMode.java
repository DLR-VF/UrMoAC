package de.dlr.ivf.urmo.router.spring.mode;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public enum AlternativeMode {

    FOOT("Zu Fuß",3.6,0,280,0, 2),
    //ostseefähre
    FERRY("Fähre", 28, 0.03, 85,10, -1),

    //flexbus
    CALLABUS("Rufbus", 80,75,85,0, 8),
    BIKESHARING("E-Bike Sharing", 20,0, 510,0, 4),
    CARPOOLING("Mitfahrangebot", 200, 150, 170,31, 8);

    private static final List<AlternativeMode> VALUES =
            List.of(values());
    private static final int SIZE = VALUES.size();
    private static final Random RANDOM = new Random();


    private final String translatedName;
    private final double speed;
    private final double co2PerKm;

    private final double kkcPerHour;

    private final double pricePerKm;
    private final long urmoModeId;


    private AlternativeMode(String modeTranslation, double vmax, double co2PerKm, double kkcPerHour, double pricePerKm, long urmoModeId) {

        this.translatedName = modeTranslation;
        this.speed = vmax/3.6;
        this.co2PerKm = co2PerKm;
        this.kkcPerHour = kkcPerHour;
        this.pricePerKm = pricePerKm;
        this.urmoModeId = urmoModeId;
    }

    public static AlternativeMode randomMode() {
        return VALUES.get(RANDOM.nextInt(SIZE));
    }

    public double getSpeed(){
        return this.speed;
    }

    public long getUrmoModeId(){
        return this.urmoModeId;
    }

    public String getTranslatedName(){
        return translatedName;
    }

    public double getCo2PerKm() {
        return co2PerKm;
    }

    public double getKkcPerHour() {
        return kkcPerHour;
    }

    public double getPricePerKm() {
        return pricePerKm;
    }

    public static Collection<AlternativeMode> modes(){
        return VALUES;
    }
}

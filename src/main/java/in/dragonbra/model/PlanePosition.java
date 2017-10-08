package in.dragonbra.model;

import org.apache.commons.math3.geometry.euclidean.threed.SphericalCoordinates;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * @author lngtr
 * @since 2017-10-02
 */
public class PlanePosition implements Serializable {

    public static final double R_EARTH_FEET = 20898950.1312;

    private static final DecimalFormat FORMAT = new DecimalFormat("#.####");

    static {
        FORMAT.setRoundingMode(RoundingMode.HALF_UP);
    }

    private SphericalCoordinates sphericalCoordinates;

    private double timestamp;

    private String icao24;

    private boolean isAirborne;

    public PlanePosition(SphericalCoordinates sphericalCoordinates, double timestamp, String icao24, boolean isAirborne) {
        this.sphericalCoordinates = sphericalCoordinates;
        this.timestamp = timestamp;
        this.icao24 = icao24;
        this.isAirborne = isAirborne;
    }

    public SphericalCoordinates getSphericalCoordinates() {
        return sphericalCoordinates;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public String getIcao24() {
        return icao24;
    }

    public boolean isAirborne() {
        return isAirborne;
    }

    @Override
    public String toString() {
        return icao24 + "," +
                (isAirborne ? "1" : "0") + "," +
                timestamp + "," +
                Math.toDegrees(sphericalCoordinates.getTheta()) + "," +
                (Math.toDegrees(sphericalCoordinates.getPhi()) - 90) + "," +
                FORMAT.format(sphericalCoordinates.getR() - R_EARTH_FEET);

    }
}

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

    private boolean end;

    public PlanePosition(SphericalCoordinates sphericalCoordinates, double timestamp, String icao24, boolean isAirborne) {
        this(sphericalCoordinates, timestamp, icao24, isAirborne, false);
    }

    public PlanePosition(SphericalCoordinates sphericalCoordinates, double timestamp, String icao24, boolean isAirborne, boolean end) {
        this.sphericalCoordinates = sphericalCoordinates;
        this.timestamp = timestamp;
        this.icao24 = icao24;
        this.isAirborne = isAirborne;
        this.end = end;
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
                getLongitude() + "," +
                getLatitude() + "," +
                FORMAT.format(getAltitude()) + "," +
                (end ? 1 : 0);

    }

    public boolean isEnd() {
        return end;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    public double getAltitude() {
        return sphericalCoordinates.getR() - R_EARTH_FEET;
    }

    public double getLongitude() {
        return Math.toDegrees(sphericalCoordinates.getTheta());
    }

    public double getLatitude() {
        return Math.toDegrees(sphericalCoordinates.getPhi()) - 90;
    }
}

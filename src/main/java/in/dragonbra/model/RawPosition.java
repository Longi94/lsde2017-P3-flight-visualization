package in.dragonbra.model;

import java.io.Serializable;

/**
 * @author lngtr
 * @since 2017-10-11
 */
public class RawPosition implements Serializable {

    private double timestamp;

    private double longitude;

    private double latitude;

    private double altitude;

    private boolean isAirborne;

    private boolean isEnd;

    public RawPosition(double timestamp, double longitude, double latitude, double altitude, boolean isAirborne, boolean isEnd) {

        this.timestamp = timestamp;
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.isAirborne = isAirborne;
        this.isEnd = isEnd;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public boolean isAirborne() {
        return isAirborne;
    }

    public void setAirborne(boolean airborne) {
        isAirborne = airborne;
    }

    public boolean isEnd() {
        return isEnd;
    }

    public void setEnd(boolean end) {
        isEnd = end;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }
}

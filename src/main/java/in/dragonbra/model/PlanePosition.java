package in.dragonbra.model;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.io.Serializable;

/**
 * @author lngtr
 * @since 2017-10-02
 */
public class PlanePosition implements Serializable {

    private Vector3D pos;

    private double timestamp;

    private String icao24;

    private boolean isAirborne;

    public PlanePosition(Vector3D pos, double timestamp, String icao24, boolean isAirborne) {
        this.pos = pos;
        this.timestamp = timestamp;
        this.icao24 = icao24;
        this.isAirborne = isAirborne;
    }

    public Vector3D getPos() {
        return pos;
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
                pos.getX() + "," +
                pos.getY() + "," +
                pos.getZ();

    }
}

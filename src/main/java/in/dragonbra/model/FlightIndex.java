package in.dragonbra.model;

import java.io.Serializable;

/**
 * @author lngtr
 * @since 2017-10-17
 */
public class FlightIndex implements Serializable {

    private int[] flightOffsets;

    private String[] airlineNames;

    private String[] flightIdentities;

    public int[] getFlightOffsets() {
        return flightOffsets;
    }

    public void setFlightOffsets(int[] flightOffsets) {
        this.flightOffsets = flightOffsets;
    }

    public String[] getAirlineNames() {
        return airlineNames;
    }

    public void setAirlineNames(String[] airlineNames) {
        this.airlineNames = airlineNames;
    }

    public String[] getFlightIdentities() {
        return flightIdentities;
    }

    public void setFlightIdentities(String[] flightIdentities) {
        this.flightIdentities = flightIdentities;
    }
}

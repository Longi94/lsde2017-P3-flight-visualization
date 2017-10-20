package in.dragonbra.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lngtr
 * @since 2017-10-12
 */
public class Flight implements Serializable {

    private String icao24;

    @Expose
    @SerializedName("t")
    private List<Double> ts;
    @Expose
    @SerializedName("x")
    private List<Double> lon;
    @Expose
    @SerializedName("y")
    private List<Double> lat;
    @Expose
    @SerializedName("z")
    private List<Double> alt;

    @Expose
    @SerializedName("c")
    private String identity;

    public Flight(String icao24, String identity, List<Double> ts, List<Double> lon, List<Double> lat, List<Double> alt) {
        this.icao24 = icao24;
        this.identity = identity;
        this.ts = ts;
        this.lon = lon;
        this.lat = lat;
        this.alt = alt;
    }

    public Flight(String icao24, String identity) {
        this.icao24 = icao24;
        this.identity = identity;
        this.ts = new ArrayList<>();
        this.lon = new ArrayList<>();
        this.lat = new ArrayList<>();
        this.alt = new ArrayList<>();
    }

    public String getIcao24() {
        return icao24;
    }

    public void setIcao24(String icao24) {
        this.icao24 = icao24;
    }

    public List<Double> getTs() {
        return ts;
    }

    public void setTs(List<Double> ts) {
        this.ts = ts;
    }

    public List<Double> getLon() {
        return lon;
    }

    public void setLon(List<Double> lon) {
        this.lon = lon;
    }

    public List<Double> getLat() {
        return lat;
    }

    public void setLat(List<Double> lat) {
        this.lat = lat;
    }

    public List<Double> getAlt() {
        return alt;
    }

    public void setAlt(List<Double> alt) {
        this.alt = alt;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }
}

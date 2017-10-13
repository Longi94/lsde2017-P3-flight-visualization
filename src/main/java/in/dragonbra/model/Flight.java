package in.dragonbra.model;

import java.io.Serializable;

/**
 * @author lngtr
 * @since 2017-10-12
 */
public class Flight implements Serializable {

    private String icao24;

    private Double[] ts;
    private Float[] lon;
    private Float[] lat;
    private Float[] alt;

    public Flight(String icao24, Double[] ts, Float[] lon, Float[] lat, Float[] alt) {
        this.icao24 = icao24;
        this.ts = ts;
        this.lon = lon;
        this.lat = lat;
        this.alt = alt;
    }

    public String getIcao24() {
        return icao24;
    }

    public void setIcao24(String icao24) {
        this.icao24 = icao24;
    }

    public Double[] getTs() {
        return ts;
    }

    public void setTs(Double[] ts) {
        this.ts = ts;
    }

    public Float[] getLon() {
        return lon;
    }

    public void setLon(Float[] lon) {
        this.lon = lon;
    }

    public Float[] getLat() {
        return lat;
    }

    public void setLat(Float[] lat) {
        this.lat = lat;
    }

    public Float[] getAlt() {
        return alt;
    }

    public void setAlt(Float[] alt) {
        this.alt = alt;
    }
}

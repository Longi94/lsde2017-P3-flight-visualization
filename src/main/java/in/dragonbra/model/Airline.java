package in.dragonbra.model;

import java.util.HashSet;
import java.util.Set;

/**
 * @author lngtr
 * @since 2017-10-17
 */
public class Airline {
    private String icao;

    private String name;

    private Set<String> flightIdentities = new HashSet<>();

    public Airline(String icao, String name) {
        this.icao = icao;
        this.name = name;
    }

    public String getIcao() {
        return icao;
    }

    public void setIcao(String icao) {
        this.icao = icao;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getFlightIdentities() {
        return flightIdentities;
    }

    public void setFlightIdentities(Set<String> flightIdentities) {
        this.flightIdentities = flightIdentities;
    }
}

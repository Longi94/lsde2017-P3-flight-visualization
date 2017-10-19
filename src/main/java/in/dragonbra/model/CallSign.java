package in.dragonbra.model;

import java.io.Serializable;

/**
 * @author lngtr
 * @since 2017-10-17
 */
public class CallSign implements Serializable {

    private final double timestamp;

    private final String identity;

    public CallSign(double timestamp, String identity) {
        this.timestamp = timestamp;
        this.identity = identity;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public String getIdentity() {
        return identity;
    }
}

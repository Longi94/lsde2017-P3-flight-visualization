package in.dragonbra.model;

import org.opensky.libadsb.msgs.ModeSReply;

import java.io.Serializable;

/**
 * @author lngtr
 * @since 2017-09-30
 */
public class Message implements Serializable{

    private double timeStamp;

    private ModeSReply modeSReply;

    public double getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(double timeStamp) {
        this.timeStamp = timeStamp;
    }

    public ModeSReply getModeSReply() {
        return modeSReply;
    }

    public void setModeSReply(ModeSReply modeSReply) {
        this.modeSReply = modeSReply;
    }
}

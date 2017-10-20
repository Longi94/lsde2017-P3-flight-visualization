package in.dragonbra.model;

import java.util.List;

/**
 * @author lngtr
 * @since 2017-10-19
 */
public class SplitFlight {
    private Flight f1;

    private Flight f2;

    public SplitFlight(Flight original, double timestamp) {
        this.f1 = new Flight(original.getIcao24(), original.getIdentity());
        this.f2 = new Flight(original.getIcao24(), original.getIdentity());

        List<Double> ts = original.getTs();

        for (int i = 0; i < ts.size() - 1; i++) {
            if (timestamp == ts.get(i)) {
                f1.getTs().add(original.getTs().get(i));
                f1.getLon().add(original.getLon().get(i));
                f1.getLat().add(original.getLat().get(i));
                f1.getAlt().add(original.getAlt().get(i));
                f2.getTs().add(original.getTs().get(i));
                f2.getLon().add(original.getLon().get(i));
                f2.getLat().add(original.getLat().get(i));
                f2.getAlt().add(original.getAlt().get(i));
            }
            else if (timestamp > ts.get(i) && timestamp < ts.get(i + 1)) {
                f1.getTs().add(original.getTs().get(i));
                f1.getLon().add(original.getLon().get(i));
                f1.getLat().add(original.getLat().get(i));
                f1.getAlt().add(original.getAlt().get(i));

                double step = (timestamp - ts.get(i)) / (ts.get(i + 1) - ts.get(i));

                double cutLon = original.getLon().get(i) + step * (original.getLon().get(i + 1) - original.getLon().get(i));
                double cutLan = original.getLat().get(i) + step * (original.getLat().get(i + 1) - original.getLat().get(i));
                double cutAlt = original.getAlt().get(i) + step * (original.getAlt().get(i + 1) - original.getAlt().get(i));

                f1.getTs().add(timestamp);
                f1.getLon().add(cutLon);
                f1.getLat().add(cutLan);
                f1.getAlt().add(cutAlt);

                f2.getTs().add(timestamp);
                f2.getLon().add(cutLon);
                f2.getLat().add(cutLan);
                f2.getAlt().add(cutAlt);
            } else if (timestamp > ts.get(i)) {
                f1.getTs().add(original.getTs().get(i));
                f1.getLon().add(original.getLon().get(i));
                f1.getLat().add(original.getLat().get(i));
                f1.getAlt().add(original.getAlt().get(i));
            } else {
                f2.getTs().add(original.getTs().get(i));
                f2.getLon().add(original.getLon().get(i));
                f2.getLat().add(original.getLat().get(i));
                f2.getAlt().add(original.getAlt().get(i));
            }
        }

        f2.getTs().add(original.getTs().get(original.getTs().size() - 1));
        f2.getLon().add(original.getLon().get(original.getLon().size() - 1));
        f2.getLat().add(original.getLat().get(original.getLat().size() - 1));
        f2.getAlt().add(original.getAlt().get(original.getAlt().size() - 1));
    }

    public Flight getF1() {
        return f1;
    }

    public Flight getF2() {
        return f2;
    }
}

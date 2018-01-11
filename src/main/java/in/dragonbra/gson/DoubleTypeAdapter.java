package in.dragonbra.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * @author lngtr
 * @since 2017-10-20
 */
public class DoubleTypeAdapter extends TypeAdapter<Double> {
    private static final DecimalFormat FORMAT = new DecimalFormat("###.##");

    static {
        FORMAT.setRoundingMode(RoundingMode.HALF_UP);
    }

    @Override
    public void write(JsonWriter out, Double value) throws IOException {
        out.jsonValue(FORMAT.format(value));
    }

    @Override
    public Double read(JsonReader in) throws IOException {
        return in.nextDouble();
    }
}

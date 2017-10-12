package in.dragonbra.util;

import in.dragonbra.model.PlanePosition;
import org.apache.commons.math3.geometry.euclidean.threed.SphericalCoordinates;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

/**
 * @author lngtr
 * @since 2017-10-07
 */
public class SparkUtils {

    public static JavaRDD<PlanePosition> readCsv(Dataset<Row> df) {
        // map the csv to java objects
        return df.map(new MapFunction<Row, PlanePosition>() {
            @Override
            public PlanePosition call(Row row) throws Exception {
                String lon = row.getString(3);
                String lat = row.getString(4);
                String alt = row.getString(5);
                String timestamp = row.getString(2);
                String isAirborne = row.getString(1);
                String icao24 = row.getString(0);

                if (row.size() > 6) {
                    String isEnd = row.getString(6);
                    return new PlanePosition(
                            new SphericalCoordinates(
                                    Double.parseDouble(alt) + PlanePosition.R_EARTH_FEET,
                                    Math.toRadians(Double.parseDouble(lon)),
                                    Math.toRadians(Double.parseDouble(lat) + 90)
                            ),
                            Double.parseDouble(timestamp),
                            icao24,
                            "1".equals(isAirborne),
                            "1".equals(isEnd)
                    );
                } else {
                    return new PlanePosition(
                            new SphericalCoordinates(
                                    Double.parseDouble(alt) + PlanePosition.R_EARTH_FEET,
                                    Math.toRadians(Double.parseDouble(lon)),
                                    Math.toRadians(Double.parseDouble(lat) + 90)
                            ),
                            Double.parseDouble(timestamp),
                            icao24,
                            "1".equals(isAirborne)
                    );
                }
            }
        }, Encoders.kryo(PlanePosition.class)).javaRDD();
    }
}

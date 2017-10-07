package in.dragonbra.util;

import in.dragonbra.model.PlanePosition;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
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
                String x = row.getString(3);
                String y = row.getString(4);
                String z = row.getString(5);
                String timestamp = row.getString(2);
                String isAirborne = row.getString(1);
                String icao24 = row.getString(0);
                //System.out.println(icao24 + " " + timestamp);
                return new PlanePosition(
                        new Vector3D(Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z)),
                        Double.parseDouble(timestamp),
                        icao24,
                        "1".equals(isAirborne)
                );
            }
        }, Encoders.kryo(PlanePosition.class)).javaRDD();
    }
}

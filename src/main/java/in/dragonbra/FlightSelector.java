package in.dragonbra;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.SparkSession;

import java.io.File;

/**
 * @author lngtr
 * @since 2017-10-03
 */
public class FlightSelector {

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Usage: [input path] [icao24]");
            System.exit(1);
        }

        final String icao24 = args[1];

        if (new File(icao24).exists()) {
            System.out.println(icao24 + " already exists.");
            System.exit(1);
        }

        long start = System.currentTimeMillis();

        SparkSession spark = SparkSession
                .builder()
                .appName(PositionExtractor.class.getSimpleName())
                .getOrCreate();

        // Creates a DataFrame from a specified file
        JavaRDD<String> lines = spark.read().textFile(args[0]).javaRDD();

        JavaRDD<String> selectedFlight = lines.filter(new Function<String, Boolean>() {
            @Override
            public Boolean call(String line) throws Exception {
                return line.startsWith(icao24);
            }
        });

        selectedFlight.saveAsTextFile(icao24);

        spark.stop();

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + " milliseconds.");
    }
}

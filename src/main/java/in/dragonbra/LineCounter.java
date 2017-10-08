package in.dragonbra;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.SparkSession;

/**
 * @author lngtr
 * @since 2017-10-08
 */
public class LineCounter {

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Usage: [input path]");
            System.exit(1);
        }

        long start = System.currentTimeMillis();

        SparkSession spark = SparkSession
                .builder()
                .appName(PositionExtractor.class.getSimpleName())
                .getOrCreate();

        // Creates a DataFrame from a specified file
        JavaRDD<String> lines = spark.read().textFile(args[0]).javaRDD();

        long count = lines.count();

        spark.stop();

        System.out.println("Number of lines: " + count);

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + " milliseconds.");
    }
}

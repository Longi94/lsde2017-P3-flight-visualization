package in.dragonbra;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.opensky.libadsb.Decoder;
import org.opensky.libadsb.msgs.ModeSReply;

public class FlightVisualizer {
    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Usage: [input avro file]");
            System.exit(1);
        }

        SparkSession spark = SparkSession
                .builder()
                .appName("group06")
                .master("local")
                .getOrCreate();

        // Creates a DataFrame from a specified file
        Dataset<Row> df = spark.read().format("com.databricks.spark.avro").load(args[0]);

        JavaRDD<ModeSReply> messages = df.sort("timeAtServer")
                .select("timeAtServer", "rawMessage")
                .map(value -> {
                    try {
                        return Decoder.genericDecoder(value.getString(1));
                    } catch (Exception ignored) {
                    }
                    return null;
                }, Encoders.kryo(ModeSReply.class))
                .filter(p -> p != null
                        && (p.getType() == ModeSReply.subtype.ADSB_AIRBORN_POSITION
                        || p.getType() == ModeSReply.subtype.ADSB_SURFACE_POSITION))
                .javaRDD();

        messages.saveAsTextFile("sample-out");

        spark.stop();
    }
}

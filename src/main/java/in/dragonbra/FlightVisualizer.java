package in.dragonbra;

import org.apache.commons.io.FileUtils;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.opensky.libadsb.Decoder;
import org.opensky.libadsb.msgs.ModeSReply;

import java.io.File;
import java.io.IOException;

public class FlightVisualizer {

    public static final String OUTPUT_PATH = "sample-out";

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            System.out.println("Usage: [input directory]");
            System.exit(1);
        }

        String[] paths = args[0].split(",");

        FileUtils.deleteDirectory(new File(OUTPUT_PATH));

        SparkSession spark = SparkSession
                .builder()
                .appName(MessageTypeStats.class.getSimpleName())
                .getOrCreate();

        // Creates a DataFrame from a specified file
        Dataset<Row> df = spark.read().format("com.databricks.spark.avro").load(paths);

        JavaRDD<ModeSReply> messages = df.sort("timeAtServer")
                .select("timeAtServer", "rawMessage")
                .map(new MapFunction<Row, ModeSReply>() {
                    @Override
                    public ModeSReply call(Row value) throws Exception {
                        try {
                            return Decoder.genericDecoder(value.getString(1));
                        } catch (Exception ignored) {
                        }
                        return null;
                    }
                }, Encoders.kryo(ModeSReply.class))
                .filter(new FilterFunction<ModeSReply>() {
                    @Override
                    public boolean call(ModeSReply p) throws Exception {
                        return p != null
                                && (p.getType() == ModeSReply.subtype.ADSB_AIRBORN_POSITION
                                || p.getType() == ModeSReply.subtype.ADSB_SURFACE_POSITION);
                    }
                })
                .javaRDD();

        messages.saveAsTextFile(OUTPUT_PATH);

        spark.stop();
    }
}

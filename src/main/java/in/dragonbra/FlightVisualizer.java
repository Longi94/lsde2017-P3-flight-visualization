package in.dragonbra;

import org.apache.commons.io.FileUtils;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.opensky.libadsb.Decoder;
import org.opensky.libadsb.msgs.ModeSReply;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FlightVisualizer {

    public static final String OUTPUT_PATH = "sample-out";

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            System.out.println("Usage: [input directory]");
            System.exit(1);
        }

        Collection<File> files = FileUtils.listFiles(new File(args[0]), new String[]{"avro"}, true);
        List<String> paths = files.stream().map(File::getAbsolutePath).collect(Collectors.toList());

        FileUtils.deleteDirectory(new File(OUTPUT_PATH));

        SparkSession spark = SparkSession
                .builder()
                .appName("group06")
                .master("local")
                .getOrCreate();

        // Creates a DataFrame from a specified file
        Dataset<Row> df = spark.read().format("com.databricks.spark.avro")
                .load(paths.toArray(new String[paths.size()]));

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

        messages.saveAsTextFile(OUTPUT_PATH);

        spark.stop();
    }
}

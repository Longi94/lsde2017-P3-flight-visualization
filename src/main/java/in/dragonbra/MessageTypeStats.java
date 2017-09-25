package in.dragonbra;

import org.apache.commons.io.FileUtils;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.opensky.libadsb.Decoder;
import org.opensky.libadsb.msgs.ModeSReply;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageTypeStats {

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("Usage: [input directory] [output file]");
            System.exit(1);
        }

        long start = System.currentTimeMillis();

        Collection<File> files = FileUtils.listFiles(new File(args[0]), new String[]{"avro"}, true);
        List<String> paths = files.stream().map(File::getAbsolutePath).collect(Collectors.toList());

        SparkSession spark = SparkSession
                .builder()
                .appName("group06")
                .master("local")
                .getOrCreate();

        // Creates a DataFrame from a specified file
        Dataset<Row> df = spark.read().format("com.databricks.spark.avro")
                .load(paths.toArray(new String[paths.size()]));

        JavaRDD<ModeSReply.subtype> types = df.select("rawMessage")
                .map(value -> {
                    try {
                        return Decoder.genericDecoder(value.getString(0)).getType();
                    } catch (Exception ignored) {
                    }
                    return ModeSReply.subtype.MODES_REPLY;
                }, Encoders.kryo(ModeSReply.subtype.class))
                .javaRDD();

        Map<ModeSReply.subtype, Long> typeCount = types.countByValue();

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(args[1]), "utf-8"))) {
            for (Map.Entry<ModeSReply.subtype, Long> entry : typeCount.entrySet()) {
                writer.write(entry.getKey().toString() + "," + entry.getValue().toString());
                writer.write('\n');
            }
        }

        spark.stop();

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + " milliseconds.");
    }
}

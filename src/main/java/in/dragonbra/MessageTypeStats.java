package in.dragonbra;

import org.apache.log4j.Logger;
import org.apache.parquet.Strings;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.opensky.libadsb.Decoder;
import org.opensky.libadsb.msgs.ModeSReply;

import java.io.*;
import java.util.Map;

public class MessageTypeStats {

    public static final Logger logger = Logger.getLogger(MessageTypeStats.class);

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("Usage: [input directory] [output file]");
            System.exit(1);
        }

        logger.info("Arguments = " + Strings.join(args, " "));
        logger.info("Working Directory = " + System.getProperty("user.dir"));

        long start = System.currentTimeMillis();

        String[] paths = args[0].split(",");
        SparkSession spark = SparkSession
                .builder()
                .appName(MessageTypeStats.class.getSimpleName())
                .getOrCreate();

        // Creates a DataFrame from a specified file
        Dataset<Row> df = spark.read().format("com.databricks.spark.avro").load(paths);

        JavaRDD<ModeSReply.subtype> types = df.select("rawMessage")
                .map(new MapFunction<Row, ModeSReply.subtype>() {
                    @Override
                    public ModeSReply.subtype call(Row value) throws Exception {
                        try {
                            return Decoder.genericDecoder(value.getString(0)).getType();
                        } catch (Exception ignored) {
                        }
                        return ModeSReply.subtype.MODES_REPLY;
                    }
                }, Encoders.kryo(ModeSReply.subtype.class))
                .javaRDD();

        Map<ModeSReply.subtype, Long> typeCount = types.countByValue();

        for (Map.Entry<ModeSReply.subtype, Long> entry : typeCount.entrySet()) {
            System.out.println(entry.getKey().toString() + "," + entry.getValue().toString());
        }

        spark.stop();

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + " milliseconds.");
    }
}

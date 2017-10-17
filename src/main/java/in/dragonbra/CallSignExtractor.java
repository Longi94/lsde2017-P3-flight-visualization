package in.dragonbra;

import in.dragonbra.model.Message;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.opensky.libadsb.Decoder;
import org.opensky.libadsb.msgs.IdentificationMsg;
import org.opensky.libadsb.msgs.ModeSReply.subtype;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.opensky.libadsb.tools.toHexString;

/**
 * @author lngtr
 * @since 2017-10-17
 */
public class CallSignExtractor {

    private static final String OUTPUT_PATH = "spark-data/call-signs";

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            System.out.println("Usage: [input directory]");
            System.exit(1);
        }

        if (new File(OUTPUT_PATH).exists()) {
            System.out.println(OUTPUT_PATH + " already exists.");
            System.exit(1);
        }

        long start = System.currentTimeMillis();

        String[] paths = args[0].split(",");

        SparkSession spark = SparkSession
                .builder()
                .appName(CallSignExtractor.class.getSimpleName())
                .getOrCreate();

        // Creates a DataFrame from a specified file
        Dataset<Row> df = spark.read().format("com.databricks.spark.avro").load(paths);

        JavaRDD<Message> messages = df

                // We are interested in the raw message and the time it arrived to the server
                .select("timeAtServer", "rawMessage")

                // Decode the raw message to ModeSReply object
                .map(new MapFunction<Row, Message>() {
                    @Override
                    public Message call(Row row) throws Exception {
                        Message message = new Message();

                        // I don't like tuples so I'm attaching the timestamp to the message using a new class
                        message.setTimeStamp(row.getDouble(0));
                        try {
                            message.setModeSReply(Decoder.genericDecoder(row.getString(1)));
                        } catch (Exception ignored) {
                            message.setModeSReply(null);
                        }

                        return message;
                    }
                }, Encoders.kryo(Message.class))

                // We only need the position messages
                .filter(new FilterFunction<Message>() {
                    @Override
                    public boolean call(Message message) throws Exception {
                        return message.getModeSReply() != null
                                && (message.getModeSReply().getType() == subtype.ADSB_IDENTIFICATION);
                    }
                })
                .javaRDD();

        JavaRDD<String> callSigns = messages

                // Sort the RDD by timestamp
                .sortBy(new Function<Message, Double>() {
                    @Override
                    public Double call(Message message) throws Exception {
                        return message.getTimeStamp();
                    }
                }, true, messages.partitions().size())

                // Group the positions by the icao24 which identifies the plane
                .groupBy(new Function<Message, String>() {
                    @Override
                    public String call(Message message) throws Exception {
                        return toHexString(message.getModeSReply().getIcao24());
                    }
                })

                // Decode the positions from the messages
                .flatMap(new FlatMapFunction<Tuple2<String, Iterable<Message>>, String>() {
                    @Override
                    public Iterator<String> call(Tuple2<String, Iterable<Message>> t) throws Exception {
                        return decodePositions(t._1, t._2);
                    }
                });

        callSigns.saveAsTextFile(OUTPUT_PATH);

        spark.stop();

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + " milliseconds.");
    }

    private static Iterator<String> decodePositions(String icao24, Iterable<Message> messages) {
        List<String> callSigns = new LinkedList<>();

        // Decoding is done according to the java-adsb example
        // https://github.com/openskynetwork/java-adsb/blob/master/src/main/java/org/opensky/example/ExampleDecoder.java

        String currentIdent = null;
        double startTs = 0;

        for (Message message : messages) {
            IdentificationMsg reply = (IdentificationMsg) message.getModeSReply();
            String ident = new String(reply.getIdentity()).trim();

            if (ident.isEmpty()) continue;

            if (currentIdent == null) {
                currentIdent = ident;
                startTs = message.getTimeStamp();
                callSigns.add(icao24 + "," + startTs + "," + currentIdent);
            }

            if (!ident.equals(currentIdent)) {
                callSigns.add(icao24 + "," + startTs + "," + ident);
                currentIdent = ident;
                startTs = message.getTimeStamp();
            }
        }

        return callSigns.iterator();
    }
}

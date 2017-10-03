package in.dragonbra;

import in.dragonbra.model.Message;
import org.apache.commons.lang3.StringUtils;
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
import org.opensky.libadsb.Position;
import org.opensky.libadsb.PositionDecoder;
import org.opensky.libadsb.msgs.AirbornePositionMsg;
import org.opensky.libadsb.msgs.ModeSReply;
import org.opensky.libadsb.msgs.ModeSReply.subtype;
import org.opensky.libadsb.msgs.SurfacePositionMsg;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.opensky.libadsb.tools.toHexString;

public class PositionExtractor {

    private static final String OUTPUT_PATH = "positions";

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
                .appName(PositionExtractor.class.getSimpleName())
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
                                && (message.getModeSReply().getType() == subtype.ADSB_AIRBORN_POSITION
                                || message.getModeSReply().getType() == subtype.ADSB_SURFACE_POSITION);
                    }
                })
                .javaRDD();

        JavaRDD<String> positions = messages

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

        positions.saveAsTextFile(OUTPUT_PATH);

        spark.stop();

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + " milliseconds.");
    }

    private static Iterator<String> decodePositions(String icao24, Iterable<Message> messages) {
        // There has to be one decoder per icao since multiple messages have to combined to
        // get a single position
        PositionDecoder decoder = new PositionDecoder();

        List<String> positions = new LinkedList<>();

        int isAirborne = 0;

        // Decoding is done according to the java-adsb example
        // https://github.com/openskynetwork/java-adsb/blob/master/src/main/java/org/opensky/example/ExampleDecoder.java

        for (Message message : messages) {
            ModeSReply reply = message.getModeSReply();
            Position pos = null;

            switch (reply.getType()) {
                case ADSB_AIRBORN_POSITION:
                    isAirborne = 1;
                    AirbornePositionMsg apMsg = (AirbornePositionMsg) reply;
                    apMsg.setNICSupplementA(decoder.getNICSupplementA());
                    pos = decoder.decodePosition(message.getTimeStamp(), apMsg);
                    break;
                case ADSB_SURFACE_POSITION:
                    isAirborne = 0;
                    SurfacePositionMsg spMsg = (SurfacePositionMsg) reply;
                    pos = decoder.decodePosition(message.getTimeStamp(), spMsg);
                    break;
            }

            if (pos != null && pos.isReasonable() &&
                    pos.getAltitude() != null && // apparently this is a thing that can happen
                    pos.getLongitude() != null &&
                    pos.getLatitude() != null) {
                // Saving as a csv format for now
                String joined = StringUtils.join(new Object[]{
                                icao24,
                                isAirborne,
                                message.getTimeStamp(),
                                pos.getLongitude(),
                                pos.getLatitude(),
                                pos.getAltitude()
                        },
                        ','
                );
                positions.add(joined);
            }
        }

        return positions.iterator();
    }
}

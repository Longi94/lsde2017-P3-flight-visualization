package in.dragonbra;

import com.google.flatbuffers.FlatBufferBuilder;
import in.dragonbra.flatbuffers.FlightFb;
import in.dragonbra.flatbuffers.FlightsFb;
import in.dragonbra.model.CallSign;
import in.dragonbra.model.Flight;
import in.dragonbra.model.PlanePosition;
import in.dragonbra.util.SparkUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author lngtr
 * @since 2018-01-19
 */
public class CsvToFbs {

    private static final Pattern FILE_PATTERN = Pattern.compile("part-[0-9]{5}");

    private static final String POSITIONS_INPUT_PATH = "spark-data/split-positions";
    private static final String CALL_SIGNS_INPUT_PATH = "spark-data/call-signs";

    private static final double CHUNK_INTERVAL = 14400;
    private static final double START_TS = 1474156800;

    public static void main(String[] args) throws IOException {

        long start = System.currentTimeMillis();

        // Read the call signs first
        Map<String, List<CallSign>> callSigns = new HashMap<>();

        Collection<File> files = FileUtils.listFiles(new File(CALL_SIGNS_INPUT_PATH), new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return FILE_PATTERN.matcher(file.getName()).matches();
            }

            @Override
            public boolean accept(File dir, String name) {
                return FILE_PATTERN.matcher(name).matches();
            }
        }, null);

        // Read the call signs into a map so we can match flights with their identity
        for (File file : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] row = line.split(",");

                    if (!callSigns.containsKey(row[0])) {
                        callSigns.put(row[0], new LinkedList<CallSign>());
                    }

                    callSigns.get(row[0]).add(new CallSign(Double.parseDouble(row[1]), row[2]));
                }
            }
        }

        SparkSession spark = SparkSession
                .builder()
                .appName(PositionExtractor.class.getSimpleName())
                .getOrCreate();

        // Creates a DataFrame from a specified file
        Dataset<Row> df = spark.read().csv(POSITIONS_INPUT_PATH);

        // map the csv to java objects
        final JavaRDD<PlanePosition> positions = SparkUtils.readCsv(df);

        JavaRDD<Flight> reducedPositions = positions
                // group the positions by icao24
                .groupBy(new Function<PlanePosition, String>() {
                    @Override
                    public String call(PlanePosition position) throws Exception {
                        return position.getIcao24();
                    }
                })

                // map to flights
                .flatMap(new FlatMapFunction<Tuple2<String, Iterable<PlanePosition>>, Flight>() {
                    @Override
                    public Iterator<Flight> call(Tuple2<String, Iterable<PlanePosition>> t) throws Exception {

                        List<Flight> flights = new LinkedList<>();
                        List<Double> ts = new ArrayList<>();
                        List<Double> lon = new ArrayList<>();
                        List<Double> lat = new ArrayList<>();
                        List<Double> alt = new ArrayList<>();

                        for (PlanePosition planePos : t._2) {
                            if (planePos.isAirborne()) {
                                ts.add(planePos.getTimestamp() - START_TS);
                                lon.add(planePos.getLongitude());
                                lat.add(planePos.getLatitude());
                                alt.add(planePos.getAltitude());
                            }

                            if ((!planePos.isAirborne() || planePos.isEnd()) && !ts.isEmpty()) {
                                flights.add(new Flight(t._1, null, ts, lon, lat, alt));

                                ts = new ArrayList<>();
                                lon = new ArrayList<>();
                                lat = new ArrayList<>();
                                alt = new ArrayList<>();
                            }
                        }

                        if (!ts.isEmpty()) {
                            flights.add(new Flight(t._1, null, ts, lon, lat, alt));
                        }

                        return flights.iterator();
                    }
                })

                // sort by the start time of the flight
                .sortBy(new Function<Flight, Double>() {
                    @Override
                    public Double call(Flight v1) throws Exception {
                        return v1.getTs().get(0);
                    }
                }, true, positions.getNumPartitions());

        // collect the flights for further processing, have to do it without spark
        List<Flight> flights = new ArrayList<>(reducedPositions.collect());

        System.out.println("Total number of flights " + flights.size());

        spark.stop();

        // temporary arrays for the flight
        List<Flight> currentFlights = new ArrayList<>();
        List<Flight> nextFlights = new ArrayList<>();
        List<Flight> temp;

        // chunks are identified by timestamp
        double currentChunk = flights.get(0).getTs().get(0) - flights.get(0).getTs().get(0) % CHUNK_INTERVAL;

        Iterator<Flight> iterator = flights.iterator();
        Flight flight;

        while (iterator.hasNext()) {
            flight = iterator.next();

            if (flight.getTs().size() < 2) {
                // pointless flight objects
                iterator.remove();
                continue;
            }

            if (currentChunk + CHUNK_INTERVAL <= flight.getTs().get(0)) {

                // finished with one chunk, write the fb
                flatBufSerialize(currentFlights, currentChunk);
                System.out.println("Number of flights in chunk " + String.format("%.0f", currentChunk) + "  " + currentFlights.size());

                currentChunk += CHUNK_INTERVAL;

                // If need be, further split the flights
                temp = nextFlights;
                currentFlights.clear();
                nextFlights = new ArrayList<>();

                for (Flight tempFlight : temp) {
                    CsvToJson.splitFlight(tempFlight, currentFlights, nextFlights, currentChunk);
                }

                temp.clear();
            }

            // get the identity of the flight
            String identity = CsvToJson.getIdentity(callSigns, flight.getIcao24(), flight.getTs().get(0) + START_TS);
            flight.setIdentity(identity);

            CsvToJson.splitFlight(flight, currentFlights, nextFlights, currentChunk);

            iterator.remove();
        }

        // write the last fb too
        flatBufSerialize(currentFlights, currentChunk);
        System.out.println("Number of flights in chunk " + String.format("%.0f", currentChunk) + "  " + currentFlights.size());

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + " milliseconds.");
    }

    private static void flatBufSerialize(List<Flight> flights, double currentChunk) throws IOException {
        FlatBufferBuilder builder = new FlatBufferBuilder();

        Map<String, Integer> identities = new HashMap<>();

        int[] flightOffsets = new int[flights.size()];

        for (int i = 0; i < flights.size(); i++) {
            Flight flight = flights.get(i);

            int identityOffset = 0;
            if (flight.getIdentity() != null) {
                if (identities.containsKey(flight.getIdentity())) {
                    identityOffset = identities.get(flight.getIdentity());
                } else {
                    identityOffset = builder.createString(flight.getIdentity());
                    identities.put(flight.getIdentity(), identityOffset);
                }
            }


            double[] timestamps = new double[flight.getTs().size()];
            float[] longitudes = new float[flight.getTs().size()];
            float[] latitudes = new float[flight.getTs().size()];
            float[] altitudes = new float[flight.getTs().size()];

            for (int j = 0; j < flight.getTs().size(); j++) {
                timestamps[j] = flight.getTs().get(j);
                longitudes[j] = flight.getLon().get(j).floatValue();
                latitudes[j] = flight.getLat().get(j).floatValue();
                altitudes[j] = flight.getAlt().get(j).floatValue();
            }

            int timestampVector = FlightFb.createTimestampsVector(builder, timestamps);
            int longitudesVector = FlightFb.createLongitudesVector(builder, longitudes);
            int latitudesVector = FlightFb.createLatitudesVector(builder, latitudes);
            int altitudesVector = FlightFb.createAltitudesVector(builder, altitudes);

            FlightFb.startFlightFb(builder);
            FlightFb.addTimestamps(builder, timestampVector);
            FlightFb.addLongitudes(builder, longitudesVector);
            FlightFb.addLatitudes(builder, latitudesVector);
            FlightFb.addAltitudes(builder, altitudesVector);
            if (flight.getIdentity() != null) {
                FlightFb.addIdentity(builder, identityOffset);
            }
            flightOffsets[i] = FlightFb.endFlightFb(builder);
        }

        int flightsVector = FlightsFb.createFlightsVector(builder, flightOffsets);

        FlightsFb.startFlightsFb(builder);
        FlightsFb.addFlights(builder, flightsVector);
        int flightsBuf = FlightsFb.endFlightsFb(builder);

        builder.finish(flightsBuf);

        ByteBuffer buffer = builder.dataBuffer();

        File file = new File("spark-data/flights/" + String.format("%.0f", currentChunk) + ".bin");
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);

        WritableByteChannel channel = Channels.newChannel(fos);
        channel.write(buffer);

        fos.flush();
        channel.close();
        fos.close();
    }
}

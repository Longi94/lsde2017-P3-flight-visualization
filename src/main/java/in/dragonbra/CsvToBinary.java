package in.dragonbra;

import com.google.gson.Gson;
import in.dragonbra.model.Airline;
import in.dragonbra.model.CallSign;
import in.dragonbra.model.Flight;
import in.dragonbra.model.PlanePosition;
import in.dragonbra.service.AirlineService;
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
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author lngtr
 * @since 2017-10-12
 */
public class CsvToBinary {

    private static final Pattern FILE_PATTERN = Pattern.compile("part-[0-9]{5}");

    private static final String POSITIONS_INPUT_PATH = "spark-data/split-positions";
    private static final String CALL_SIGNS_INPUT_PATH = "spark-data/call-signs";

    private static final double CHUNK_INTERVAL = 14400;

    public static void main(String[] args) throws IOException {

        long start = System.currentTimeMillis();

        AirlineService airlineService = new AirlineService(args[0]);

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
                        List<Float> lon = new ArrayList<>();
                        List<Float> lat = new ArrayList<>();
                        List<Float> alt = new ArrayList<>();

                        for (PlanePosition planePos : t._2) {
                            if (planePos.isAirborne()) {
                                ts.add(planePos.getTimestamp());
                                lon.add((float) planePos.getLongitude());
                                lat.add((float) planePos.getLatitude());
                                alt.add((float) planePos.getAltitude());
                            }

                            if ((!planePos.isAirborne() || planePos.isEnd()) && !ts.isEmpty()) {
                                flights.add(new Flight(
                                        t._1,
                                        ts.toArray(new Double[ts.size()]),
                                        lon.toArray(new Float[lon.size()]),
                                        lat.toArray(new Float[lat.size()]),
                                        alt.toArray(new Float[alt.size()])
                                ));
                                ts.clear();
                                lon.clear();
                                lat.clear();
                                alt.clear();
                            }
                        }

                        if (!ts.isEmpty()) {
                            flights.add(new Flight(
                                    t._1,
                                    ts.toArray(new Double[ts.size()]),
                                    lon.toArray(new Float[lon.size()]),
                                    lat.toArray(new Float[lat.size()]),
                                    alt.toArray(new Float[alt.size()])
                            ));
                        }

                        return flights.iterator();
                    }
                })

                .sortBy(new Function<Flight, Double>() {
                    @Override
                    public Double call(Flight v1) throws Exception {
                        return v1.getTs()[0];
                    }
                }, true, positions.getNumPartitions());

        List<Flight> flights = reducedPositions.collect();

        spark.stop();

        DataOutputStream positionsOut = new DataOutputStream(new FileOutputStream("spark-data/positions.bin"));
        DataOutputStream flightsOut = new DataOutputStream(new FileOutputStream("spark-data/flights.bin"));
        DataOutputStream timeOut = new DataOutputStream(new FileOutputStream("spark-data/time-index.bin"));
        ObjectOutputStream identityOut = new ObjectOutputStream(new FileOutputStream("spark-data/identities.bin"));

        double nextHour = flights.get(0).getTs()[0] - (flights.get(0).getTs()[0] % CHUNK_INTERVAL);

        List<String> flightIdentities = new ArrayList<>();

        int flightOffset = 0;
        for (Flight flight : flights) {

            double startTs = flight.getTs()[0];

            if (nextHour < startTs) {
                timeOut.writeDouble(nextHour);
                timeOut.writeInt(flightOffset);
                nextHour = nextHour + CHUNK_INTERVAL;
            }

            flightsOut.writeInt(positionsOut.size());

            String identity = getIdentity(callSigns, flight.getIcao24(), flight.getTs()[0]);
            flightIdentities.add(identity);

            flightOffset++;

            for (double ts : flight.getTs()) {
                positionsOut.writeDouble(ts);
            }
            for (float lon : flight.getLon()) {
                positionsOut.writeFloat(lon);
            }
            for (float lat : flight.getLat()) {
                positionsOut.writeFloat(lat);
            }
            for (float alt : flight.getAlt()) {
                positionsOut.writeFloat(alt);
            }
        }

        identityOut.writeObject(flightIdentities.toArray(new String[flightIdentities.size()]));

        Map<String, Airline> airlineMap = new HashMap<>();

        for (String identity : flightIdentities) {
            if (identity == null || identity.length() < 6 || identity.length() > 7) {
                continue;
            }
            String icao = identity.substring(0, 3);
            String airlineName = airlineService.getAirlineName(icao);

            if (airlineName != null) {
                if (!airlineMap.containsKey(icao)) {
                    airlineMap.put(icao, new Airline(icao, airlineName));
                }
                airlineMap.get(icao).getFlightIdentities().add(identity);
            }
        }

        List<Airline> airlines = new ArrayList<>();
        airlines.addAll(airlineMap.values());
        Collections.sort(airlines, new Comparator<Airline>() {
            @Override
            public int compare(Airline o1, Airline o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        Writer writer = new FileWriter("spark-data/airlines.json");
        new Gson().toJson(airlines, writer);

        writer.flush();
        writer.close();

        identityOut.flush();
        identityOut.close();

        positionsOut.flush();
        positionsOut.close();

        flightsOut.flush();
        flightsOut.close();

        timeOut.flush();
        timeOut.close();

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + " milliseconds.");
    }

    private static String getIdentity(Map<String, List<CallSign>> callSigns, String icao24, double timestamp) {
        List<CallSign> identities = callSigns.get(icao24);

        if (identities == null) {
            return null;
        }

        for (CallSign callSign : identities) {
            if (callSign.getTimestamp() < timestamp) {
                return callSign.getIdentity();
            }
        }

        return null;
    }
}

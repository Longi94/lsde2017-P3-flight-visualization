package in.dragonbra;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import in.dragonbra.gson.DoubleTypeAdapter;
import in.dragonbra.model.*;
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
public class CsvToJson {

    private static final Pattern FILE_PATTERN = Pattern.compile("part-[0-9]{5}");

    private static final String POSITIONS_INPUT_PATH = "spark-data/split-positions";
    private static final String CALL_SIGNS_INPUT_PATH = "spark-data/call-signs";

    private static final double CHUNK_INTERVAL = 14400;
    private static final double START_TS = 1474156800;

    public static void main(String[] args) throws IOException {

        long start = System.currentTimeMillis();

        // This is the service that will resolve airline names
        AirlineService airlineService = new AirlineService(args[0]);

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

        List<String> flightIdentities = new ArrayList<>();

        // temporary arrays for the flight
        List<Flight> currentFlights = new ArrayList<>();
        List<Flight> nextFlights = new ArrayList<>();
        List<Flight> temp;

        // chunks are identified by timestamp
        double currentChunk = flights.get(0).getTs().get(0) - flights.get(0).getTs().get(0) % CHUNK_INTERVAL;

        Iterator<Flight> iterator = flights.iterator();
        Flight flight;

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Double.class, new DoubleTypeAdapter()).create();

        while (iterator.hasNext()) {
            flight = iterator.next();

            if (flight.getTs().size() < 2) {
                // pointless flight objects
                iterator.remove();
                continue;
            }

            if (currentChunk + CHUNK_INTERVAL <= flight.getTs().get(0)) {

                // finished with one chunk, write the json
                try (Writer writer = new FileWriter("spark-data/flights/" + String.format("%.0f", currentChunk) + ".json")) {
                    gson.toJson(currentFlights, writer);
                    writer.flush();

                    System.out.println("Number of flights in chunk " + String.format("%.0f", currentChunk) + "  " + currentFlights.size());
                }

                currentChunk += CHUNK_INTERVAL;

                // If need be, further split the flights
                temp = nextFlights;
                currentFlights.clear();
                nextFlights = new ArrayList<>();

                for (Flight tempFlight : temp) {
                    splitFlight(tempFlight, currentFlights, nextFlights, currentChunk);
                }

                temp.clear();
            }

            // get the identity of the flight
            String identity = getIdentity(callSigns, flight.getIcao24(), flight.getTs().get(0));
            flightIdentities.add(identity);
            flight.setIdentity(identity);

            splitFlight(flight, currentFlights, nextFlights, currentChunk);

            iterator.remove();
        }

        // write the last json too
        try (Writer writer = new FileWriter("spark-data/flights/" + String.format("%.0f", currentChunk) + ".json")) {
            gson.toJson(currentFlights, writer);
            writer.flush();

            System.out.println("Number of flights in chunk " + String.format("%.0f", currentChunk) + "  " + currentFlights.size());
        }

        // start resolving airline names and create a json with the airlines and flight identities
        Map<String, Airline> airlineMap = new HashMap<>();
        int nullCount = 0;

        for (String identity : flightIdentities) {
            if (identity == null || identity.length() < 6 || identity.length() > 7) {
                nullCount++;
                continue;
            }
            String icao = identity.substring(0, 3);
            String airlineName = airlineService.getAirlineName(icao);

            if (airlineName != null) {
                if (!airlineMap.containsKey(icao)) {
                    airlineMap.put(icao, new Airline(icao, airlineName));
                }
                airlineMap.get(icao).getFlightIdentities().add(identity);
            } else {
                nullCount++;
            }
        }

        for (Map.Entry<String, Airline> entry : airlineMap.entrySet()){
            System.out.println(entry.getKey() + "\t" + entry.getValue().getName() + "\t" + entry.getValue().getFlightIdentities().size());
        }
        System.out.println("Unidentified flights " + nullCount);

        List<Airline> airlines = new ArrayList<>();
        airlines.addAll(airlineMap.values());
        Collections.sort(airlines, new Comparator<Airline>() {
            @Override
            public int compare(Airline o1, Airline o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        try (Writer writer = new FileWriter("spark-data/airlines.json")) {
            new Gson().toJson(airlines, writer);
        }

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + " milliseconds.");
    }

    private static void splitFlight(Flight flight, List<Flight> current, List<Flight> next, double currentChunk) {
        double nextId = currentChunk + CHUNK_INTERVAL;
        if (flight.getTs().get(flight.getTs().size() - 1) > nextId) {
            // flight overlaps with other chunk, split it
            SplitFlight sf = new SplitFlight(flight, nextId);
            current.add(sf.getF1());
            next.add(sf.getF2());
        } else {
            current.add(flight);
        }
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

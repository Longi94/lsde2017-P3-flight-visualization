package in.dragonbra;

import in.dragonbra.model.Flight;
import in.dragonbra.model.PlanePosition;
import in.dragonbra.util.SparkUtils;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author lngtr
 * @since 2017-10-12
 */
public class CsvToBinary {

    private static final String INPUT_PATH = "spark-data/split-positions";

    private static final double CHUNK_INTERVAL = 14400;

    public static void main(String[] args) throws IOException {

        long start = System.currentTimeMillis();

        SparkSession spark = SparkSession
                .builder()
                .appName(PositionExtractor.class.getSimpleName())
                .getOrCreate();

        // Creates a DataFrame from a specified file
        Dataset<Row> df = spark.read().csv(INPUT_PATH);

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

        DataOutputStream positionsOut = new DataOutputStream(new FileOutputStream("positions.bin"));
        DataOutputStream flightsOut = new DataOutputStream(new FileOutputStream("flights.bin"));
        DataOutputStream timeOut = new DataOutputStream(new FileOutputStream("time-index.bin"));

        double nextHour = flights.get(0).getTs()[0] - (flights.get(0).getTs()[0] % CHUNK_INTERVAL);

        int flightOffset = 0;
        for (Flight flight : flights) {

            double startTs = flight.getTs()[0];

            if (nextHour < startTs) {
                timeOut.writeDouble(nextHour);
                timeOut.writeInt(flightOffset);
                nextHour = nextHour + CHUNK_INTERVAL;
            }

            flightsOut.writeInt(positionsOut.size());
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

        positionsOut.flush();
        positionsOut.close();

        flightsOut.flush();
        flightsOut.close();

        timeOut.flush();
        timeOut.close();

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + " milliseconds.");
    }
}

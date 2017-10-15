package in.dragonbra;

import com.google.common.collect.Lists;
import in.dragonbra.model.PlanePosition;
import in.dragonbra.util.MathUtils;
import in.dragonbra.util.SparkUtils;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author lngtr
 * @since 2017-10-02
 */
public class RDPReducer {

    private static final String INPUT_PATH = "realistic-positions";
    private static final String OUTPUT_PATH = "reduced-positions";
    private static final double EPSILON = 300.0;

    public static void main(String[] args) {

        if (new File(OUTPUT_PATH).exists()) {
            System.out.println(OUTPUT_PATH + " already exists.");
            System.exit(1);
        }

        long start = System.currentTimeMillis();

        SparkSession spark = SparkSession
                .builder()
                .appName(PositionExtractor.class.getSimpleName())
                .getOrCreate();

        // Creates a DataFrame from a specified file
        Dataset<Row> df = spark.read().csv(INPUT_PATH);

        // map the csv to java objects
        JavaRDD<PlanePosition> positions = SparkUtils.readCsv(df);

        JavaRDD<String> reducedPositions = positions
                // group the positions by icao24
                .groupBy(new Function<PlanePosition, String>() {
                    @Override
                    public String call(PlanePosition position) throws Exception {
                        return position.getIcao24();
                    }
                })

                // apply the Ramer-Douglas-Peucker algorithm
                .mapValues(new Function<Iterable<PlanePosition>, List<PlanePosition>>() {
                    @Override
                    public List<PlanePosition> call(Iterable<PlanePosition> positionsIter) throws Exception {
                        return DouglasPeucker(Lists.newArrayList(positionsIter), EPSILON);
                    }
                })

                // map back to csv
                .flatMap(new FlatMapFunction<Tuple2<String, List<PlanePosition>>, String>() {
                    @Override
                    public Iterator<String> call(Tuple2<String, List<PlanePosition>> t) throws Exception {
                        List<String> positions = new LinkedList<>();

                        for (PlanePosition position : t._2) {
                            positions.add(position.toString());
                        }

                        return positions.iterator();
                    }
                });

        reducedPositions.saveAsTextFile(OUTPUT_PATH);

        spark.stop();

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + " milliseconds.");
    }

    public static List<PlanePosition> DouglasPeucker(List<PlanePosition> positions, double epsilon) {
        // Find the point with the maximum distance
        double dmax = 0;
        int index = 0;
        int end = positions.size() - 1;

        for (int i = 1; i < end; i++) {
            double d = MathUtils.distanceFromLine(
                    positions.get(i).getSphericalCoordinates().getCartesian(),
                    positions.get(0).getSphericalCoordinates().getCartesian(),
                    positions.get(end).getSphericalCoordinates().getCartesian()
            );
            if (d > dmax) {
                index = i;
                dmax = d;
            }
        }

        List<PlanePosition> result = new ArrayList<>();

        // If max distance is greater than epsilon, recursively simplify
        if (dmax > epsilon) {
            // Recursive call
            List<PlanePosition> recResults1 = DouglasPeucker(positions.subList(0, index + 1), epsilon);
            List<PlanePosition> recResults2 = DouglasPeucker(positions.subList(index, end + 1), epsilon);

            // Build the result list
            recResults1.remove(recResults1.size() - 1); // this is a duplicate
            result.addAll(recResults1);
            result.addAll(recResults2);
        } else {
            result.add(positions.get(0));
            result.add(positions.get(end));
        }
        // Return the result
        return result;
    }
}

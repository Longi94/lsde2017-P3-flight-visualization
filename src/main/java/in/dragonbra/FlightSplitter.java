package in.dragonbra;

import com.google.common.collect.Lists;
import in.dragonbra.model.PlanePosition;
import in.dragonbra.util.SparkUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static in.dragonbra.util.MathUtils.getAngle;

/**
 * @author lngtr
 * @since 2017-10-10
 */
public class FlightSplitter {

    private static final String INPUT_PATH = "reduced-positions";
    private static final String OUTPUT_PATH = "split-positions";

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

                // sort by timestamp (again...)
                .sortBy(new Function<PlanePosition, Double>() {
                    @Override
                    public Double call(PlanePosition position) throws Exception {
                        return position.getTimestamp();
                    }
                }, true, positions.getNumPartitions())

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
                        return identifyCuts(Lists.newArrayList(positionsIter));
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

    private static List<PlanePosition> identifyCuts(List<PlanePosition> positions) {
        for (int i = 1; i < positions.size() - 2; i++) {
            if (isEnd(positions, i)) {
                positions.get(i).setEnd(true);
            }
        }

        return positions;
    }

    private static boolean isEnd(List<PlanePosition> positions, int i) {
        boolean timeGap = positions.get(i + 1).getTimestamp() - positions.get(i).getTimestamp() > 1000;

        if (!timeGap) return false;

        if (!positions.get(i).isAirborne()) return true;

        Vector3D A = positions.get(i - 1).getSphericalCoordinates().getCartesian();
        Vector3D B = positions.get(i).getSphericalCoordinates().getCartesian();
        Vector3D C = positions.get(i + 1).getSphericalCoordinates().getCartesian();
        Vector3D D = positions.get(i + 2).getSphericalCoordinates().getCartesian();

        return getAngle(A, B, C) < Math.PI * 3 / 4 || getAngle(B, C, D) < Math.PI * 3 / 4;
    }

}

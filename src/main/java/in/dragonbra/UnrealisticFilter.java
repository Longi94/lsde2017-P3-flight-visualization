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

/**
 * @author lngtr
 * @since 2017-10-05
 */
public class UnrealisticFilter {

    private static final String INPUT_PATH = "positions";
    private static final String OUTPUT_PATH = "realistic-positions";

    private static final double MAX_SPEED = 10000;

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

                // throw out unrealistic positions
                .flatMap(new FlatMapFunction<Tuple2<String, Iterable<PlanePosition>>, String>() {
                    @Override
                    public Iterator<String> call(Tuple2<String, Iterable<PlanePosition>> t) throws Exception {
                        List<PlanePosition> positions = Lists.newArrayList(t._2);
                        List<String> newPositions = new LinkedList<>();

                        Iterator<PlanePosition> iterator = positions.iterator();
                        PlanePosition previousPos = iterator.next();
                        while (iterator.hasNext()) {
                            PlanePosition currentPos = iterator.next();

                            if (isTeleport(previousPos, currentPos)) {
                                iterator.remove();
                            } else {
                                previousPos = currentPos;
                            }
                        }

                        for (PlanePosition position : positions) {
                            newPositions.add(position.toString());
                        }

                        return newPositions.iterator();
                    }
                });

        reducedPositions.saveAsTextFile(OUTPUT_PATH);

        spark.stop();

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + " milliseconds.");
    }

    private static boolean isTeleport(PlanePosition start, PlanePosition end) {
        Vector3D v1 = start.getSphericalCoordinates().getCartesian();
        Vector3D v2 = end.getSphericalCoordinates().getCartesian();

        double d = v1.distance(v2);
        double t = end.getTimestamp() - start.getTimestamp();

        return d / t > MAX_SPEED;
    }
}

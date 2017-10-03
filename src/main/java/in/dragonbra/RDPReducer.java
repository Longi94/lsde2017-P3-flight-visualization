package in.dragonbra;

import com.google.common.collect.Lists;
import in.dragonbra.model.PlanePosition;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
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

    private static final String INPUT_PATH = "positions";
    private static final String OUTPUT_PATH = "reduced-positions";

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Usage: [epsilon]");
            System.exit(1);
        }

        if (new File(OUTPUT_PATH).exists()) {
            System.out.println(OUTPUT_PATH + " already exists.");
            System.exit(1);
        }

        long start = System.currentTimeMillis();

        final double epsilon = Double.parseDouble(args[0]);

        SparkSession spark = SparkSession
                .builder()
                .appName(PositionExtractor.class.getSimpleName())
                .getOrCreate();

        // Creates a DataFrame from a specified file
        Dataset<Row> df = spark.read().csv(INPUT_PATH);

        JavaRDD<PlanePosition> positions = df.map(new MapFunction<Row, PlanePosition>() {
            @Override
            public PlanePosition call(Row row) throws Exception {
                String x = row.getString(3);
                String y = row.getString(4);
                String z = row.getString(5);
                String timestamp = row.getString(2);
                String isAirborne = row.getString(1);
                String icao24 = row.getString(0);
                //System.out.println(icao24 + " " + timestamp);
                return new PlanePosition(
                        new Vector3D(Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z)),
                        Double.parseDouble(timestamp),
                        icao24,
                        "1".equals(isAirborne)
                );
            }
        }, Encoders.kryo(PlanePosition.class)).javaRDD();

        JavaRDD<String> reducedPositions = positions
                .groupBy(new Function<PlanePosition, String>() {
                    @Override
                    public String call(PlanePosition position) throws Exception {
                        return position.getIcao24();
                    }
                })
                .mapValues(new Function<Iterable<PlanePosition>, List<PlanePosition>>() {
                    @Override
                    public List<PlanePosition> call(Iterable<PlanePosition> positionsIter) throws Exception {
                        return DouglasPeucker(Lists.newArrayList(positionsIter), epsilon);
                    }
                })
                .flatMap(new FlatMapFunction<Tuple2<String, List<PlanePosition>>, String>() {
                    @Override
                    public Iterator<String> call(Tuple2<String, List<PlanePosition>> t) throws Exception {
                        List<String> positions = new LinkedList<>();

                        for (PlanePosition position : t._2) {
                            positions.add(StringUtils.join(new Object[]{
                                            t._1,
                                            position.isAirborne() ? "1" : "0",
                                            position.getTimestamp(),
                                            position.getPos().getX(),
                                            position.getPos().getY(),
                                            position.getPos().getZ()
                                    },
                                    ','
                            ));
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
            double d = distanceFromLine(positions.get(i).getPos(), positions.get(0).getPos(), positions.get(end).getPos());
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
            recResults1.remove(recResults1.size() - 1);
            result.addAll(recResults1);
            result.addAll(recResults2);
        } else {
            result.add(positions.get(0));
            result.add(positions.get(end));
        }
        // Return the result
        return result;
    }

    public static double distanceFromLine(Vector3D P, Vector3D L1, Vector3D L2) {
        return abs(P.subtract(L1).crossProduct(P.subtract(L2))) /
                abs(L2.subtract(L1));
    }

    private static double abs(Vector3D v) {
        return v.distance(Vector3D.ZERO);
    }
}

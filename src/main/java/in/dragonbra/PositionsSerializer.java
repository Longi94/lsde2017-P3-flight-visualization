package in.dragonbra;

import in.dragonbra.model.RawPosition;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author lngtr
 * @since 2017-10-11
 */
public class PositionsSerializer {

    private static final String INPUT_PATH = "split-positions";

    private static final Pattern FILE_PATTERN = Pattern.compile("part-[0-9]{5}");


    public static void main(String[] args) throws IOException {

        String currentIcao24 = null;
        List<RawPosition> rawPositions = new ArrayList<>();
        Map<String, RawPosition[]> planes = new HashMap<>();

        Collection<File> files = FileUtils.listFiles(new File(INPUT_PATH), new IOFileFilter() {
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
                    String[] split = line.split(",");

                    String icao = split[0];
                    boolean isAirborne = "1".equals(split[1]);
                    double timestamp = Double.parseDouble(split[2]);
                    double longitude = Double.parseDouble(split[3]);
                    double latitude = Double.parseDouble(split[4]);
                    double altitude = Double.parseDouble(split[5]);
                    boolean isEnd = "1".equals(split[6]);

                    if (!icao.equals(currentIcao24)) {

                        if (currentIcao24 != null) {
                            planes.put(currentIcao24, rawPositions.toArray(new RawPosition[rawPositions.size()]));
                        }

                        rawPositions.clear();
                        currentIcao24 = icao;
                    }

                    rawPositions.add(new RawPosition(
                            timestamp, longitude, latitude, altitude, isAirborne, isEnd
                    ));
                }
            }
        }

        FileOutputStream fileOut = new FileOutputStream("flights.ser");
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(planes);
        out.close();
        fileOut.close();
    }
}

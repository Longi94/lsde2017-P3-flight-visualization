package in.dragonbra.util;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import static org.apache.commons.math3.util.FastMath.cos;
import static org.apache.commons.math3.util.FastMath.sin;

/**
 * @author lngtr
 * @since 2017-10-07
 */
public class MathUtils {

    // http://answers.google.com/answers/threadview?id=326655
    public static Vector3D polarToCartesian(double lon, double lat, double alt) {
        return new Vector3D(
                alt * cos(lat) * sin(lon),
                alt * sin(lat),
                alt * cos(lat) * cos(lon)
        );
    }
}

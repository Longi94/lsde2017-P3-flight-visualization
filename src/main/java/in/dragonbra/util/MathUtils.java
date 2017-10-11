package in.dragonbra.util;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;

/**
 * @author lngtr
 * @since 2017-10-11
 */
public class MathUtils {
    public static double getAngle(Vector3D A, Vector3D B, Vector3D C) {
        return FastMath.acos(A.subtract(B).dotProduct(C.subtract(B)) / (abs(A.subtract(B)) * abs(C.subtract(B))));
    }

    public static double distanceFromLine(Vector3D P, Vector3D L1, Vector3D L2) {
        // based on http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html
        return abs(P.subtract(L1).crossProduct(P.subtract(L2))) /
                abs(L2.subtract(L1));
    }

    public static double abs(Vector3D v) {
        // why is there no abs method in Vector3D smh
        return v.distance(Vector3D.ZERO);
    }
}

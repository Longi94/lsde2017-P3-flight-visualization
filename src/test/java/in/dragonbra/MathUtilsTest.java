package in.dragonbra;

import in.dragonbra.model.PlanePosition;
import in.dragonbra.util.MathUtils;
import org.apache.commons.math3.geometry.euclidean.threed.SphericalCoordinates;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author lngtr
 * @since 2017-10-03
 */
public class MathUtilsTest {
    @Test
    public void douglasPeucker() throws Exception {
        List<PlanePosition> positions = new ArrayList<>();

        positions.add(new PlanePosition(new SphericalCoordinates(new Vector3D(0, 0, 1)), 0, null, true));
        positions.add(new PlanePosition(new SphericalCoordinates(new Vector3D(1, 0, 1)), 0, null, true));
        positions.add(new PlanePosition(new SphericalCoordinates(new Vector3D(1.5, 0, 1)), 0, null, true));
        positions.add(new PlanePosition(new SphericalCoordinates(new Vector3D(2, 0, 1)), 0, null, true));
        positions.add(new PlanePosition(new SphericalCoordinates(new Vector3D(2, 1, 1)), 0, null, true));
        positions.add(new PlanePosition(new SphericalCoordinates(new Vector3D(2, 2, 1)), 0, null, true));

        List<PlanePosition> newPositions = RDPReducer.DouglasPeucker(positions, 0.001);

        assertEquals(3, newPositions.size());
        assertEquals(positions.get(0), newPositions.get(0));
        assertEquals(positions.get(3), newPositions.get(1));
        assertEquals(positions.get(5), newPositions.get(2));

        positions.clear();

        positions.add(new PlanePosition(new SphericalCoordinates(new Vector3D(0, 0, 1)), 0, null, true));
        positions.add(new PlanePosition(new SphericalCoordinates(new Vector3D(1, 0.001, 1)), 0, null, true));
        positions.add(new PlanePosition(new SphericalCoordinates(new Vector3D(1.5, 0.001, 1)), 0, null, true));
        positions.add(new PlanePosition(new SphericalCoordinates(new Vector3D(2, 0, 1)), 0, null, true));
        positions.add(new PlanePosition(new SphericalCoordinates(new Vector3D(2, 1.001, 1)), 0, null, true));
        positions.add(new PlanePosition(new SphericalCoordinates(new Vector3D(2, 2, 1)), 0, null, true));

        newPositions = RDPReducer.DouglasPeucker(positions, 0.01);

        assertEquals(3, newPositions.size());
        assertEquals(positions.get(0), newPositions.get(0));
        assertEquals(positions.get(3), newPositions.get(1));
        assertEquals(positions.get(5), newPositions.get(2));
    }

    @Test
    public void distanceFromLine() throws Exception {
        assertEquals(1.0, MathUtils.distanceFromLine(
                new Vector3D(0, 1, 0 ),
                new Vector3D(1, 0, 0 ),
                new Vector3D(0, 0, 0 )
        ), 0.0);
        assertEquals(1.0, MathUtils.distanceFromLine(
                new Vector3D(100, 1, 0 ),
                new Vector3D(1, 0, 0 ),
                new Vector3D(0, 0, 0 )
        ), 0.0);
        assertEquals(Math.sqrt(2.0), MathUtils.distanceFromLine(
                new Vector3D(0, 1, 1 ),
                new Vector3D(1, 0, 0 ),
                new Vector3D(0, 0, 0 )
        ), 0.000001);
        assertEquals(3399.4563388899414, MathUtils.distanceFromLine(
                new Vector3D(4124, 4234, 432 ),
                new Vector3D(123, 763, 51 ),
                new Vector3D(0, 0, 0 )
        ), 0.000001);
    }

    @Test
    public void getAngle() throws Exception {
        Vector3D A = new Vector3D(0,0,0);
        Vector3D B = new Vector3D(0,1,0);
        Vector3D C = new Vector3D(0,1,1);

        Assert.assertEquals(Math.PI / 2, MathUtils.getAngle(A, B, C), 0.001);


        A = new Vector3D(234,543,425);
        B = new Vector3D(423,286,234);
        C = new Vector3D(234,2342,25);

        Assert.assertEquals(0.8239194474, MathUtils.getAngle(A, B, C), 0.001);
    }
}
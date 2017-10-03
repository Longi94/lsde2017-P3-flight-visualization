package in.dragonbra;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author lngtr
 * @since 2017-10-03
 */
public class RDPReducerTest {
    @Test
    public void douglasPeucker() throws Exception {
    }

    @Test
    public void distanceFromLine() throws Exception {
        assertEquals(1.0, RDPReducer.distanceFromLine(
                new Vector3D(0, 1, 0 ),
                new Vector3D(1, 0, 0 ),
                new Vector3D(0, 0, 0 )
        ), 0.0);
        assertEquals(1.0, RDPReducer.distanceFromLine(
                new Vector3D(100, 1, 0 ),
                new Vector3D(1, 0, 0 ),
                new Vector3D(0, 0, 0 )
        ), 0.0);
        assertEquals(Math.sqrt(2.0), RDPReducer.distanceFromLine(
                new Vector3D(0, 1, 1 ),
                new Vector3D(1, 0, 0 ),
                new Vector3D(0, 0, 0 )
        ), 0.000001);
        assertEquals(3399.4563388899414, RDPReducer.distanceFromLine(
                new Vector3D(4124, 4234, 432 ),
                new Vector3D(123, 763, 51 ),
                new Vector3D(0, 0, 0 )
        ), 0.000001);
    }

}
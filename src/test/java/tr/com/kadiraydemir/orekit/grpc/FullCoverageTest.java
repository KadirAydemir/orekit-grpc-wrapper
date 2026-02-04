package tr.com.kadiraydemir.orekit.grpc;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import tr.com.kadiraydemir.orekit.service.frame.FrameService;
import org.eclipse.microprofile.health.HealthCheckResponse;

import org.eclipse.microprofile.health.Liveness;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.FramesFactory;
import tr.com.kadiraydemir.orekit.service.propagation.IntegratorService;
import tr.com.kadiraydemir.orekit.service.visibility.VisibilityService;

@QuarkusTest
public class FullCoverageTest {

    @Inject
    FrameService frameService;

    @Inject
    @Liveness
    MyLivenessCheck livenessCheck;

    @Test
    public void testFrameService() {
        // Test null
        Frame frame = frameService.resolveFrame(null);
        Assertions.assertEquals("TEME", frame.getName());

        // Test all frames
        Assertions.assertEquals("TEME", frameService.resolveFrame(ReferenceFrame.TEME).getName());
        Assertions.assertEquals("GCRF", frameService.resolveFrame(ReferenceFrame.GCRF).getName());
        Assertions.assertEquals("EME2000", frameService.resolveFrame(ReferenceFrame.EME2000).getName());
        Assertions.assertTrue(frameService.resolveFrame(ReferenceFrame.ITRF).getName().contains("ITRF"));

        // Test getTemeFrame
        Assertions.assertEquals("TEME", frameService.getTemeFrame().getName());

        // Test createTopocentricFrame
        TopocentricFrame topo = frameService.createTopocentricFrame(39.9, 32.8, 1000.0, "TestStation");
        Assertions.assertEquals("TestStation", topo.getName());
    }

    @Test
    public void testLiveness() {
        HealthCheckResponse response = livenessCheck.call();
        Assertions.assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        Assertions.assertEquals("alive", response.getName());
    }

    @Test
    public void testFrameServiceEdgeCases() {
        // Test unknown frame (default)
        Assertions.assertEquals("TEME", frameService.resolveFrame(ReferenceFrame.UNRECOGNIZED).getName());
    }

    @Inject
    IntegratorService integratorService;

    @Test
    public void testIntegratorServiceEdgeCases() {
        Orbit dummyOrbit = new CartesianOrbit(
                new PVCoordinates(new Vector3D(7000000, 0, 0), new Vector3D(0, 7500, 0)),
                FramesFactory.getGCRF(),
                AbsoluteDate.J2000_EPOCH,
                Constants.WGS84_EARTH_MU);
        // Test null
        Assertions.assertNotNull(integratorService.createIntegrator(null, dummyOrbit));
        // Test unrecognized
        Assertions.assertNotNull(integratorService.createIntegrator(IntegratorType.UNRECOGNIZED, dummyOrbit));
    }

    @Inject
    VisibilityService visibilityService;

    @Test
    public void testVisibilityServiceInitialVisibility() {
        GroundStation station = GroundStation.newBuilder()
                .setName("Test")
                .setLatitudeDegrees(0)
                .setLongitudeDegrees(0)
                .setAltitudeMeters(0)
                .build();

        // A TLE that is definitely NOT visible initially (e.g. on the other side of
        // Earth)
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  180.0000 0005000  0.0000  50.0000 15.50000000 10005";

        AccessIntervalsRequest request = AccessIntervalsRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setGroundStation(station)
                .setStartDateIso("2024-01-01T00:00:00Z")
                .setEndDateIso("2024-01-01T00:10:00Z") // 10 mins
                .setMinElevationDegrees(10.0)
                .build();

        var response = visibilityService.getAccessIntervals(request);
        Assertions.assertNotNull(response);
    }
}

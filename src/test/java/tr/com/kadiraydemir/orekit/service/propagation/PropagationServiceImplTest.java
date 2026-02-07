package tr.com.kadiraydemir.orekit.service.propagation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.PropagateRequest;
import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;
import tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest;
import tr.com.kadiraydemir.orekit.mapper.PropagationTestMapper;
import tr.com.kadiraydemir.orekit.model.OrbitResult;
import tr.com.kadiraydemir.orekit.model.TleResult;

import java.time.Duration;
import java.util.List;

@QuarkusTest
public class PropagationServiceImplTest {

    @Inject
    PropagationService propagationService;

    @Inject
    PropagationTestMapper propagationTestMapper;

    @Test
    public void testPropagate() {
        PropagateRequest request = PropagateRequest.newBuilder()
                .setSatelliteName("TestSat")
                .setSemimajorAxis(7000000.0)
                .setEccentricity(0.001)
                .setInclination(Math.toRadians(45.0))
                .setPerigeeArgument(0)
                .setRightAscensionOfAscendingNode(0)
                .setMeanAnomaly(0)
                .setEpochIso("2024-01-01T12:00:00Z")
                .setDuration(3600.0)
                .build();

        OrbitResult result = propagationService.propagate(propagationTestMapper.toDTO(request));

        Assertions.assertNotNull(result);
        Assertions.assertEquals("TestSat", result.satelliteName());
        Assertions.assertNotEquals(0.0, result.posX());
    }

    @Test
    public void testPropagateInvalidDate() {
        PropagateRequest request = PropagateRequest.newBuilder()
                .setEpochIso("invalid-date")
                .build();

        Assertions.assertThrows(RuntimeException.class, () -> propagationService.propagate(propagationTestMapper.toDTO(request)));
    }

    @Test
    public void testPropagateTLE() {
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDate("2024-01-01T12:00:00Z")
                .setEndDate("2024-01-01T13:00:00Z")
                .setPositionCount(10)
                .setOutputFrame(ReferenceFrame.TEME)
                .build();

        List<TleResult> results = propagationService.propagateTLE(propagationTestMapper.toDTO(request))
                .collect().asList().await().atMost(Duration.ofSeconds(5));

        Assertions.assertNotNull(results);
        Assertions.assertFalse(results.isEmpty());
    }

    @Test
    public void testPropagateTLEInvalid() {
        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setTleLine1("invalid")
                .build();

        Assertions.assertThrows(Exception.class, () -> propagationService.propagateTLE(propagationTestMapper.toDTO(request))
                .collect().asList().await().atMost(Duration.ofSeconds(5)));
    }

    @Test
    public void testPropagateTLEHighVolume() {
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDate("2024-01-01T12:00:00Z")
                .setEndDate("2024-01-02T12:00:00Z") // 24 hours
                .setPositionCount(1000)
                .setOutputFrame(ReferenceFrame.TEME)
                .build();

        List<TleResult> results = propagationService.propagateTLE(propagationTestMapper.toDTO(request))
                .collect().asList().await().atMost(Duration.ofSeconds(10));

        Assertions.assertNotNull(results);
        int totalPoints = results.stream().mapToInt(r -> r.positions().size()).sum();
        Assertions.assertEquals(1000, totalPoints);
    }

    @Test
    public void testPropagateTLE_ZeroPositionCount() {
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDate("2024-01-01T12:00:00Z")
                .setEndDate("2024-01-01T13:00:00Z")
                .setPositionCount(0)
                .setOutputFrame(ReferenceFrame.TEME)
                .build();

        List<TleResult> results = propagationService.propagateTLE(propagationTestMapper.toDTO(request))
                .collect().asList().await().atMost(Duration.ofSeconds(5));

        Assertions.assertNotNull(results);
        // Expecting one TleResult with empty positions list
        Assertions.assertEquals(1, results.size());
        Assertions.assertTrue(results.get(0).positions().isEmpty());
    }

    @Test
    public void testPropagateTLE_SinglePositionCount() {
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDate("2024-01-01T12:00:00Z")
                .setEndDate("2024-01-01T13:00:00Z")
                .setPositionCount(1)
                .setOutputFrame(ReferenceFrame.TEME)
                .build();

        List<TleResult> results = propagationService.propagateTLE(propagationTestMapper.toDTO(request))
                .collect().asList().await().atMost(Duration.ofSeconds(5));

        Assertions.assertNotNull(results);
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(1, results.get(0).positions().size());
        // Verify the single position is at start date (approximately)
        // Note: The service uses ISO strings, so exact match depends on formatting
        // But we just verify we got 1 point.
    }

    @Test
    public void testPropagateTLE_NegativePositionCount() {
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDate("2024-01-01T12:00:00Z")
                .setEndDate("2024-01-01T13:00:00Z")
                .setPositionCount(-1)
                .setOutputFrame(ReferenceFrame.TEME)
                .build();

        // Should throw exception because ArrayList constructor fails with negative capacity
        Assertions.assertThrows(Exception.class, () -> propagationService.propagateTLE(propagationTestMapper.toDTO(request))
                .collect().asList().await().atMost(Duration.ofSeconds(5)));
    }
}

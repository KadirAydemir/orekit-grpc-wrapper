package tr.com.kadiraydemir.orekit.service.propagation;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.PropagateRequest;
import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;
import tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest;
import tr.com.kadiraydemir.orekit.model.OrbitResult;
import tr.com.kadiraydemir.orekit.model.TleResult;

import java.util.List;

@QuarkusTest
public class PropagationServiceImplTest {

    @Inject
    PropagationServiceImpl propagationService;

    @Test
    public void testPropagateSuccess() {
        PropagateRequest request = PropagateRequest.newBuilder()
                .setSatelliteName("Test")
                .setSemimajorAxis(7000000.0)
                .setEccentricity(0.001)
                .setInclination(0.5)
                .setEpochIso("2024-01-01T12:00:00Z")
                .setDuration(3600.0)
                .build();

        OrbitResult result = propagationService.propagate(request);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Test", result.satelliteName());
        Assertions.assertNotEquals(0.0, result.posX());
    }

    @Test
    public void testPropagateFailure() {
        PropagateRequest request = PropagateRequest.newBuilder()
                .setEpochIso("INVALID-DATE") // Will cause parse error
                .build();

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            propagationService.propagate(request);
        });
        Assertions.assertTrue(thrown.getMessage().contains("Propagation failed"));
    }

    @Test
    public void testPropagateTLESuccess() {
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDate("2024-01-01T12:00:00Z")
                .setEndDate("2024-01-01T13:00:00Z")
                .setPositionCount(2)
                .setOutputFrame(ReferenceFrame.TEME)
                .build();

        Multi<TleResult> resultMulti = propagationService.propagateTLE(request);
        List<TleResult> results = resultMulti.collect().asList().await().indefinitely();

        // With batch size 100 and 2 positions, we expect 1 result containing 2 positions
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(2, results.get(0).positions().size());
    }

    @Test
    public void testPropagateTLEFailure() {
        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setTleLine1("BAD TLE")
                .build();

        Multi<TleResult> resultMulti = propagationService.propagateTLE(request);

        Assertions.assertThrows(RuntimeException.class, () -> {
            resultMulti.collect().asList().await().indefinitely();
        });
    }

    @Test
    public void testPropagateTLEBlockingSuccess() {
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDate("2024-01-01T12:00:00Z")
                .setEndDate("2024-01-01T13:00:00Z")
                .setPositionCount(1005) // Should produce 2 batches (1000 + 5)
                .setOutputFrame(ReferenceFrame.TEME)
                .build();

        List<TleResult> results = new java.util.ArrayList<>();
        propagationService.propagateTLEBlocking(request, results::add);

        Assertions.assertEquals(2, results.size());
        Assertions.assertEquals(1000, results.get(0).positions().size());
        Assertions.assertEquals(5, results.get(1).positions().size());
    }
}

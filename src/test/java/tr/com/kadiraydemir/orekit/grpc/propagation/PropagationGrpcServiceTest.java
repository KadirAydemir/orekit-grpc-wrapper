package tr.com.kadiraydemir.orekit.grpc.propagation;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.*;

import java.time.Duration;
import java.util.List;

@QuarkusTest
public class PropagationGrpcServiceTest {

        @GrpcClient("orbital-service-client")
        OrbitalService orbitalService;

        @Test
        public void testPropagateTLE() {
                String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
                String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

                TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                                .setTleLine1(line1)
                                .setTleLine2(line2)
                                .setStartDate("2024-01-01T12:00:00Z")
                                .setEndDate("2024-01-01T13:00:00Z") // 1 hour
                                .setPositionCount(10)
                                .setOutputFrame(ReferenceFrame.TEME)
                                .build();

                List<TLEPropagateResponse> responses = orbitalService.propagateTLE(request)
                                .collect().asList()
                                .await().atMost(Duration.ofSeconds(30));

                Assertions.assertNotNull(responses);
                Assertions.assertFalse(responses.isEmpty());

                // Sum of all positions in all batches should be 10
                int totalPositions = responses.stream()
                                .mapToInt(r -> r.getPositionsCount())
                                .sum();
                Assertions.assertEquals(10, totalPositions);
        }

        @Test
        public void testPropagation() {
                PropagateRequest request = PropagateRequest.newBuilder()
                                .setSatelliteName("TestSat")
                                .setSemimajorAxis(7000000.0)
                                .setEccentricity(0.001)
                                .setInclination(Math.toRadians(45.0))
                                .setPerigeeArgument(0)
                                .setRightAscensionOfAscendingNode(0)
                                .setMeanAnomaly(0)
                                .setEpochIso("2024-01-01T12:00:00Z")
                                .setDuration(3600.0) // 1 hour
                                .build();

                PropagateResponse response = orbitalService.propagate(request)
                                .await().atMost(Duration.ofSeconds(30));

                Assertions.assertNotNull(response);
                Assertions.assertEquals("TestSat", response.getSatelliteName());
                Assertions.assertNotEquals(0.0, response.getPosX());
                Assertions.assertNotNull(response.getFinalDateIso());
                System.out.println(
                                "Propagated Position: " + response.getPosX() + ", " + response.getPosY() + ", "
                                                + response.getPosZ());
        }
}

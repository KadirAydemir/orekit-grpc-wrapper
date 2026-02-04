package tr.com.kadiraydemir.orekit.grpc.propagation;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.*;

import java.time.Duration;

@QuarkusTest
public class PropagationGrpcServiceTest {

        @GrpcClient("orbital-service-client")
        OrbitalService orbitalService;

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
                                .await().atMost(Duration.ofSeconds(5));

                Assertions.assertNotNull(response);
                Assertions.assertEquals("TestSat", response.getSatelliteName());
                Assertions.assertNotEquals(0.0, response.getPosX());
                Assertions.assertNotNull(response.getFinalDateIso());
                System.out.println(
                                "Propagated Position: " + response.getPosX() + ", " + response.getPosY() + ", "
                                                + response.getPosZ());
        }

        @Test
        public void testTLEListPropagation() {
                TLE tle1 = TLE.newBuilder()
                                .setTleLine1("1 25544U 98067A   21150.84021272  .00000868  00000-0  23602-4 0  9999")
                                .setTleLine2("2 25544  51.6441 234.3414 0002621 161.9427 337.3343 15.48911283285741")
                                .build();

                TLE tle2 = TLE.newBuilder()
                                .setTleLine1("1 25544U 98067A   21150.84021272  .00000868  00000-0  23602-4 0  9999")
                                .setTleLine2("2 25544  51.6441 234.3414 0002621 161.9427 337.3343 15.48911283285741")
                                .build();

                TLEListPropagateRequest request = TLEListPropagateRequest.newBuilder()
                                .setModel(PropagationModel.SGP4)
                                .addTles(tle1)
                                .addTles(tle2)
                                .setStartDate("2021-05-30T20:10:00.000Z")
                                .setEndDate("2021-05-30T21:10:00.000Z")
                                .setPositionCount(10)
                                .setOutputFrame(ReferenceFrame.TEME)
                                .build();

                var results = orbitalService.propagateTLEList(request)
                                .collect().asList()
                                .await().atMost(Duration.ofSeconds(30));

                Assertions.assertFalse(results.isEmpty());
                boolean hasIndex0 = results.stream().anyMatch(r -> r.getTleIndex() == 0);
                boolean hasIndex1 = results.stream().anyMatch(r -> r.getTleIndex() == 1);
                Assertions.assertTrue(hasIndex0, "Should have results for TLE 0");
                Assertions.assertTrue(hasIndex1, "Should have results for TLE 1");
        }
}

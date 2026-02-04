package tr.com.kadiraydemir.orekit.grpc;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.*;

import java.time.Duration;

@QuarkusTest
public class OrbitalServiceTest {

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
}

package tr.com.kadiraydemir.orekit.grpc.propagation;

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
                                .await().atMost(Duration.ofSeconds(10));

                Assertions.assertNotNull(response);
                Assertions.assertEquals("TestSat", response.getSatelliteName());
                Assertions.assertNotEquals(0.0, response.getPosX());
                Assertions.assertNotNull(response.getFinalDateIso());
                System.out.println(
                                "Propagated Position: " + response.getPosX() + ", " + response.getPosY() + ", "
                                                + response.getPosZ());
        }

        @Test
        public void testPropagateTLE() {
                String line1 = "1 25544U 98067A   24035.59253472  .00016717  00000+0  30048-3 0  9993";
                String line2 = "2 25544  51.6406 205.2346 0005086 211.3916 148.6924 15.50020612437617";

                TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                                .setTleLine1(line1)
                                .setTleLine2(line2)
                                .setStartDate("2024-02-04T14:00:00Z")
                                .setEndDate("2024-02-04T14:10:00Z")
                                .setPositionCount(10)
                                .build();

                var responses = orbitalService.propagateTLE(request)
                                .collect().asList()
                                .await().atMost(Duration.ofSeconds(10));

                Assertions.assertFalse(responses.isEmpty());

                TLEPropagateResponse firstResponse = responses.get(0);
                Assertions.assertFalse(firstResponse.getPositionsList().isEmpty());

                PositionPoint firstPoint = firstResponse.getPositionsList().get(0);
                Assertions.assertNotNull(firstPoint.getTimestamp());
                Assertions.assertNotEquals(0.0, firstPoint.getX());
        }

        @Test
        public void testNumericalPropagation() {
                String line1 = "1 25544U 98067A   24035.59253472  .00016717  00000+0  30048-3 0  9993";
                String line2 = "2 25544  51.6406 205.2346 0005086 211.3916 148.6924 15.50020612437617";

                TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                                .setTleLine1(line1)
                                .setTleLine2(line2)
                                .setStartDate("2024-02-04T14:00:00Z")
                                .setEndDate("2024-02-04T14:10:00Z")
                                .setPositionCount(5)
                                .setModel(PropagationModel.NUMERICAL)
                                .setIntegrator(IntegratorType.DORMAND_PRINCE_853)
                                .setOutputFrame(ReferenceFrame.GCRF)
                                .build();

                var responses = orbitalService.propagateTLE(request)
                                .collect().asList()
                                .await().atMost(Duration.ofSeconds(20));

                Assertions.assertFalse(responses.isEmpty());
                Assertions.assertEquals("GCRF", responses.get(0).getFrame());
        }

        @Test
        public void testDifferentIntegrators() {
                String line1 = "1 25544U 98067A   24035.59253472  .00016717  00000+0  30048-3 0  9993";
                String line2 = "2 25544  51.6406 205.2346 0005086 211.3916 148.6924 15.50020612437617";

                IntegratorType[] types = {
                                IntegratorType.DORMAND_PRINCE_54,
                                IntegratorType.CLASSICAL_RUNGE_KUTTA,
                                IntegratorType.ADAMS_BASHFORTH,
                                IntegratorType.ADAMS_MOULTON,
                                IntegratorType.GRAGG_BULIRSCH_STOER
                };

                for (IntegratorType type : types) {
                        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                                        .setTleLine1(line1)
                                        .setTleLine2(line2)
                                        .setStartDate("2024-02-04T14:00:00Z")
                                        .setEndDate("2024-02-04T14:01:00Z")
                                        .setPositionCount(2)
                                        .setModel(PropagationModel.NUMERICAL)
                                        .setIntegrator(type)
                                        .build();

                        var responses = orbitalService.propagateTLE(request)
                                        .collect().asList()
                                        .await().atMost(Duration.ofSeconds(10));

                        Assertions.assertEquals(2, responses.size());
                }
        }
}

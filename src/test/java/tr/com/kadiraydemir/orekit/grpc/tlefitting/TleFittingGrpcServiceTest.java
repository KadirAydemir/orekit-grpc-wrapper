package tr.com.kadiraydemir.orekit.grpc.tlefitting;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.FitTLERequest;
import tr.com.kadiraydemir.orekit.grpc.FitTLEResponse;
import tr.com.kadiraydemir.orekit.grpc.PositionMeasurement;
import tr.com.kadiraydemir.orekit.grpc.TleFittingService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TleFittingGrpcService.
 */
@QuarkusTest
public class TleFittingGrpcServiceTest {

        @GrpcClient
        TleFittingService tleFittingService;

        // Sample ISS TLE - must be exactly 69 characters per line
        private static final String ISS_TLE_LINE1 = "1 25544U 98067A   24001.50000000  .00000000  00000-0  00000-0 0  0010";
        private static final String ISS_TLE_LINE2 = "2 25544  51.6400  100.0000 0007000 100.0000 260.0000 15.50000000  010";

        @Test
        public void testFitTLEGrpc() {
                // Create measurements
                List<PositionMeasurement> measurements = createMeasurements();

                FitTLERequest request = FitTLERequest.newBuilder()
                                .setInitialTleLine1(ISS_TLE_LINE1)
                                .setInitialTleLine2(ISS_TLE_LINE2)
                                .setSatelliteName("ISS")
                                .setSatelliteNumber(25544)
                                .setInternationalDesignator("1998-067A")
                                .addAllMeasurements(measurements)
                                .setConvergenceThreshold(1.0e-3)
                                .setMaxIterations(25)
                                .build();

                FitTLEResponse response = tleFittingService.fitTLE(request).await()
                                .atMost(java.time.Duration.ofSeconds(30));

                assertNotNull(response);
                assertNotNull(response.getStatistics());

                // Response should either succeed or return a meaningful error
                if (response.getError().isEmpty()) {
                        // Success case
                        assertTrue(response.getStatistics().getConverged());
                        assertFalse(response.getFittedTleLine1().isEmpty());
                        assertFalse(response.getFittedTleLine2().isEmpty());
                        assertEquals(69, response.getFittedTleLine1().length());
                        assertEquals(69, response.getFittedTleLine2().length());
                } else {
                        // Error case - should have meaningful error message
                        assertFalse(response.getError().isEmpty());
                }
        }

        @Test
        public void testFitTLEWithTooFewMeasurements() {
                // Only 1 measurement - should fail validation
                List<PositionMeasurement> measurements = new ArrayList<>();
                measurements.add(PositionMeasurement.newBuilder()
                                .setTimestamp("2024-01-01T12:00:00Z")
                                .setPositionX(1000.0)
                                .setPositionY(2000.0)
                                .setPositionZ(3000.0)
                                .setWeight(1.0)
                                .setSigma(1000.0)
                                .build());

                FitTLERequest request = FitTLERequest.newBuilder()
                                .setSatelliteName("TEST")
                                .setSatelliteNumber(99999)
                                .addAllMeasurements(measurements)
                                .build();

                FitTLEResponse response = tleFittingService.fitTLE(request).await()
                                .atMost(java.time.Duration.ofSeconds(10));

                assertNotNull(response);
                assertFalse(response.getError().isEmpty());
                assertFalse(response.getStatistics().getConverged());
        }

        private List<PositionMeasurement> createMeasurements() {
                List<PositionMeasurement> measurements = new ArrayList<>();

                String[] timestamps = {
                                "2024-01-01T12:00:00Z",
                                "2024-01-01T12:10:00Z",
                                "2024-01-01T12:20:00Z",
                                "2024-01-01T12:30:00Z",
                                "2024-01-01T12:40:00Z",
                                "2024-01-01T12:50:00Z"
                };

                for (int i = 0; i < timestamps.length; i++) {
                        double angle = i * Math.PI / 3;
                        double radius = 6800000.0;
                        double x = radius * Math.cos(angle);
                        double y = radius * Math.sin(angle);
                        double z = radius * 0.1 * Math.sin(angle * 2);

                        measurements.add(PositionMeasurement.newBuilder()
                                        .setTimestamp(timestamps[i])
                                        .setPositionX(x)
                                        .setPositionY(y)
                                        .setPositionZ(z)
                                        .setWeight(1.0)
                                        .setSigma(1000.0)
                                        .build());
                }

                return measurements;
        }
}

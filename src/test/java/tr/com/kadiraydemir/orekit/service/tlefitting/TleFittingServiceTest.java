package tr.com.kadiraydemir.orekit.service.tlefitting;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.model.PositionMeasurement;
import tr.com.kadiraydemir.orekit.model.TleFittingRequest;
import tr.com.kadiraydemir.orekit.model.TleFittingResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TleFittingService.
 */
@QuarkusTest
public class TleFittingServiceTest {

        @Inject
        TleFittingService tleFittingService;

        // Sample ISS TLE - must be exactly 69 characters per line
        private static final String ISS_TLE_LINE1 = "1 25544U 98067A   24001.50000000  .00000000  00000-0  00000-0 0  0010";
        private static final String ISS_TLE_LINE2 = "2 25544  51.6400  100.0000 0007000 100.0000 260.0000 15.50000000  010";

        @Test
        public void testFitTLEWithTooFewMeasurements() {
                // Only 1 measurement - should fail validation
                List<PositionMeasurement> measurements = new ArrayList<>();
                measurements.add(PositionMeasurement.of("2024-01-01T12:00:00Z", 1000.0, 2000.0, 3000.0));

                TleFittingRequest request = new TleFittingRequest(
                                ISS_TLE_LINE1,
                                ISS_TLE_LINE2,
                                "TEST",
                                25544,
                                "1998-067A",
                                measurements,
                                1.0e-3,
                                25,
                                tr.com.kadiraydemir.orekit.model.ReferenceFrameType.TEME);

                TleFittingResult result = tleFittingService.fitTLE(request);

                assertNotNull(result);
                assertNotNull(result.error());
                assertFalse(result.converged());
                assertTrue(result.error().contains("At least 2 measurements required"));
        }

        @Test
        public void testFitTLEServiceDoesNotCrash() {
                // Test that the service handles requests without crashing
                List<PositionMeasurement> measurements = createMeasurements();

                TleFittingRequest request = new TleFittingRequest(
                                ISS_TLE_LINE1,
                                ISS_TLE_LINE2,
                                "ISS",
                                25544,
                                "1998-067A",
                                measurements,
                                1.0e-3,
                                25,
                                tr.com.kadiraydemir.orekit.model.ReferenceFrameType.TEME);

                // Should not throw an exception - even if fitting doesn't converge
                TleFittingResult result = assertDoesNotThrow(() -> tleFittingService.fitTLE(request));
                assertNotNull(result);

                // Either we get a successful result or a meaningful error
                if (result.error() != null) {
                        // If there's an error, it should be informative
                        assertFalse(result.error().isEmpty());
                } else {
                        // If successful, we should have TLE lines
                        assertNotNull(result.fittedTleLine1());
                        assertNotNull(result.fittedTleLine2());
                        assertEquals(69, result.fittedTleLine1().length());
                        assertEquals(69, result.fittedTleLine2().length());
                }
        }

        @Test
        public void testFitTLEWithDefaultInitialTLE() {
                // Test with no initial TLE provided
                List<PositionMeasurement> measurements = createMeasurements();

                TleFittingRequest request = new TleFittingRequest(
                                null, // No initial line 1
                                null, // No initial line 2
                                "TEST_SAT",
                                99999,
                                "2024-001A",
                                measurements,
                                1.0e-3,
                                25,
                                tr.com.kadiraydemir.orekit.model.ReferenceFrameType.TEME);

                // Should not throw an exception
                TleFittingResult result = assertDoesNotThrow(() -> tleFittingService.fitTLE(request));
                assertNotNull(result);
        }

        /**
         * Creates position measurements for testing.
         */
        private List<PositionMeasurement> createMeasurements() {
                List<PositionMeasurement> measurements = new ArrayList<>();

                // Add measurements at different times
                String[] timestamps = {
                                "2024-01-01T12:00:00Z",
                                "2024-01-01T12:10:00Z",
                                "2024-01-01T12:20:00Z",
                                "2024-01-01T12:30:00Z",
                                "2024-01-01T12:40:00Z",
                                "2024-01-01T12:50:00Z"
                };

                // Create positions
                for (int i = 0; i < timestamps.length; i++) {
                        double angle = i * Math.PI / 3;
                        double radius = 6800000.0; // LEO altitude in meters
                        double x = radius * Math.cos(angle);
                        double y = radius * Math.sin(angle);
                        double z = radius * 0.1 * Math.sin(angle * 2);

                        measurements.add(PositionMeasurement.of(
                                        timestamps[i],
                                        x,
                                        y,
                                        z));
                }

                return measurements;
        }
}

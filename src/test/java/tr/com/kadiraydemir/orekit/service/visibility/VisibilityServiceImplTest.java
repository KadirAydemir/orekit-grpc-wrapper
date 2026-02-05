package tr.com.kadiraydemir.orekit.service.visibility;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.AccessIntervalsRequest;
import tr.com.kadiraydemir.orekit.grpc.GroundStation;
import tr.com.kadiraydemir.orekit.mapper.VisibilityTestMapper;
import tr.com.kadiraydemir.orekit.model.VisibilityResult;

@QuarkusTest
public class VisibilityServiceImplTest {

    @Inject
    VisibilityService visibilityService;

    @Inject
    VisibilityTestMapper visibilityTestMapper;

    @Test
    public void testGetAccessIntervals() {
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        GroundStation station = GroundStation.newBuilder()
                .setName("Ankara")
                .setLatitudeDegrees(39.9334)
                .setLongitudeDegrees(32.8597)
                .setAltitudeMeters(1000.0)
                .build();

        AccessIntervalsRequest request = AccessIntervalsRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDateIso("2024-01-01T00:00:00Z")
                .setEndDateIso("2024-01-01T02:00:00Z")
                .setGroundStation(station)
                .setMinElevationDegrees(10.0)
                .build();

        VisibilityResult result = visibilityService.getAccessIntervals(visibilityTestMapper.toDTO(request));

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Ankara", result.stationName());
        Assertions.assertFalse(result.intervals().isEmpty());

        System.out.println("First pass: " + result.intervals().get(0).startIso() + " - "
                + result.intervals().get(0).endIso());
    }

    @Test
    public void testGetAccessIntervalsNoPass() {
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        GroundStation station = GroundStation.newBuilder()
                .setName("Ankara")
                .setLatitudeDegrees(39.9334)
                .setLongitudeDegrees(32.8597)
                .setAltitudeMeters(1000.0)
                .build();

        // Short duration where no pass is expected
        // We know from previous tests that the pass starts around 00:04:49.
        // So we check a very short interval before that.
        AccessIntervalsRequest request = AccessIntervalsRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDateIso("2024-01-01T00:00:00Z")
                .setEndDateIso("2024-01-01T00:01:00Z")
                .setGroundStation(station)
                .setMinElevationDegrees(10.0)
                .build();

        VisibilityResult result = visibilityService.getAccessIntervals(visibilityTestMapper.toDTO(request));

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.intervals().isEmpty());
    }

    @Test
    public void testGetAccessIntervalsAlreadyVisible() {
        // TLE that puts sat over Ankara at start
        // Simplified test - just ensuring it runs without error when handling initial visibility
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        GroundStation station = GroundStation.newBuilder()
                .setName("Ankara")
                .setLatitudeDegrees(39.9334)
                .setLongitudeDegrees(32.8597)
                .setAltitudeMeters(1000.0)
                .build();

        AccessIntervalsRequest request = AccessIntervalsRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDateIso("2024-01-01T00:05:00Z") // Adjusted time
                .setEndDateIso("2024-01-01T00:15:00Z")
                .setGroundStation(station)
                .setMinElevationDegrees(0.0) // Low elevation to increase chance
                .build();

        VisibilityResult result = visibilityService.getAccessIntervals(visibilityTestMapper.toDTO(request));

        Assertions.assertNotNull(result);
    }

    @Test
    public void testInvalidTLE() {
        GroundStation station = GroundStation.newBuilder()
                .setName("Ankara")
                .setLatitudeDegrees(39.9334)
                .setLongitudeDegrees(32.8597)
                .setAltitudeMeters(1000.0)
                .build();

        AccessIntervalsRequest request = AccessIntervalsRequest.newBuilder()
                .setTleLine1("invalid")
                .setTleLine2("invalid")
                .setStartDateIso("2024-01-01T00:00:00Z")
                .setEndDateIso("2024-01-01T02:00:00Z")
                .setGroundStation(station)
                .setMinElevationDegrees(10.0)
                .build();

        Assertions.assertThrows(Exception.class, () -> visibilityService.getAccessIntervals(visibilityTestMapper.toDTO(request)));
    }
}

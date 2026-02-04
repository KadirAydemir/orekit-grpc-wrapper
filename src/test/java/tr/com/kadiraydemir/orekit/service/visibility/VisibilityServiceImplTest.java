package tr.com.kadiraydemir.orekit.service.visibility;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import tr.com.kadiraydemir.orekit.grpc.AccessIntervalsRequest;
import tr.com.kadiraydemir.orekit.grpc.GroundStation;
import tr.com.kadiraydemir.orekit.model.AccessIntervalResult;
import tr.com.kadiraydemir.orekit.model.VisibilityResult;

import java.time.format.DateTimeFormatter;
import java.util.List;

@QuarkusTest
public class VisibilityServiceImplTest {

    @Inject
    VisibilityServiceImpl visibilityService;

    @Test
    public void testVisibilityBranches() {
        // ISS TLE
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        GroundStation station = GroundStation.newBuilder()
                .setName("Ankara")
                .setLatitudeDegrees(39.9334)
                .setLongitudeDegrees(32.8597)
                .setAltitudeMeters(938.0)
                .build();

        // 1. Find a pass in the first 2 hours
        AccessIntervalsRequest req1 = AccessIntervalsRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setGroundStation(station)
                .setStartDateIso("2024-01-01T00:00:00Z")
                .setEndDateIso("2024-01-01T04:00:00Z")
                .setMinElevationDegrees(10.0)
                .build();

        VisibilityResult res1 = visibilityService.getAccessIntervals(req1);
        Assertions.assertFalse(res1.intervals().isEmpty(), "Should find at least one pass");

        AccessIntervalResult firstPass = res1.intervals().get(0);
        AbsoluteDate passStart = new AbsoluteDate(firstPass.startIso(), TimeScalesFactory.getUTC());
        AbsoluteDate passEnd = new AbsoluteDate(firstPass.endIso(), TimeScalesFactory.getUTC());

        // 2. Test "Start Inside" (Already visible at start)
        // Start = passStart + 10s
        // End = passEnd + 10s (Sets during interval)
        AbsoluteDate start2 = passStart.shiftedBy(10.0);
        AbsoluteDate end2 = passEnd.shiftedBy(10.0);

        AccessIntervalsRequest req2 = req1.toBuilder()
                .setStartDateIso(start2.toString(TimeScalesFactory.getUTC()))
                .setEndDateIso(end2.toString(TimeScalesFactory.getUTC()))
                .build();

        VisibilityResult res2 = visibilityService.getAccessIntervals(req2);
        Assertions.assertEquals(1, res2.intervals().size());
        // Interval should start at simulation start
        Assertions.assertEquals(0.0,
            new AbsoluteDate(res2.intervals().get(0).startIso(), TimeScalesFactory.getUTC()).durationFrom(start2), 1e-3);
        // Interval should end at actual pass end
        Assertions.assertEquals(0.0,
            new AbsoluteDate(res2.intervals().get(0).endIso(), TimeScalesFactory.getUTC()).durationFrom(passEnd), 1e-3);

        // 3. Test "End Inside" (Still visible at end)
        // Start = passStart - 10s (Rises during interval)
        // End = passEnd - 10s (Simulation ends while visible)
        AbsoluteDate start3 = passStart.shiftedBy(-10.0);
        AbsoluteDate end3 = passEnd.shiftedBy(-10.0);

        AccessIntervalsRequest req3 = req1.toBuilder()
                .setStartDateIso(start3.toString(TimeScalesFactory.getUTC()))
                .setEndDateIso(end3.toString(TimeScalesFactory.getUTC()))
                .build();

        VisibilityResult res3 = visibilityService.getAccessIntervals(req3);
        Assertions.assertEquals(1, res3.intervals().size());
        // Interval should start at actual pass start
        Assertions.assertEquals(0.0,
            new AbsoluteDate(res3.intervals().get(0).startIso(), TimeScalesFactory.getUTC()).durationFrom(passStart), 1e-3);
        // Interval should end at simulation end
        Assertions.assertEquals(0.0,
            new AbsoluteDate(res3.intervals().get(0).endIso(), TimeScalesFactory.getUTC()).durationFrom(end3), 1e-3);

        // 4. Test "Always Inside" (Start visible, End visible)
        // Start = passStart + 10s
        // End = passEnd - 10s
        AbsoluteDate start4 = passStart.shiftedBy(10.0);
        AbsoluteDate end4 = passEnd.shiftedBy(-10.0);

        AccessIntervalsRequest req4 = req1.toBuilder()
                .setStartDateIso(start4.toString(TimeScalesFactory.getUTC()))
                .setEndDateIso(end4.toString(TimeScalesFactory.getUTC()))
                .build();

        VisibilityResult res4 = visibilityService.getAccessIntervals(req4);
        Assertions.assertEquals(1, res4.intervals().size());
        Assertions.assertEquals(0.0,
            new AbsoluteDate(res4.intervals().get(0).startIso(), TimeScalesFactory.getUTC()).durationFrom(start4), 1e-3);
        Assertions.assertEquals(0.0,
            new AbsoluteDate(res4.intervals().get(0).endIso(), TimeScalesFactory.getUTC()).durationFrom(end4), 1e-3);
    }
}

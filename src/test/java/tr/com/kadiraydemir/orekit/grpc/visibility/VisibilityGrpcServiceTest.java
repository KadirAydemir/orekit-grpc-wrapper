package tr.com.kadiraydemir.orekit.grpc.visibility;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.AccessInterval;
import tr.com.kadiraydemir.orekit.grpc.AccessIntervalsRequest;
import tr.com.kadiraydemir.orekit.grpc.AccessIntervalsResponse;
import tr.com.kadiraydemir.orekit.grpc.GroundStation;
import tr.com.kadiraydemir.orekit.grpc.VisibilityService;

import java.time.Duration;

@QuarkusTest
public class VisibilityGrpcServiceTest {

    @GrpcClient("visibility-service-client")
    VisibilityService visibilityService;

    @Test
    public void testVisibility() {
        GroundStation station = GroundStation.newBuilder()
                .setName("Ankara")
                .setLatitudeDegrees(39.9334)
                .setLongitudeDegrees(32.8597)
                .setAltitudeMeters(938.0)
                .build();

        // ISS TLE (approximate)
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        AccessIntervalsRequest request = AccessIntervalsRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setGroundStation(station)
                .setStartDateIso("2024-01-01T00:00:00Z")
                .setEndDateIso("2024-01-02T00:00:00Z") // 1 day
                .setMinElevationDegrees(10.0)
                .build();

        AccessIntervalsResponse response = visibilityService.getAccessIntervals(request)
                .await().atMost(Duration.ofSeconds(30));

        Assertions.assertNotNull(response);
        Assertions.assertEquals("Ankara", response.getStationName());
        // ISS should pass over Ankara at least once in 24h
        Assertions.assertTrue(response.getIntervalsCount() > 0, "Should have at least one pass over Ankara");

        AccessInterval firstPass = response.getIntervals(0);
        System.out.println("First pass: " + firstPass.getStartIso() + " - " + firstPass.getEndIso());
    }
}

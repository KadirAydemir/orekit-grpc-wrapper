package tr.com.kadiraydemir.orekit.grpc.visibility;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void testBulkVisibilityCalculation() {
        GroundStation station = GroundStation.newBuilder()
                .setName("Ankara")
                .setLatitudeDegrees(39.9334)
                .setLongitudeDegrees(32.8597)
                .setAltitudeMeters(938.0)
                .build();

        // ISS TLE
        String issLine1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String issLine2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        // Hubble TLE
        String hubbleLine1 = "1 20580U 90037B   24001.00000000  .00001285  00000-0  65430-4 0  9992";
        String hubbleLine2 = "2 20580  28.4699 139.8847 0002819 100.0000 260.0000 15.09691001 22222";

        BatchAccessIntervalsRequest request = BatchAccessIntervalsRequest.newBuilder()
                .addTles(TLELines.newBuilder().setTleLine1(issLine1).setTleLine2(issLine2).build())
                .addTles(TLELines.newBuilder().setTleLine1(hubbleLine1).setTleLine2(hubbleLine2).build())
                .setGroundStation(station)
                .setStartDateIso("2024-01-01T00:00:00Z")
                .setEndDateIso("2024-01-02T00:00:00Z")
                .setMinElevationDegrees(10.0)
                .build();

        // Collect batch responses
        List<BatchAccessIntervalsResponse> batchResponses = visibilityService.batchGetAccessIntervals(request)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(60));

        // Flatten the batch responses to individual AccessIntervalsResponses
        List<AccessIntervalsResponse> allResponses = new ArrayList<>();
        for (BatchAccessIntervalsResponse batchResponse : batchResponses) {
            allResponses.addAll(batchResponse.getResultsList());
        }

        System.out.println("Received " + batchResponses.size() + " batches with " + allResponses.size() + " total responses");

        // Should receive 2 responses (one for each satellite)
        Assertions.assertEquals(2, allResponses.size(), "Should receive 2 responses for 2 satellites");

        for (AccessIntervalsResponse response : allResponses) {
            System.out.println("Satellite: " + response.getSatelliteName() + " has " + response.getIntervalsCount() + " access intervals from Ankara");
        }
    }
}

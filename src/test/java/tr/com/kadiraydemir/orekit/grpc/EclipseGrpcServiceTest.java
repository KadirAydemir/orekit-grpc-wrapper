package tr.com.kadiraydemir.orekit.grpc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class EclipseGrpcServiceTest {

    @GrpcClient("eclipse-service-client")
    EclipseService eclipseService;

    @Test
    public void testEclipseCalculation() {
        // ISS TLE
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        EclipseRequest request = EclipseRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDateIso("2024-02-01T00:00:00Z")
                .setEndDateIso("2024-02-02T00:00:00Z") // 1 day, 1 month later
                .build();

        EclipseResponse response = eclipseService.calculateEclipses(request)
                .await().atMost(Duration.ofSeconds(60));

        Assertions.assertNotNull(response);
        System.out.println("Found " + response.getIntervalsCount() + " eclipse intervals.");

        // ISS orbits every ~90 mins, so in 24h there should be ~16 orbits.
        // It enters eclipse almost every orbit.
        Assertions.assertTrue(response.getIntervalsCount() > 0, "Should have eclipse intervals");

        for (EclipseInterval interval : response.getIntervalsList()) {
            System.out.println("Eclipse: " + interval.getStartIso() + " - " + interval.getEndIso() + " (" + interval.getDurationSeconds() + "s)");
            Assertions.assertTrue(interval.getDurationSeconds() > 0);
        }
    }

    @Test
    public void testBulkEclipseCalculation() {
        // ISS TLE
        String issLine1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String issLine2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        // Hubble TLE
        String hubbleLine1 = "1 20580U 90037B   24001.00000000  .00001285  00000-0  65430-4 0  9992";
        String hubbleLine2 = "2 20580  28.4699 139.8847 0002819 100.0000 260.0000 15.09691001 22222";

        BatchEclipseRequest request = BatchEclipseRequest.newBuilder()
                .addTles(TLEPair.newBuilder().setLine1(issLine1).setLine2(issLine2).build())
                .addTles(TLEPair.newBuilder().setLine1(hubbleLine1).setLine2(hubbleLine2).build())
                .setStartDateIso("2024-02-01T00:00:00Z")
                .setEndDateIso("2024-02-02T00:00:00Z")
                .build();

        // Collect batch responses
        List<BatchEclipseResponse> batchResponses = eclipseService.batchCalculateEclipses(request)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(60));

        // Flatten the batch responses to individual EclipseResponses
        List<EclipseResponse> allResponses = new ArrayList<>();
        for (BatchEclipseResponse batchResponse : batchResponses) {
            allResponses.addAll(batchResponse.getResultsList());
        }

        System.out.println("Received " + batchResponses.size() + " batches with " + allResponses.size() + " total responses");

        // Should receive 2 responses (one for each satellite)
        Assertions.assertEquals(2, allResponses.size(), "Should receive 2 responses for 2 satellites");

        for (EclipseResponse response : allResponses) {
            Assertions.assertTrue(response.getNoradId() > 0, "NORAD ID should be positive");
            System.out.println("Satellite NORAD ID: " + response.getNoradId() + " has " + response.getIntervalsCount() + " eclipse intervals");
        }
    }
}

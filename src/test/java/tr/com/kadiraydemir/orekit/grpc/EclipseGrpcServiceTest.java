package tr.com.kadiraydemir.orekit.grpc;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.EclipseInterval;
import tr.com.kadiraydemir.orekit.grpc.EclipseRequest;
import tr.com.kadiraydemir.orekit.grpc.EclipseResponse;
import tr.com.kadiraydemir.orekit.grpc.EclipseService;

import java.time.Duration;

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
                .await().atMost(Duration.ofSeconds(30));

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
}

package tr.com.kadiraydemir.orekit.grpc.propagation;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@QuarkusTest
public class ParallelPropagationTest {

    @GrpcClient("orbital-service-client")
    OrbitalService orbitalService;

    @Test
    public void testParallelPropagateTLE() {
        int parallelRequests = 5;
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDate("2024-01-01T12:00:00Z")
                .setEndDate("2024-01-01T13:00:00Z") // 1 hour
                .setPositionCount(10)
                .setOutputFrame(ReferenceFrame.TEME)
                .build();

        List<Uni<List<TLEPropagateResponse>>> unis = new ArrayList<>();

        for (int i = 0; i < parallelRequests; i++) {
            unis.add(orbitalService.propagateTLE(request)
                    .collect().asList());
        }

        List<List<TLEPropagateResponse>> results = Uni.join().all(unis).andFailFast()
                .await().atMost(Duration.ofSeconds(30));

        Assertions.assertEquals(parallelRequests, results.size());
        for (List<TLEPropagateResponse> responses : results) {
            Assertions.assertFalse(responses.isEmpty());
            int totalPositions = responses.stream()
                    .mapToInt(TLEPropagateResponse::getPositionsCount)
                    .sum();
            Assertions.assertEquals(10, totalPositions);
        }
    }
}

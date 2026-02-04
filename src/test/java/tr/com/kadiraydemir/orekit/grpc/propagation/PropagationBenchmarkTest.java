package tr.com.kadiraydemir.orekit.grpc.propagation;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

@QuarkusTest
public class PropagationBenchmarkTest {

    @GrpcClient("orbital-service-client")
    OrbitalService orbitalService;

    @Test
    public void testPropagateTLEPerformance() {
        // ISS TLE
        String line1 = "1 25544U 98067A   23355.72295190  .00016622  00000+0  30613-3 0  9997";
        String line2 = "2 25544  51.6413 259.6247 0001395 348.8188 126.9748 15.49571329431105";

        int positionCount = 10000;

        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setModel(PropagationModel.SGP4)
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDate("2023-12-21T12:00:00Z")
                .setEndDate("2023-12-22T12:00:00Z") // 24 hours
                .setPositionCount(positionCount)
                .setOutputFrame(ReferenceFrame.TEME)
                .build();

        long startTime = System.currentTimeMillis();

        AtomicInteger totalPoints = new AtomicInteger(0);

        List<TLEPropagateResponse> responses = orbitalService.propagateTLE(request)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(60));

        for (TLEPropagateResponse response : responses) {
            totalPoints.addAndGet(response.getPositionsCount());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("BENCHMARK_RESULT: Time taken: " + duration + " ms");
        System.out.println("BENCHMARK_RESULT: Total responses: " + responses.size());
        System.out.println("BENCHMARK_RESULT: Total points: " + totalPoints.get());

        Assertions.assertEquals(positionCount, totalPoints.get(), "Total points should match requested count");
    }
}

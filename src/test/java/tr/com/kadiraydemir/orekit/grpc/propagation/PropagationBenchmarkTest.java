package tr.com.kadiraydemir.orekit.grpc.propagation;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.*;

import java.time.Duration;
import java.util.ArrayList;
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

        @Test
        public void testPropagateTLEListPerformance() {
                int tleCount = 1000;
                int positionCount = 10;

                List<TLELines> tles = new ArrayList<>(tleCount);
                String baseLine1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  999";
                String baseLine2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000";

                for (int i = 0; i < tleCount; i++) {
                        String line1 = baseLine1 + String.format("%01d", i % 10);
                        String line2 = baseLine2 + String.format("%05d", i);
                        tles.add(TLELines.newBuilder()
                                        .setTleLine1(line1)
                                        .setTleLine2(line2)
                                        .build());
                }

                BatchTLEPropagateRequest request = BatchTLEPropagateRequest.newBuilder()
                                .setModel(PropagationModel.SGP4)
                                .setStartDate("2024-01-01T12:00:00Z")
                                .setEndDate("2024-01-01T13:00:00Z")
                                .setPositionCount(positionCount)
                                .setOutputFrame(ReferenceFrame.TEME)
                                .addAllTles(tles)
                                .build();

                long startTime = System.currentTimeMillis();

                List<BatchTLEPropagateResponse> responses = orbitalService.batchPropagateTLE(request)
                                .collect().asList()
                                .await().atMost(Duration.ofSeconds(120));

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                System.out.println("TLE_LIST_BENCHMARK: Total TLEs: " + tleCount);
                System.out.println("TLE_LIST_BENCHMARK: Processed: " + responses.size());
                System.out.println("TLE_LIST_BENCHMARK: Duration: " + duration + " ms");
                System.out.println("TLE_LIST_BENCHMARK: Throughput: " + (tleCount * 1000.0 / duration) + " TLEs/sec");
                System.out.println("TLE_LIST_BENCHMARK: Avg per TLE: " + (duration / (double) tleCount) + " ms");

                Assertions.assertEquals(tleCount, responses.size(), "Should receive response for each TLE");
        }
}

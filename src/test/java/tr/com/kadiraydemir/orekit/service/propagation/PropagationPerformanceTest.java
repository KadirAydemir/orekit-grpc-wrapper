package tr.com.kadiraydemir.orekit.service.propagation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;
import tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest;
import tr.com.kadiraydemir.orekit.mapper.PropagationTestMapper;
import tr.com.kadiraydemir.orekit.model.TleResult;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@QuarkusTest
public class PropagationPerformanceTest {

    @Inject
    PropagationService propagationService;

    @Inject
    PropagationTestMapper propagationTestMapper;

    @Test
    public void testConcurrentPropagation() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setStartDate("2024-01-01T12:00:00Z")
                .setEndDate("2024-01-02T12:00:00Z")
                .setPositionCount(100)
                .setOutputFrame(ReferenceFrame.TEME)
                .build();

        long start = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    List<TleResult> results = propagationService.propagateTLE(propagationTestMapper.toDTO(request))
                            .collect().asList().await().atMost(Duration.ofSeconds(10));
                    Assertions.assertFalse(results.isEmpty());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - start;

        System.out.println("Execution thread: " + Thread.currentThread().getName());
        System.out.println("Total items: " + threadCount);
        System.out.println("Duration: " + duration + " ms");

        executor.shutdown();
    }
}

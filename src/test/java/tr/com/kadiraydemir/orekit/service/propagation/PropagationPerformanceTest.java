package tr.com.kadiraydemir.orekit.service.propagation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.*;
import tr.com.kadiraydemir.orekit.model.TleResult;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@QuarkusTest
public class PropagationPerformanceTest {

    @Inject
    PropagationService propagationService;

    @Test
    public void testBlockingBehavior() {
        // Valid ISS TLE
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";

        TLEPropagateRequest request = TLEPropagateRequest.newBuilder()
                .setTleLine1(line1)
                .setTleLine2(line2)
                .setModel(PropagationModel.SGP4)
                .setOutputFrame(ReferenceFrame.TEME)
                .setStartDate("2024-01-01T00:00:00Z")
                .setEndDate("2024-01-01T01:00:00Z") // 1 hour
                .setPositionCount(1000)
                .build();

        AtomicReference<String> threadName = new AtomicReference<>();

        long start = System.currentTimeMillis();

        List<TleResult> results = propagationService.propagateTLE(request)
                .invoke(item -> {
                    if (threadName.get() == null) {
                        threadName.set(Thread.currentThread().getName());
                    }
                })
                .collect().asList()
                .await().atMost(Duration.ofSeconds(30));

        long duration = System.currentTimeMillis() - start;

        Assertions.assertFalse(results.isEmpty());
        System.out.println("Execution thread: " + threadName.get());
        System.out.println("Total items: " + results.size());
        System.out.println("Duration: " + duration + " ms");
    }
}

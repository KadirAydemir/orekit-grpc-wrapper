package tr.com.kadiraydemir.orekit.grpc;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MyLivenessCheckTest {

    @Inject
    @Liveness
    MyLivenessCheck livenessCheck;

    @Test
    public void testLiveness() {
        HealthCheckResponse response = livenessCheck.call();
        Assertions.assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        Assertions.assertEquals("alive", response.getName());
    }
}

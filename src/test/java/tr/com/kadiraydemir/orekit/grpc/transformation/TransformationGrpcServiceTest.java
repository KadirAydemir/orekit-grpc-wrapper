package tr.com.kadiraydemir.orekit.grpc.transformation;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.CoordinateTransformService;
import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;
import tr.com.kadiraydemir.orekit.grpc.TransformRequest;
import tr.com.kadiraydemir.orekit.grpc.TransformResponse;

import java.time.Duration;

@QuarkusTest
public class TransformationGrpcServiceTest {

    @GrpcClient("coordinate-transform-service-client")
    CoordinateTransformService coordinateTransformService;

    @Test
    public void testTransformation() {
        TransformRequest request = TransformRequest.newBuilder()
                .setSourceFrame(ReferenceFrame.TEME)
                .setTargetFrame(ReferenceFrame.ITRF)
                .setEpochIso("2024-01-01T12:00:00Z")
                .setX(7000000.0)
                .setY(0.0)
                .setZ(0.0)
                .setVx(0.0)
                .setVy(7500.0)
                .setVz(0.0)
                .build();

        TransformResponse response = coordinateTransformService.transform(request)
                .await().atMost(Duration.ofSeconds(30));

        Assertions.assertNotNull(response);
        Assertions.assertEquals(ReferenceFrame.ITRF, response.getTargetFrame());
        // Simple check that coordinates changed (TEME != ITRF due to Earth rotation)
        Assertions.assertNotEquals(7000000.0, response.getX(), 1.0);
    }
}

package tr.com.kadiraydemir.orekit.grpc.transformation;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.grpc.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void testBatchTransformation() {
        // Create multiple state vectors to transform
        List<StateVector> stateVectors = new ArrayList<>();
        
        // State vector 1
        stateVectors.add(StateVector.newBuilder()
                .setX(7000000.0)
                .setY(0.0)
                .setZ(0.0)
                .setVx(0.0)
                .setVy(7500.0)
                .setVz(0.0)
                .build());
        
        // State vector 2
        stateVectors.add(StateVector.newBuilder()
                .setX(0.0)
                .setY(7000000.0)
                .setZ(0.0)
                .setVx(-7500.0)
                .setVy(0.0)
                .setVz(0.0)
                .build());

        BatchTransformRequest request = BatchTransformRequest.newBuilder()
                .setSourceFrame(ReferenceFrame.TEME)
                .setTargetFrame(ReferenceFrame.ITRF)
                .setEpochIso("2024-01-01T12:00:00Z")
                .addAllStateVectors(stateVectors)
                .build();

        // Collect batch responses
        List<BatchTransformResponse> batchResponses = coordinateTransformService.batchTransform(request)
                .collect().asList()
                .await().atMost(Duration.ofSeconds(30));

        // Flatten the batch responses to individual TransformResponses
        List<TransformResponse> allResponses = new ArrayList<>();
        for (BatchTransformResponse batchResponse : batchResponses) {
            allResponses.addAll(batchResponse.getResultsList());
        }

        System.out.println("Received " + batchResponses.size() + " batches with " + allResponses.size() + " total responses");

        // Should receive 2 responses (one for each state vector)
        Assertions.assertEquals(2, allResponses.size(), "Should receive 2 responses for 2 state vectors");

        for (TransformResponse response : allResponses) {
            Assertions.assertEquals(ReferenceFrame.ITRF, response.getTargetFrame());
            // Verify coordinates were transformed (should not be the same as input)
            Assertions.assertNotEquals(0.0, response.getX(), 1.0);
        }
    }
}

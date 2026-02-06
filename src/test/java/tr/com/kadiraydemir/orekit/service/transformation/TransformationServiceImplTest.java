package tr.com.kadiraydemir.orekit.service.transformation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.model.ReferenceFrameType;
import tr.com.kadiraydemir.orekit.model.TransformRequest;
import tr.com.kadiraydemir.orekit.model.TransformResult;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("TransformationServiceImpl Tests")
public class TransformationServiceImplTest {

    @Inject
    TransformationService transformationService;

    @Test
    @DisplayName("Should transform coordinates between frames successfully")
    public void transform_validRequest_returnsTransformedCoordinates() {
        // Given - Simple position in EME2000
        TransformRequest request = new TransformRequest(
            ReferenceFrameType.EME2000,
            ReferenceFrameType.ITRF,
            "2024-01-01T12:00:00Z",
            7000000.0,  // x
            0.0,        // y
            0.0,        // z
            0.0,        // vx
            7500.0,     // vy (circular orbit velocity)
            0.0         // vz
        );

        // When
        TransformResult result = transformationService.transform(request);

        // Then
        assertNotNull(result);
        assertEquals("EME2000", result.sourceFrame());
        assertEquals("ITRF", result.targetFrame());
        assertEquals("2024-01-01T12:00:00Z", result.epochIso());
        
        // Coordinates should be transformed (not same as input)
        assertNotEquals(0.0, result.x());
        assertNotEquals(0.0, result.y());
        assertNotEquals(0.0, result.z());
    }

    @Test
    @DisplayName("Should throw exception for invalid date format")
    public void transform_invalidDate_throwsException() {
        // Given - Invalid date format
        TransformRequest request = new TransformRequest(
            ReferenceFrameType.EME2000,
            ReferenceFrameType.ITRF,
            "invalid-date-format",
            7000000.0,
            0.0,
            0.0,
            0.0,
            7500.0,
            0.0
        );

        // Then
        assertThrows(Exception.class, () -> {
            transformationService.transform(request);
        });
    }

    @Test
    @DisplayName("Should transform from ITRF to EME2000")
    public void transform_itrfToEme2000_returnsTransformedCoordinates() {
        // Given - Position in ITRF
        TransformRequest request = new TransformRequest(
            ReferenceFrameType.ITRF,
            ReferenceFrameType.EME2000,
            "2024-01-01T12:00:00Z",
            6371000.0,  // Earth radius approx
            0.0,
            0.0,
            0.0,
            0.0,
            0.0
        );

        // When
        TransformResult result = transformationService.transform(request);

        // Then
        assertNotNull(result);
        assertEquals("ITRF", result.sourceFrame());
        assertEquals("EME2000", result.targetFrame());
        assertNotNull(result.x());
        assertNotNull(result.y());
        assertNotNull(result.z());
    }

    @Test
    @DisplayName("Should transform with zero velocity")
    public void transform_zeroVelocity_returnsTransformedCoordinates() {
        // Given - Position with no velocity
        TransformRequest request = new TransformRequest(
            ReferenceFrameType.EME2000,
            ReferenceFrameType.ITRF,
            "2024-01-01T00:00:00Z",
            7000000.0,
            1000000.0,
            500000.0,
            0.0,
            0.0,
            0.0
        );

        // When
        TransformResult result = transformationService.transform(request);

        // Then
        assertNotNull(result);
        // Note: Frame transformation introduces velocity due to Earth rotation
        // so output velocity won't be exactly zero even with zero input velocity
        assertNotNull(result.vx());
        assertNotNull(result.vy());
        assertNotNull(result.vz());
    }

    @Test
    @DisplayName("Should handle different epochs")
    public void transform_differentEpochs_returnsTransformedCoordinates() {
        // Given - Same position, different times
        String[] epochs = {
            "2024-01-01T00:00:00Z",
            "2024-06-01T00:00:00Z",
            "2024-12-01T00:00:00Z"
        };

        for (String epoch : epochs) {
            TransformRequest request = new TransformRequest(
                ReferenceFrameType.EME2000,
                ReferenceFrameType.ITRF,
                epoch,
                7000000.0,
                0.0,
                0.0,
                0.0,
                7500.0,
                0.0
            );

            // When
            TransformResult result = transformationService.transform(request);

            // Then
            assertNotNull(result, "Should return result for epoch: " + epoch);
            assertEquals(epoch, result.epochIso());
        }
    }

    @Test
    @DisplayName("Should transform with high velocity")
    public void transform_highVelocity_returnsTransformedCoordinates() {
        // Given - High velocity (near escape velocity)
        TransformRequest request = new TransformRequest(
            ReferenceFrameType.EME2000,
            ReferenceFrameType.ITRF,
            "2024-01-01T12:00:00Z",
            7000000.0,
            0.0,
            0.0,
            10000.0,  // vx
            10000.0,  // vy
            10000.0   // vz
        );

        // When
        TransformResult result = transformationService.transform(request);

        // Then
        assertNotNull(result);
        assertTrue(Math.abs(result.vx()) > 0);
        assertTrue(Math.abs(result.vy()) > 0);
        assertTrue(Math.abs(result.vz()) > 0);
    }
}

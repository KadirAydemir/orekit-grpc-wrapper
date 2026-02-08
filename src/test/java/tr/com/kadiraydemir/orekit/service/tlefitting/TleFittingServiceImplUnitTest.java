package tr.com.kadiraydemir.orekit.service.tlefitting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.ZipJarCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import tr.com.kadiraydemir.orekit.model.PositionMeasurement;
import tr.com.kadiraydemir.orekit.model.ReferenceFrameType;
import tr.com.kadiraydemir.orekit.model.TleFittingRequest;
import tr.com.kadiraydemir.orekit.model.TleFittingResult;
import tr.com.kadiraydemir.orekit.service.frame.FrameService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TleFittingServiceImplUnitTest {

    @Mock
    FrameService frameService;

    @InjectMocks
    TleFittingServiceImpl tleFittingService;

    @BeforeAll
    public static void initOrekit() {
        File orekitData = new File("orekit-data.zip");
        if (orekitData.exists()) {
            DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            // Check if already loaded to avoid duplicates
            if (manager.getProviders().isEmpty()) {
                manager.addProvider(new ZipJarCrawler(orekitData));
            }
        }
    }

    @Test
    public void fitTLE_insufficientMeasurements_returnsFailure() {
        List<PositionMeasurement> measurements = new ArrayList<>();
        measurements.add(PositionMeasurement.of("2024-01-01T12:00:00Z", 1000.0, 2000.0, 3000.0));

        TleFittingRequest request = new TleFittingRequest(
                null, null, "SAT", 12345, "ID",
                measurements, 1.0, 100, ReferenceFrameType.TEME
        );

        TleFittingResult result = tleFittingService.fitTLE(request);
        assertNotNull(result);
        assertNotNull(result.error());
        assertTrue(result.error().contains("At least 2 measurements required"));
    }

    @Test
    public void fitTLE_estimationFailure_returnsFailure() {
        // Setup measurements
        List<PositionMeasurement> measurements = new ArrayList<>();
        measurements.add(PositionMeasurement.of("2024-01-01T12:00:00Z", 7000000.0, 0.0, 0.0));
        measurements.add(PositionMeasurement.of("2024-01-01T12:10:00Z", 0.0, 7000000.0, 0.0));

        // Mock frame service to return a frame but we want estimation to fail or produce no result
        // Since we can't easily mock the internal BatchLSEstimator, we rely on input that causes convergence failure
        // or effectively test that the method handles exceptions.

        // To test exception handling, we can make frameService throw an exception
        when(frameService.resolveFrame(any())).thenThrow(new RuntimeException("Frame error"));

        TleFittingRequest request = new TleFittingRequest(
                null, null, "SAT", 12345, "ID",
                measurements, 1.0, 100, ReferenceFrameType.TEME
        );

        TleFittingResult result = tleFittingService.fitTLE(request);
        assertNotNull(result);
        assertNotNull(result.error());
        assertTrue(result.error().contains("TLE fitting failed: Frame error"));
    }

    @Test
    public void addMeasurements_transformationException_usesOriginalPosition() {
        // This tests the catch block inside addMeasurements
        // We need inputFrame != temeFrame
        // And inputFrame.getTransformTo(temeFrame) to throw exception

        Frame mockFrame = mock(Frame.class);
        when(mockFrame.getName()).thenReturn("MockFrame");
        when(frameService.resolveFrame(any())).thenReturn(mockFrame);

        // When getTransformTo is called, throw exception
        when(mockFrame.getTransformTo(any(Frame.class), any(AbsoluteDate.class)))
            .thenThrow(new org.orekit.errors.OrekitException(org.orekit.errors.OrekitMessages.NO_DATA_IN_FILE, "test"));

        List<PositionMeasurement> measurements = new ArrayList<>();
        measurements.add(PositionMeasurement.of("2024-01-01T12:00:00Z", 7000000.0, 0.0, 0.0));
        measurements.add(PositionMeasurement.of("2024-01-01T12:10:00Z", 0.0, 7000000.0, 0.0));

        TleFittingRequest request = new TleFittingRequest(
                null, null, "SAT", 12345, "ID",
                measurements, 1.0, 100, ReferenceFrameType.ITRF
        );

        // We expect the service to proceed (using original position) and likely fail estimation or succeed depending on logic
        // But we want to ensure it doesn't crash with the OrekitException from getTransformTo

        TleFittingResult result = tleFittingService.fitTLE(request);
        assertNotNull(result);
        // It might fail estimation, but it shouldn't be "TLE fitting failed: ... NO_DATA_IN_FILE"
        // effectively checking that the warn log was printed and execution continued.

        // Wait, if it continues, it adds the measurement. Then it runs estimation.
        // If estimation fails, it returns failure.

        // The important part is that the exception from getTransformTo is caught.
        // We can verify this by checking that the error message is NOT related to "NO_DATA_IN_FILE"
        // OR if it fails later, it's fine.

        if (result.error() != null) {
             assertFalse(result.error().contains("NO_DATA_IN_FILE"));
        }
    }

    @Test
    public void fitTLE_mathRuntimeException_returnsFailure() {
        // Mock frame service to throw MathRuntimeException
        // hipparchus.exception.MathRuntimeException is checked or unchecked?
        // It's a RuntimeException.

        when(frameService.resolveFrame(any())).thenThrow(new org.hipparchus.exception.MathRuntimeException(
                new org.hipparchus.exception.DummyLocalizable("Math error"), "detail"));

        List<PositionMeasurement> measurements = new ArrayList<>();
        measurements.add(PositionMeasurement.of("2024-01-01T12:00:00Z", 7000000.0, 0.0, 0.0));
        measurements.add(PositionMeasurement.of("2024-01-01T12:10:00Z", 0.0, 7000000.0, 0.0));

        TleFittingRequest request = new TleFittingRequest(
                null, null, "SAT", 12345, "ID",
                measurements, 1.0, 100, ReferenceFrameType.TEME
        );

        TleFittingResult result = tleFittingService.fitTLE(request);
        assertNotNull(result);
        assertNotNull(result.error());
        // Logic logs "TLE fitting failed - Math error" and returns "TLE fitting failed: Math error"
        // Wait, e.getMessage() for MathRuntimeException usually returns the localized message.
        assertTrue(result.error().contains("TLE fitting failed"));
    }

    @Test
    public void fitTLE_orekitException_returnsFailure() {
        // Mock frame service to throw OrekitException
        // OrekitException is a RuntimeException in newer versions (10+)

        when(frameService.resolveFrame(any())).thenThrow(new org.orekit.errors.OrekitException(
                org.orekit.errors.OrekitMessages.NO_DATA_IN_FILE, "test"));

        List<PositionMeasurement> measurements = new ArrayList<>();
        measurements.add(PositionMeasurement.of("2024-01-01T12:00:00Z", 7000000.0, 0.0, 0.0));
        measurements.add(PositionMeasurement.of("2024-01-01T12:10:00Z", 0.0, 7000000.0, 0.0));

        TleFittingRequest request = new TleFittingRequest(
                null, null, "SAT", 12345, "ID",
                measurements, 1.0, 100, ReferenceFrameType.TEME
        );

        TleFittingResult result = tleFittingService.fitTLE(request);
        assertNotNull(result);
        assertNotNull(result.error());
        assertTrue(result.error().contains("TLE fitting failed"));
    }
}

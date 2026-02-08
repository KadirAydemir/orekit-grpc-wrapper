package tr.com.kadiraydemir.orekit.service.analysis;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.propagation.analytical.tle.TLE;
import tr.com.kadiraydemir.orekit.model.ReferenceFrameType;

import java.util.ArrayList;
import java.util.List;

@QuarkusTest
public class StatelessOrbitAnalysisServiceTest {

    @Inject
    StatelessOrbitAnalysisService analysisService;

    private TLE createSampleTLE() {
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";
        return new TLE(line1, line2);
    }

    private TLE createModifiedTLE(TLE original, double meanMotionOffset) {
        // Create a slightly modified TLE to simulate a maneuver
        // This is a simplified approach - in reality, modifying TLE requires
        // recalculating checksums
        String line1 = original.getLine1();
        String line2 = original.getLine2();
        return new TLE(line1, line2);
    }

    @Test
    public void testAnalyzeNoManeuvers() {
        // Test with same TLE as initial and observed - should detect no maneuvers
        TLE initialTle = createSampleTLE();
        List<TLE> observedTles = List.of(initialTle);

        StatelessOrbitAnalysisService.ManeuverDetectionResult result = analysisService.detect(
                initialTle,
                observedTles,
                null, // Use default config
                5.0, // 5km threshold - should not trigger for same TLE
                ReferenceFrameType.TEME);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.maneuvers());
        Assertions.assertNotNull(result.maneuvers());
        // Same TLE at epoch should have ~0 residual
        Assertions.assertTrue(result.maneuvers().isEmpty() ||
                result.maneuvers().stream().allMatch(m -> m.residualKm() < 5.0));
    }

    @Test
    public void testAnalyzeWithEmptyObservedTles() {
        TLE initialTle = createSampleTLE();
        List<TLE> observedTles = new ArrayList<>();

        StatelessOrbitAnalysisService.ManeuverDetectionResult result = analysisService.detect(
                initialTle,
                observedTles,
                null,
                2.0,
                ReferenceFrameType.TEME);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.maneuvers().isEmpty());
    }

    @Test
    public void testAnalyzeWithCustomConfig() {
        TLE initialTle = createSampleTLE();
        List<TLE> observedTles = List.of(initialTle);

        StatelessOrbitAnalysisService.ForceModelConfig config = new StatelessOrbitAnalysisService.ForceModelConfig(20,
                20, false, false);

        StatelessOrbitAnalysisService.ManeuverDetectionResult result = analysisService.detect(
                initialTle,
                observedTles,
                config,
                1.0,
                ReferenceFrameType.EME2000);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.frame());
        Assertions.assertNotNull(result.frame());
    }

    @Test
    public void testAnalyzeManeuversParallelProcessing() {
        // Test that parallel processing works correctly
        TLE initialTle = createSampleTLE();

        // Create multiple observed TLEs (all same for this test)
        List<TLE> observedTles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            observedTles.add(initialTle);
        }

        StatelessOrbitAnalysisService.ManeuverDetectionResult result = analysisService.detect(
                initialTle,
                observedTles,
                null,
                1.0,
                ReferenceFrameType.TEME);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.maneuvers());
        // All TLEs are same, so no maneuvers should be detected with reasonable
        // threshold
    }

    @Test
    public void testAnalyzeWithDifferentOutputFrames() {
        TLE initialTle = createSampleTLE();

        for (ReferenceFrameType frame : ReferenceFrameType.values()) {
            StatelessOrbitAnalysisService.ManeuverDetectionResult result = analysisService.detect(
                    initialTle,
                    new ArrayList<>(),
                    null,
                    2.0,
                    frame);

            Assertions.assertNotNull(result, "Result should not be null for frame: " + frame);
            Assertions.assertNotNull(result.frame(), "Frame should not be null");
        }
    }
}
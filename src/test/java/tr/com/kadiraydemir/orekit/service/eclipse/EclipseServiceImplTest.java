package tr.com.kadiraydemir.orekit.service.eclipse;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tr.com.kadiraydemir.orekit.model.EclipseIntervalResult;
import tr.com.kadiraydemir.orekit.model.EclipseRequest;
import tr.com.kadiraydemir.orekit.model.EclipseResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("EclipseServiceImpl Tests")
public class EclipseServiceImplTest {

    @Inject
    EclipseService eclipseService;

    @Test
    @DisplayName("Should calculate eclipses for valid TLE")
    public void calculateEclipses_validTLE_returnsResult() {
        // Given - ISS TLE from recent epoch
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";
        
        EclipseRequest request = new EclipseRequest(
            line1,
            line2,
            "2024-01-01T00:00:00Z",
            "2024-01-02T00:00:00Z"
        );

        // When
        EclipseResult result = eclipseService.calculateEclipses(request);

        // Then
        assertNotNull(result);
        assertTrue(result.noradId() > 0);
        assertNotNull(result.intervals());
        // ISS typically has eclipses, so we expect some intervals
        // But this depends on the specific time period
    }

    @Test
    @DisplayName("Should handle short propagation period")
    public void calculateEclipses_shortPeriod_returnsResult() {
        // Given - Short 1-hour window
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";
        
        EclipseRequest request = new EclipseRequest(
            line1,
            line2,
            "2024-01-01T12:00:00Z",
            "2024-01-01T13:00:00Z"
        );

        // When
        EclipseResult result = eclipseService.calculateEclipses(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.intervals());
    }

    @Test
    @DisplayName("Should handle invalid TLE gracefully")
    public void calculateEclipses_invalidTLE_throwsException() {
        // Given - Invalid TLE lines
        String line1 = "INVALID LINE 1";
        String line2 = "INVALID LINE 2";
        
        EclipseRequest request = new EclipseRequest(
            line1,
            line2,
            "2024-01-01T00:00:00Z",
            "2024-01-02T00:00:00Z"
        );

        // Then
        assertThrows(Exception.class, () -> {
            eclipseService.calculateEclipses(request);
        });
    }

    @Test
    @DisplayName("Should calculate eclipses for geostationary satellite")
    public void calculateEclipses_geostationarySatellite_returnsResult() {
        // Given - Geostationary satellite TLE (example)
        String line1 = "1 36516U 10024A   24001.00000000 -.00000113  00000-0  00000-0 0  9990";
        String line2 = "2 36516   0.0478  75.7114 0002428  90.0000 270.0000  1.00270112 11111";
        
        EclipseRequest request = new EclipseRequest(
            line1,
            line2,
            "2024-01-01T00:00:00Z",
            "2024-01-02T00:00:00Z"
        );

        // When
        EclipseResult result = eclipseService.calculateEclipses(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.intervals());
        // GEO satellites have eclipse seasons, so intervals may or may not be present
    }

    @Test
    @DisplayName("Should handle TLE with different satellite")
    public void calculateEclipses_differentSatellite_returnsResult() {
        // Given - Different satellite (Hubble as example)
        String line1 = "1 20580U 90037B   24001.00000000  .00001285  00000-0  65430-4 0  9992";
        String line2 = "2 20580  28.4699 139.8847 0002819 100.0000 260.0000 15.09691001 22222";
        
        EclipseRequest request = new EclipseRequest(
            line1,
            line2,
            "2024-01-01T00:00:00Z",
            "2024-01-01T06:00:00Z"
        );

        // When
        EclipseResult result = eclipseService.calculateEclipses(request);

        // Then
        assertNotNull(result);
        assertTrue(result.noradId() > 0);
        assertNotNull(result.intervals());
    }

    @Test
    @DisplayName("Should include interval details when eclipses occur")
    public void calculateEclipses_withEclipses_hasIntervalDetails() {
        // Given - Longer period to increase chance of eclipse
        String line1 = "1 25544U 98067A   24001.00000000  .00016717  00000-0  10270-3 0  9991";
        String line2 = "2 25544  51.6444  20.0000 0005000  0.0000  50.0000 15.50000000 10005";
        
        EclipseRequest request = new EclipseRequest(
            line1,
            line2,
            "2024-01-01T00:00:00Z",
            "2024-01-03T00:00:00Z" // 48 hours
        );

        // When
        EclipseResult result = eclipseService.calculateEclipses(request);

        // Then
        assertNotNull(result);
        List<EclipseIntervalResult> intervals = result.intervals();
        
        // If there are intervals, verify they have proper structure
        for (EclipseIntervalResult interval : intervals) {
            assertNotNull(interval.startIso());
            assertNotNull(interval.endIso());
            assertTrue(interval.durationSeconds() >= 0);
        }
    }
}

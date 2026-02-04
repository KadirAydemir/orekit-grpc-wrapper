package tr.com.kadiraydemir.orekit.model;

import java.util.List;

public record VisibilityResult(
        String satelliteName,
        String stationName,
        List<AccessIntervalResult> intervals) {
}

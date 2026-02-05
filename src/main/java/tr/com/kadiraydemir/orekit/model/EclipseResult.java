package tr.com.kadiraydemir.orekit.model;

import java.util.List;

public record EclipseResult(
        String satelliteName,
        List<EclipseIntervalResult> intervals) {
}

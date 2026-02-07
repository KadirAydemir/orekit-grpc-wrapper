package tr.com.kadiraydemir.orekit.model;

import java.util.List;

public record EclipseResult(
        int noradId,
        List<EclipseIntervalResult> intervals) {
}

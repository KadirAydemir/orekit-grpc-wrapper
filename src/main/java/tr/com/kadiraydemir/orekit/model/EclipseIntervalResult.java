package tr.com.kadiraydemir.orekit.model;

public record EclipseIntervalResult(
        String startIso,
        String endIso,
        double durationSeconds) {
}

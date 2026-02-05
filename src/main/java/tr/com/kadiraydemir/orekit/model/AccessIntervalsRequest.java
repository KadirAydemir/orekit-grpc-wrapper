package tr.com.kadiraydemir.orekit.model;

public record AccessIntervalsRequest(
        String tleLine1,
        String tleLine2,
        String startDateIso,
        String endDateIso,
        GroundStation groundStation,
        double minElevationDegrees) {
}

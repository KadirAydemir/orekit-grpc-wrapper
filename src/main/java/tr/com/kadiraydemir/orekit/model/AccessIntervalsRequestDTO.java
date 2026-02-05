package tr.com.kadiraydemir.orekit.model;

public record AccessIntervalsRequestDTO(
        String tleLine1,
        String tleLine2,
        String startDateIso,
        String endDateIso,
        GroundStationDTO groundStation,
        double minElevationDegrees) {
}

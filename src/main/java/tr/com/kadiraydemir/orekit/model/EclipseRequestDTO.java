package tr.com.kadiraydemir.orekit.model;

public record EclipseRequestDTO(
        String tleLine1,
        String tleLine2,
        String startDateIso,
        String endDateIso) {
}

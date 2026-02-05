package tr.com.kadiraydemir.orekit.model;

public record GroundStationDTO(
        String name,
        double latitudeDegrees,
        double longitudeDegrees,
        double altitudeMeters) {
}

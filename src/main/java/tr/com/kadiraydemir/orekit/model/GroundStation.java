package tr.com.kadiraydemir.orekit.model;

public record GroundStation(
        String name,
        double latitudeDegrees,
        double longitudeDegrees,
        double altitudeMeters) {
}

package tr.com.kadiraydemir.orekit.model;

public record PropagateRequest(
        String satelliteName,
        double semimajorAxis,
        double eccentricity,
        double inclination,
        double perigeeArgument,
        double rightAscensionOfAscendingNode,
        double meanAnomaly,
        String epochIso,
        double duration) {
}

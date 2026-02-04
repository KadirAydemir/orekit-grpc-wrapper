package tr.com.kadiraydemir.orekit.model;

public record TransformResult(
        String sourceFrame,
        String targetFrame,
        String epochIso,
        double x,
        double y,
        double z,
        double vx,
        double vy,
        double vz) {
}

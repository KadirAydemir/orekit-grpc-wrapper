package tr.com.kadiraydemir.orekit.model;

public record TransformRequestDTO(
        ReferenceFrameType sourceFrame,
        ReferenceFrameType targetFrame,
        String epochIso,
        double x,
        double y,
        double z,
        double vx,
        double vy,
        double vz) {
}

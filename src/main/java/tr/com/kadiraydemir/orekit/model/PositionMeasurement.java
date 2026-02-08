package tr.com.kadiraydemir.orekit.model;

/**
 * Represents a single position measurement for TLE fitting.
 *
 * @param timestamp ISO-8601 timestamp of the measurement
 * @param positionX X position in meters (TEME frame)
 * @param positionY Y position in meters (TEME frame)
 * @param positionZ Z position in meters (TEME frame)
 * @param weight    Measurement weight (higher = more trusted)
 * @param sigma     Measurement uncertainty in meters
 */
public record PositionMeasurement(
                String timestamp,
                double positionX,
                double positionY,
                double positionZ,
                double weight,
                double sigma) {

        /**
         * Creates a measurement with default weight (1.0) and sigma (1000.0).
         */
        public static PositionMeasurement of(String timestamp, double x, double y, double z) {
                return new PositionMeasurement(timestamp, x, y, z, 1.0, 1000.0);
        }
}

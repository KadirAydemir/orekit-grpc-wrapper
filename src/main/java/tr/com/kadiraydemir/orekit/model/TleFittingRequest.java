package tr.com.kadiraydemir.orekit.model;

import java.util.List;

/**
 * Request to fit a TLE to a set of position measurements.
 *
 * @param initialTleLine1         Optional initial TLE Line 1 (for SGP4
 *                                template)
 * @param initialTleLine2         Optional initial TLE Line 2 (for SGP4
 *                                template)
 * @param satelliteName           Satellite name for reference
 * @param satelliteNumber         Satellite catalog number (5 digits)
 * @param internationalDesignator International designator (e.g., "1998-067A")
 * @param measurements            List of position measurements to fit against
 * @param convergenceThreshold    Convergence threshold for optimizer (default:
 *                                1.0e-3)
 * @param maxIterations           Maximum iterations for optimizer (default: 25)
 */
public record TleFittingRequest(
                String initialTleLine1,
                String initialTleLine2,
                String satelliteName,
                int satelliteNumber,
                String internationalDesignator,
                List<PositionMeasurement> measurements,
                double convergenceThreshold,
                int maxIterations,
                ReferenceFrameType inputFrame) {

        public TleFittingRequest {
                if (convergenceThreshold <= 0) {
                        convergenceThreshold = 1.0e-3;
                }
                if (maxIterations <= 0) {
                        maxIterations = 25;
                }
        }
}

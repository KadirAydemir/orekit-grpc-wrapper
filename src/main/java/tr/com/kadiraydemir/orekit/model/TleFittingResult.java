package tr.com.kadiraydemir.orekit.model;

/**
 * Result of TLE fitting operation.
 *
 * @param fittedTleLine1 Fitted TLE Line 1
 * @param fittedTleLine2 Fitted TLE Line 2
 * @param satelliteName  Satellite name
 * @param rms            Root Mean Square of residuals in meters
 * @param iterations     Number of iterations performed
 * @param converged      Whether the fit converged successfully
 * @param evaluations    Number of evaluations performed
 * @param error          Error message if fitting failed (null if successful)
 */
public record TleFittingResult(
                String fittedTleLine1,
                String fittedTleLine2,
                String satelliteName,
                double rms,
                int iterations,
                boolean converged,
                int evaluations,
                String error) {

        /**
         * Creates a successful result.
         */
        public static TleFittingResult success(
                        String line1, String line2, String satelliteName,
                        double rms, int iterations, int evaluations) {
                return new TleFittingResult(line1, line2, satelliteName, rms, iterations, true, evaluations, null);
        }

        /**
         * Creates a failed result.
         */
        public static TleFittingResult failure(String error) {
                return new TleFittingResult(null, null, null, 0.0, 0, false, 0, error);
        }
}

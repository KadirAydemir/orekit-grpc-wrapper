package tr.com.kadiraydemir.orekit.utils;

import org.hipparchus.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for TLE (Two-Line Element) parsing operations.
 * Provides methods for extracting satellite information from TLE lines.
 */
public final class TleUtils {

    private static final Logger log = LoggerFactory.getLogger(TleUtils.class);

    // TLE Line 1 field positions (0-indexed)
    private static final int SATELLITE_NUMBER_START = 2;
    private static final int SATELLITE_NUMBER_END = 7;

    private TleUtils() {
        // Utility class - prevent instantiation
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Extracts the satellite ID (NORAD catalog number) from TLE Line 1.
     *
     * TLE Line 1 format (columns 1-7):
     * 1 NNNNNU NNNNNAAA NNNNN.NNNNNNNN +.NNNNNNNN +NNNNN-N +NNNNN-N N NNNNN
     *   ^^^^^
     *   Satellite number (columns 3-7, 0-indexed: 2-7)
     *
     * @param line1 TLE Line 1
     * @return the satellite ID, or 0 if parsing fails
     */
    public static int extractSatelliteId(String line1) {
        if (line1 == null || line1.length() < SATELLITE_NUMBER_END) {
            log.warn("TLE Line 1 is null or too short to extract satellite ID");
            return 0;
        }

        try {
            String satelliteNumberStr = line1.substring(SATELLITE_NUMBER_START, SATELLITE_NUMBER_END).trim();
            return Integer.parseInt(satelliteNumberStr);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse satellite ID from TLE Line 1: {}", line1.substring(0, FastMath.min(line1.length(), 10)));
            return 0;
        } catch (IndexOutOfBoundsException e) {
            log.warn("TLE Line 1 is too short to extract satellite ID");
            return 0;
        }
    }

    /**
     * Validates TLE format by checking basic structure of both lines.
     *
     * @param line1 TLE Line 1
     * @param line2 TLE Line 2
     * @return true if both lines appear to be valid TLE format
     */
    public static boolean isValidTle(String line1, String line2) {
        if (line1 == null || line2 == null) {
            return false;
        }

        // Line 1 should start with '1' and be at least 69 characters
        // Line 2 should start with '2' and be at least 69 characters
        return line1.length() >= 69 && line2.length() >= 69
                && line1.charAt(0) == '1'
                && line2.charAt(0) == '2';
    }

    /**
     * Extracts the classification (U=Unclassified, C=Classified, S=Secret) from TLE Line 1.
     *
     * @param line1 TLE Line 1
     * @return the classification character, or '?' if parsing fails
     */
    public static char extractClassification(String line1) {
        if (line1 == null || line1.length() < 8) {
            return '?';
        }

        char classification = line1.charAt(7);
        return (classification == 'U' || classification == 'C' || classification == 'S')
                ? classification
                : '?';
    }

    /**
     * Extracts the international designator from TLE Line 1.
     *
     * @param line1 TLE Line 1
     * @return the international designator (launch year, launch number, piece), or empty string if parsing fails
     */
    public static String extractInternationalDesignator(String line1) {
        if (line1 == null || line1.length() < 17) {
            return "";
        }

        try {
            return line1.substring(9, 17).trim();
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}

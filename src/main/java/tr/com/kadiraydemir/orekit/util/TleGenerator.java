package tr.com.kadiraydemir.orekit.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility to generate test TLE data for load testing
 */
public class TleGenerator {

    /**
     * Generates a specified number of TLEs and writes them to a file
     *
     * @param count Number of TLEs to generate
     * @param filename Output filename
     * @throws IOException if writing fails
     */
    public static void generateTleFile(int count, String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (int i = 0; i < count; i++) {
                String[] tle = generateTle(i);
                writer.println(tle[0]);
                writer.println(tle[1]);
            }
        }
        System.out.println("Generated " + count + " TLEs to " + filename);
    }

    /**
     * Generates a single TLE pair with realistic orbital parameters
     *
     * @param index Satellite index for generating unique NORAD IDs
     * @return String array with [line1, line2]
     */
    public static String[] generateTle(int index) {
        // NORAD ID: 40000 + index
        int noradId = 40000 + index;

        // Epoch: Calculate day of year
        java.time.LocalDate today = java.time.LocalDate.now();
        int year = today.getYear() % 100;
        int dayOfYear = today.getDayOfYear();
        // Add fractional day (approximate time of day)
        double fractionalDay = 0.5; // midday
        String epoch = String.format("%02d%03d.%08d", year, dayOfYear, (int)(fractionalDay * 100000000));

        // Line 1
        String line1 = String.format(
            "1 %05dU 24001A   %s  .00010000  00000-0  17256-3 0  999%d",
            noradId, epoch, (index % 10)
        );

        // Line 2 with varying orbital parameters
        double inclination = 51.6 + (index % 10) * 0.1;  // 51.6 to 52.5 degrees
        double raan = (index * 3.6) % 360;  // 0 to 360 degrees
        double eccentricity = 0.0001 + (index % 100) * 0.00001;  // Very low
        double argPerigee = (index * 7.2) % 360;
        double meanAnomaly = (index * 5.4) % 360;
        double meanMotion = 15.5 + (index % 5) * 0.1;  // ~15.5 to 15.9 revs/day

        String line2 = String.format(
            "2 %05d %8.4f %8.4f %07d %8.4f %8.4f %11.8f%5d",
            noradId, inclination, raan, (int)(eccentricity * 10000000),
            argPerigee, meanAnomaly, meanMotion, index
        );

        return new String[] {line1, line2};
    }

    /**
     * Generates TLEs in JSON format for gRPC streaming
     *
     * @param count Number of TLEs
     * @param filename Output filename
     * @throws IOException if writing fails
     */
    public static void generateTleJsonFile(int count, String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("{");
            writer.println("  \"tles\": [");

            for (int i = 0; i < count; i++) {
                String[] tle = generateTle(i);
                writer.println("    {");
                writer.println("      \"line1\": \"" + tle[0] + "\",");
                writer.println("      \"line2\": \"" + tle[1] + "\"");
                writer.print("    }");
                if (i < count - 1) {
                    writer.println(",");
                } else {
                    writer.println();
                }
            }

            writer.println("  ]");
            writer.println("}");
        }
        System.out.println("Generated " + count + " TLEs in JSON format to " + filename);
    }

    public static void main(String[] args) throws IOException {
        // Generate different sizes for testing
        generateTleFile(100, "test_tles_100.txt");
        generateTleFile(1000, "test_tles_1k.txt");
        generateTleFile(25000, "test_tles_25k.txt");

        // Also generate JSON version
        generateTleJsonFile(100, "test_tles_100.json");
    }
}

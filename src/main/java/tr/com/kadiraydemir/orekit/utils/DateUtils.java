package tr.com.kadiraydemir.orekit.utils;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import tr.com.kadiraydemir.orekit.exception.OrekitException;

/**
 * Utility class for date parsing and handling
 */
public class DateUtils {

    /**
     * Parses a date string into an AbsoluteDate using UTC time scale.
     * Supports ISO-8601 variations.
     * 
     * @param dateStr   The date string to parse
     * @param fieldName The name of the field (for error messages)
     * @return AbsoluteDate
     * @throws OrekitException if parsing fails
     */
    public static AbsoluteDate parseDate(String dateStr, String fieldName) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new OrekitException(fieldName + " is empty or null");
        }

        // Remove common placeholder text if accidentally sent by clients
        String cleanedStr = dateStr.trim();
        if (cleanedStr.equals("sit minim") || cleanedStr.contains("Lorem ipsum")) {
            throw new OrekitException(
                    "Invalid placeholder date value provided for " + fieldName + ": '" + dateStr + "'");
        }

        try {
            // Attempt standard UTC parsing (handles ISO-8601 like 2024-01-01T12:00:00Z)
            return new AbsoluteDate(cleanedStr, TimeScalesFactory.getUTC());
        } catch (Exception e) {
            try {
                // Fallback: If it has T/Z, try to handle it as a simple string without them
                String fallback = cleanedStr.replace("T", " ").replace("Z", "").trim();
                return new AbsoluteDate(fallback, TimeScalesFactory.getUTC());
            } catch (Exception e2) {
                String message = String.format(
                        "Invalid date format for %s: '%s'. Expected format: YYYY-MM-DDTHH:MM:SSZ.",
                        fieldName, dateStr);
                if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    message += " Details: " + e.getMessage();
                }
                throw new OrekitException(message, e);
            }
        }
    }

    /**
     * Parses a date string or returns a default date if the string is null/empty.
     * 
     * @param dateStr     The date string to parse
     * @param defaultDate The default date to return
     * @param fieldName   The name of the field (for error messages)
     * @return AbsoluteDate
     */
    public static AbsoluteDate parseDateOrDefault(String dateStr, AbsoluteDate defaultDate, String fieldName) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return defaultDate;
        }
        return parseDate(dateStr, fieldName);
    }
}

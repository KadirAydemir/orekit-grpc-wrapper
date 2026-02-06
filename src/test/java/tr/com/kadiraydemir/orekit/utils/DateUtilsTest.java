package tr.com.kadiraydemir.orekit.utils;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import tr.com.kadiraydemir.orekit.exception.OrekitException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("DateUtils Tests")
public class DateUtilsTest {

    @Test
    @DisplayName("Should parse valid ISO-8601 date successfully")
    public void parseDate_validIso8601_returnsAbsoluteDate() {
        // Given
        String dateStr = "2024-01-01T12:00:00Z";
        String fieldName = "epoch";

        // When
        AbsoluteDate result = DateUtils.parseDate(dateStr, fieldName);

        // Then
        assertNotNull(result);
        assertTrue(result.toString().contains("2024-01-01T12:00:00.000"));
    }

    @Test
    @DisplayName("Should throw exception for null date string")
    public void parseDate_null_throwsOrekitException() {
        // Given
        String fieldName = "epoch";

        // Then
        OrekitException exception = assertThrows(OrekitException.class, () -> {
            DateUtils.parseDate(null, fieldName);
        });
        assertTrue(exception.getMessage().contains("epoch is empty or null"));
    }

    @Test
    @DisplayName("Should throw exception for empty date string")
    public void parseDate_empty_throwsOrekitException() {
        // Given
        String fieldName = "startDate";

        // Then
        OrekitException exception = assertThrows(OrekitException.class, () -> {
            DateUtils.parseDate("   ", fieldName);
        });
        assertTrue(exception.getMessage().contains("startDate is empty or null"));
    }

    @Test
    @DisplayName("Should throw exception for placeholder text 'sit minim'")
    public void parseDate_placeholderSitMinim_throwsOrekitException() {
        // Given
        String fieldName = "epoch";

        // Then
        OrekitException exception = assertThrows(OrekitException.class, () -> {
            DateUtils.parseDate("sit minim", fieldName);
        });
        assertTrue(exception.getMessage().contains("Invalid placeholder date value"));
    }

    @Test
    @DisplayName("Should throw exception for placeholder text 'Lorem ipsum'")
    public void parseDate_placeholderLoremIpsum_throwsOrekitException() {
        // Given
        String fieldName = "epoch";

        // Then
        OrekitException exception = assertThrows(OrekitException.class, () -> {
            DateUtils.parseDate("Some Lorem ipsum text here", fieldName);
        });
        assertTrue(exception.getMessage().contains("Invalid placeholder date value"));
    }

    @Test
    @DisplayName("Should throw exception for invalid date format")
    public void parseDate_invalidFormat_throwsOrekitException() {
        // Given
        String fieldName = "startDate";

        // Then
        OrekitException exception = assertThrows(OrekitException.class, () -> {
            DateUtils.parseDate("invalid-date-format", fieldName);
        });
        assertTrue(exception.getMessage().contains("Invalid date format"));
        assertTrue(exception.getMessage().contains("startDate"));
    }

    @Test
    @DisplayName("Should fallback to alternative format when T/Z present")
    public void parseDate_withTZ_fallbackFormat_success() {
        // Given
        String dateStr = "2024-06-15T10:30:45Z";
        String fieldName = "epoch";

        // When
        AbsoluteDate result = DateUtils.parseDate(dateStr, fieldName);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should return default date when input is null")
    public void parseDateOrDefault_null_returnsDefault() {
        // Given
        AbsoluteDate defaultDate = new AbsoluteDate("2024-01-01T00:00:00Z", org.orekit.time.TimeScalesFactory.getUTC());
        String fieldName = "epoch";

        // When
        AbsoluteDate result = DateUtils.parseDateOrDefault(null, defaultDate, fieldName);

        // Then
        assertEquals(defaultDate, result);
    }

    @Test
    @DisplayName("Should return default date when input is empty")
    public void parseDateOrDefault_empty_returnsDefault() {
        // Given
        AbsoluteDate defaultDate = new AbsoluteDate("2024-01-01T00:00:00Z", org.orekit.time.TimeScalesFactory.getUTC());
        String fieldName = "epoch";

        // When
        AbsoluteDate result = DateUtils.parseDateOrDefault("   ", defaultDate, fieldName);

        // Then
        assertEquals(defaultDate, result);
    }

    @Test
    @DisplayName("Should parse date when input is valid")
    public void parseDateOrDefault_validDate_returnsParsed() {
        // Given
        String dateStr = "2024-06-15T10:30:00Z";
        AbsoluteDate defaultDate = new AbsoluteDate("2024-01-01T00:00:00Z", org.orekit.time.TimeScalesFactory.getUTC());
        String fieldName = "epoch";

        // When
        AbsoluteDate result = DateUtils.parseDateOrDefault(dateStr, defaultDate, fieldName);

        // Then
        assertNotNull(result);
        assertNotEquals(defaultDate, result);
    }
}

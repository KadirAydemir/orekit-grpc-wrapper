package tr.com.kadiraydemir.orekit.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TleUtils Unit Tests")
public class TleUtilsTest {

    private static final String VALID_LINE1 = "1 25544U 98067A   24001.50000000  .00000000  00000-0  00000-0 0  0010";
    private static final String VALID_LINE2 = "2 25544  51.6400  100.0000 0007000 100.0000 260.0000 15.50000000  010";

    @Test
    @DisplayName("Should extract satellite ID correctly")
    public void extractSatelliteId_validLine1_returnsId() {
        assertEquals(25544, TleUtils.extractSatelliteId(VALID_LINE1));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1 XXXXX", "Invalid TLE"})
    @DisplayName("Should return 0 for invalid satellite ID input")
    public void extractSatelliteId_invalidInput_returnsZero(String input) {
        assertEquals(0, TleUtils.extractSatelliteId(input));
    }

    @Test
    @DisplayName("Should validate correct TLE lines")
    public void isValidTle_validLines_returnsTrue() {
        assertTrue(TleUtils.isValidTle(VALID_LINE1, VALID_LINE2));
    }

    @ParameterizedTest
    @CsvSource({
            ",",
            "Line1, Line2",
            "1 Short, 2 Short",
            "1 Valid But Short, 2 Valid But Short"
    })
    @DisplayName("Should invalidate incorrect TLE lines")
    public void isValidTle_invalidLines_returnsFalse(String line1, String line2) {
        // Simple mock strings for CSV source, actual logic checks length >= 69 and starts with 1/2
        // We need 69 chars
        String longLine1 = "1 " + "0".repeat(70);
        String longLine2 = "2 " + "0".repeat(70);

        if (line1 == null || line2 == null) {
            assertFalse(TleUtils.isValidTle(line1, line2));
            return;
        }

        // Test short lines
        assertFalse(TleUtils.isValidTle("1 Short", "2 Short"));

        // Test wrong start char
        assertFalse(TleUtils.isValidTle("3" + longLine1.substring(1), longLine2));
        assertFalse(TleUtils.isValidTle(longLine1, "3" + longLine2.substring(1)));
    }

    @Test
    @DisplayName("Should invalidate null lines explicitly")
    public void isValidTle_nullLines_returnsFalse() {
        assertFalse(TleUtils.isValidTle(null, VALID_LINE2));
        assertFalse(TleUtils.isValidTle(VALID_LINE1, null));
    }

    @Test
    @DisplayName("Should extract classification correctly")
    public void extractClassification_validLine1_returnsClassification() {
        assertEquals('U', TleUtils.extractClassification(VALID_LINE1));
    }

    @Test
    public void extractClassification_otherTypes() {
        String classified = "1 25544C 98067A   24001.50000000  .00000000  00000-0  00000-0 0  0010";
        assertEquals('C', TleUtils.extractClassification(classified));

        String secret = "1 25544S 98067A   24001.50000000  .00000000  00000-0  00000-0 0  0010";
        assertEquals('S', TleUtils.extractClassification(secret));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1 25544", "Invalid"})
    @DisplayName("Should return '?' for invalid classification input")
    public void extractClassification_invalidInput_returnsQuestionMark(String input) {
        assertEquals('?', TleUtils.extractClassification(input));
    }

    @Test
    public void extractClassification_unknownChar() {
        String unknown = "1 25544X 98067A   24001.50000000  .00000000  00000-0  00000-0 0  0010";
        assertEquals('?', TleUtils.extractClassification(unknown));
    }

    @Test
    @DisplayName("Should extract international designator correctly")
    public void extractInternationalDesignator_validLine1_returnsDesignator() {
        assertEquals("98067A", TleUtils.extractInternationalDesignator(VALID_LINE1));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1 25544", "TooShort"})
    @DisplayName("Should return empty string for invalid designator input")
    public void extractInternationalDesignator_invalidInput_returnsEmpty(String input) {
        assertEquals("", TleUtils.extractInternationalDesignator(input));
    }

    @Test
    @DisplayName("Should not be instantiable")
    public void constructor_shouldThrowAssertionError() throws Exception {
        java.lang.reflect.Constructor<TleUtils> constructor = TleUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail("Expected InvocationTargetException wrapping AssertionError");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof AssertionError);
        }
    }
}

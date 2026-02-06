package tr.com.kadiraydemir.orekit.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrekitException Tests")
public class OrekitExceptionTest {

    @Test
    @DisplayName("Should create exception with message only")
    public void constructor_withMessage_createsException() {
        // Given
        String message = "Test error message";

        // When
        OrekitException exception = new OrekitException(message);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    public void constructor_withMessageAndCause_createsException() {
        // Given
        String message = "Test error message";
        Throwable cause = new RuntimeException("Original error");

        // When
        OrekitException exception = new OrekitException(message, cause);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should be instance of RuntimeException")
    public void exception_isRuntimeException() {
        // Given
        OrekitException exception = new OrekitException("test");

        // Then
        assertTrue(exception instanceof RuntimeException);
    }
}

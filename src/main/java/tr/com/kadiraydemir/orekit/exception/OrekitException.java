package tr.com.kadiraydemir.orekit.exception;

public class OrekitException extends RuntimeException {
    public OrekitException(String message) {
        super(message);
    }

    public OrekitException(String message, Throwable cause) {
        super(message, cause);
    }
}

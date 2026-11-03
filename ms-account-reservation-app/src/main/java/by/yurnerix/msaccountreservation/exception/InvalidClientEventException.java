package by.yurnerix.msaccountreservation.exception;

public class InvalidClientEventException extends RuntimeException {

    public InvalidClientEventException(String message) {
        super(message);
    }

    public InvalidClientEventException(String message, Throwable cause) {
        super(message, cause);
    }
}

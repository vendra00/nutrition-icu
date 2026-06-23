package t1tanic.nutritionicu.exception;

/**
 * Base type for the application's own domain exceptions. Their message is human-readable and safe to show
 * to the user (no internals), so the global handlers can surface it directly; anything that is NOT an
 * {@code ApplicationException} is treated as unexpected and shown as a generic error (with a logged trace).
 */
public abstract class ApplicationException extends RuntimeException {

    protected ApplicationException(String message) {
        super(message);
    }

    protected ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

package t1tanic.nutritionicu.exception;

/** Caller-supplied input is invalid (maps to HTTP 400). */
public class ValidationException extends ApplicationException {

    public ValidationException(String message) {
        super(message);
    }
}

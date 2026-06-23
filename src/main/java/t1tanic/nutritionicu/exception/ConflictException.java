package t1tanic.nutritionicu.exception;

/** The request conflicts with current state, e.g. a duplicate or an unmet precondition (maps to HTTP 409). */
public class ConflictException extends ApplicationException {

    public ConflictException(String message) {
        super(message);
    }
}

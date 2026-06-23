package t1tanic.nutritionicu.exception;

/** A requested entity does not exist (maps to HTTP 404). */
public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

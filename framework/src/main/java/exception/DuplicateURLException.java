package exception;

/**
 * Exception thrown when a duplicate URL mapping is detected during package scanning.
 */
public class DuplicateURLException extends RuntimeException {
    
    /**
     * Constructs a new DuplicateURLException with the specified detail message.
     *
     * @param message the detail message
     */
    public DuplicateURLException(String message) {
        super(message);
    }
}

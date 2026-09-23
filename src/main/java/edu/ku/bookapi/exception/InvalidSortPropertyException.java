package edu.ku.bookapi.exception;

/**
 * Thrown when a client requests sorting by a field that is not on the
 * whitelist of sortable fields. Mapped to HTTP 400 Bad Request by
 * {@link GlobalExceptionHandler}.
 */
public class InvalidSortPropertyException extends RuntimeException {

    /**
     * Creates an exception for the offending property name.
     *
     * @param property the unknown sort property
     */
    public InvalidSortPropertyException(String property) {
        super("Invalid sort or query property: '" + property + "'.");
    }
}
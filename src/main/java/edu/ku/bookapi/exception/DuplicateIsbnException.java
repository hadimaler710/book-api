package edu.ku.bookapi.exception;

/**
 * Thrown when a book with the same ISBN already exists.
 * Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 */
public class DuplicateIsbnException extends RuntimeException {

    /**
     * Creates an exception for the conflicting ISBN.
     *
     * @param isbn the duplicated (normalized) ISBN
     */
    public DuplicateIsbnException(String isbn) {
        super("A book with ISBN " + isbn + " already exists");
    }
}
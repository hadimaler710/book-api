package edu.ku.bookapi.exception;

/**
 * Thrown when a book with the requested identifier does not exist.
 * Mapped to HTTP 404 Not Found by {@link GlobalExceptionHandler}.
 */
public class BookNotFoundException extends RuntimeException {

    /**
     * Creates an exception for the missing id.
     *
     * @param id the requested book id
     */
    public BookNotFoundException(Long id) {
        super("Book not found with id: " + id);
    }

    /**
     * Creates an exception with a custom message.
     *
     * @param message human-readable reason
     */
    public BookNotFoundException(String message) {
        super(message);
    }
}
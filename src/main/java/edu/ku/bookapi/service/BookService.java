package edu.ku.bookapi.service;

import edu.ku.bookapi.dto.BookRequest;
import edu.ku.bookapi.dto.BookResponse;
import edu.ku.bookapi.dto.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service contract for the library catalogue.
 *
 * <p>Controllers depend on this interface only, which keeps the web layer
 * decoupled from the persistence technology and simplifies mocking in tests.</p>
 */
public interface BookService {

    /**
     * Returns one page of the catalogue.
     *
     * @param pageable page number, size and sort order
     * @return paginated response including metadata
     */
    PageResponse<BookResponse> getAll(Pageable pageable);

    /**
     * Fetches a single book.
     *
     * @param id the book identifier
     * @return the matching book
     * @throws edu.ku.bookapi.exception.BookNotFoundException if no book has the id
     */
    BookResponse getById(Long id);

    /**
     * Creates a new book.
     *
     * @param request validated request payload
     * @return the persisted book
     * @throws edu.ku.bookapi.exception.DuplicateIsbnException if the ISBN already exists
     */
    BookResponse create(BookRequest request);

    /**
     * Updates an existing book.
     *
     * @param id      identifier of the book to update
     * @param request validated request payload
     * @return the updated book
     * @throws edu.ku.bookapi.exception.BookNotFoundException  if no book has the id
     * @throws edu.ku.bookapi.exception.DuplicateIsbnException if the new ISBN belongs to another book
     */
    BookResponse update(Long id, BookRequest request);

    /**
     * Deletes a book.
     *
     * @param id identifier of the book to delete
     * @throws edu.ku.bookapi.exception.BookNotFoundException if no book has the id
     */
    void delete(Long id);
}
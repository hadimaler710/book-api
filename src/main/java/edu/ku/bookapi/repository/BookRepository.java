package edu.ku.bookapi.repository;

import edu.ku.bookapi.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Book} entities.
 *
 * <p>Derived queries are used for ISBN uniqueness checks; everything else is
 * served by the {@link org.springframework.data.domain.Pageable}-aware methods
 * inherited from {@link JpaRepository} (paged and sorted in the database).</p>
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Finds a book by its normalized 13-digit ISBN.
     *
     * @param isbn normalized ISBN
     * @return the book if present
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * Checks whether a book with the given ISBN already exists.
     *
     * @param isbn normalized ISBN
     * @return {@code true} if the ISBN is taken
     */
    boolean existsByIsbn(String isbn);

    /**
     * Checks whether a book other than the given id already uses the ISBN.
     * Used to allow an update to keep its own ISBN while rejecting collisions.
     *
     * @param isbn normalized ISBN
     * @param id   id of the book being updated
     * @return {@code true} if another book owns the ISBN
     */
    boolean existsByIsbnAndIdNot(String isbn, Long id);
}
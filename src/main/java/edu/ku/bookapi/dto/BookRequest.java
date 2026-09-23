package edu.ku.bookapi.dto;

import edu.ku.bookapi.validation.Isbn;
import edu.ku.bookapi.validation.PublishedYear;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound payload for creating and updating books.
 *
 * <p>All input validation happens on this DTO so that JPA entities are never
 * bound directly to the web layer. Every field carries a human-readable
 * message which the {@code GlobalExceptionHandler} forwards to the client.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {

    /** Book title. */
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    /** Author full name. */
    @NotBlank(message = "Author is required")
    @Size(max = 100, message = "Author must not exceed 100 characters")
    private String author;

    /** ISBN-13; hyphens/spaces are tolerated and normalized before persisting. */
    @Isbn
    private String isbn;

    /** Year the book was published. */
    @NotNull(message = "Published year is required")
    @PublishedYear
    private Integer publishedYear;

    /** Category / genre. */
    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
}
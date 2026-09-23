package edu.ku.bookapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outbound representation of a book returned by every endpoint.
 *
 * <p>Exposing a dedicated response DTO keeps the JPA entity (and its auditing
 * columns, proxies and lazy state) hidden from API clients and makes the JSON
 * contract explicit and versionable.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    /** Database identifier. */
    private Long id;

    /** Book title. */
    private String title;

    /** Author full name. */
    private String author;

    /** Normalized 13-digit ISBN. */
    private String isbn;

    /** Year the book was published. */
    private Integer publishedYear;

    /** Category / genre. */
    private String category;

    /** Creation timestamp (set by JPA auditing). */
    private LocalDateTime createdAt;

    /** Last modification timestamp (set by JPA auditing). */
    private LocalDateTime updatedAt;
}
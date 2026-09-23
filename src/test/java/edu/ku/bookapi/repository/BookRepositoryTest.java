package edu.ku.bookapi.repository;

import edu.ku.bookapi.config.PersistenceConfig;
import edu.ku.bookapi.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link BookRepository} against an in-memory H2
 * database (real JPA stack, real SQL, no mocks).
 */
@DataJpaTest
@Import(PersistenceConfig.class)
@ActiveProfiles("test")
class BookRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    private Book book(String isbn) {
        return new Book("Clean Code", "Robert C. Martin", isbn, 2008, "Software Engineering");
    }

    @Test
    void saveAssignsIdAndAuditTimestamps() {
        Book saved = bookRepository.save(book("9780132350884"));
        entityManager.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    @Test
    void findByIdReturnsPersistedBook() {
        Book saved = bookRepository.save(book("9780134685991"));
        entityManager.flush();
        entityManager.clear();

        Optional<Book> found = bookRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Clean Code");
        assertThat(found.get().getIsbn()).isEqualTo("9780134685991");
    }

    @Test
    void existsByIsbnMatchesOnlyExistingIsbn() {
        bookRepository.save(book("9781449373320"));

        assertThat(bookRepository.existsByIsbn("9781449373320")).isTrue();
        assertThat(bookRepository.existsByIsbn("9781617297571")).isFalse();
    }

    @Test
    void existsByIsbnAndIdNotExcludesGivenId() {
        Book saved = bookRepository.save(book("9780132126953"));

        assertThat(bookRepository.existsByIsbnAndIdNot("9780132126953", saved.getId())).isFalse();
        assertThat(bookRepository.existsByIsbnAndIdNot("9780132126953", saved.getId() + 999)).isTrue();
    }

    @Test
    void duplicateIsbnIsRejectedByUniqueConstraint() {
        bookRepository.saveAndFlush(book("9780132350884"));

        assertThatThrownBy(() -> bookRepository.saveAndFlush(book("9780132350884")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updateKeepsCreatedAtAndChangesTitle() {
        Book saved = bookRepository.save(book("9781617297571"));
        entityManager.flush();
        entityManager.clear();

        // Read the creation timestamp straight from the database
        LocalDateTime createdAtInDb = bookRepository.findById(saved.getId()).orElseThrow().getCreatedAt();

        Book managed = bookRepository.findById(saved.getId()).orElseThrow();
        managed.setTitle("Spring in Action (6th Edition)");
        bookRepository.saveAndFlush(managed);
        entityManager.clear();

        Book reloaded = bookRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getTitle()).isEqualTo("Spring in Action (6th Edition)");
        // Auditing must not touch the creation timestamp on update
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAtInDb);
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }
}
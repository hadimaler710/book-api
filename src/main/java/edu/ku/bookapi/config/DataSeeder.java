package edu.ku.bookapi.config;

import edu.ku.bookapi.model.Book;
import edu.ku.bookapi.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds a small demo catalogue on startup (required by the lab:
 * "at least 5 sample books").
 *
 * <p>Seeding runs only when {@code book-api.seed.enabled=true} (dev/test
 * profiles) and only when the database is still empty, so restarting never
 * duplicates data. ISBNs are pre-verified ISBN-13 codes with valid check
 * digits.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final BookRepository bookRepository;
    private final BookProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.getSeed().isEnabled()) {
            log.info("Data seeding disabled via book-api.seed.enabled=false");
            return;
        }
        if (bookRepository.count() > 0) {
            log.info("Catalogue already contains books - skipping seed");
            return;
        }
        List<Book> sampleBooks = List.of(
                book("Clean Code", "Robert C. Martin", "9780132350884", 2008, "Software Engineering"),
                book("Effective Java", "Joshua Bloch", "9780134685991", 2018, "Programming"),
                book("Designing Data-Intensive Applications", "Martin Kleppmann", "9781449373320", 2017, "Distributed Systems"),
                book("Spring in Action", "Craig Walls", "9781617297571", 2022, "Frameworks"),
                book("Computer Networks", "Andrew S. Tanenbaum", "9780132126953", 2010, "Networking"));
        bookRepository.saveAll(sampleBooks);
        log.info("Seeded {} sample books into the catalogue", sampleBooks.size());
    }

    private Book book(String title, String author, String isbn, int year, String category) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setPublishedYear(year);
        book.setCategory(category);
        return book;
    }
}
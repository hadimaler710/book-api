package edu.ku.bookapi.controller;

import edu.ku.bookapi.model.Book;
import edu.ku.bookapi.model.BookInput;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final List<Book> books = new ArrayList<>(List.of(
            new Book(1L, "Java Programming", "John Smith", 5),
            new Book(2L, "Web Development", "Sara Ahmad", 3),
            new Book(3L, "Database Systems", "Ali Khan", 4)
    ));

    // GET /api/v1/books
    @GetMapping
    public List<Book> getAllBooks() {
        return books;
    }

    // PUT /api/v1/books/{bookId}
    @PutMapping("/{bookId}")
    public ResponseEntity<?> updateBook(
            @PathVariable Long bookId,
            @RequestBody BookInput input
    ) {
        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);

            if (book.id().equals(bookId)) {
                Book updatedBook = new Book(
                        book.id(),                  // Keep the original ID
                        input.title(),
                        input.author(),
                        input.availableCopies()
                );

                books.set(i, updatedBook);
                return ResponseEntity.ok(updatedBook);
            }
        }

        return ResponseEntity.notFound().build();
    }

    // DELETE /api/v1/books/{bookId}
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(
            @PathVariable Long bookId
    ) {
        boolean removed = books.removeIf(book -> book.id().equals(bookId));

        if (removed) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}

package edu.ku.bookapi.service;

import edu.ku.bookapi.dto.BookRequest;
import edu.ku.bookapi.dto.BookResponse;
import edu.ku.bookapi.dto.PageResponse;
import edu.ku.bookapi.exception.BookNotFoundException;
import edu.ku.bookapi.exception.DuplicateIsbnException;
import edu.ku.bookapi.exception.InvalidSortPropertyException;
import edu.ku.bookapi.mapper.BookMapper;
import edu.ku.bookapi.model.Book;
import edu.ku.bookapi.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Default {@link BookService} implementation.
 *
 * <p>Caching: {@code books} caches result pages (keyed by the Pageable) and
 * {@code book} caches single books (keyed by id); mutations evict or update
 * them so clients never observe stale data. Reads run as read-only
 * transactions. ISBNs are normalized (hyphens/spaces removed) before
 * persistence and duplicate checks.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookServiceImpl implements BookService {

    /** Whitelist of fields a client may sort by. */
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "title", "author", "isbn", "publishedYear", "category", "createdAt", "updatedAt");

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    /**
     * Removes hyphens and spaces so "978-0-13-235088-4" and "9780132350884"
     * are treated as the same ISBN.
     */
    static String normalizeIsbn(String raw) {
        return raw == null ? null : raw.replaceAll("[\\s-]+", "");
    }

    /** Rejects unknown sort fields with a 400 instead of a late 500. */
    private static void validateSort(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidSortPropertyException(order.getProperty());
            }
        }
    }

    @Override
    @Cacheable(cacheNames = "books", key = "#pageable")
    @Transactional(readOnly = true)
    public PageResponse<BookResponse> getAll(Pageable pageable) {
        validateSort(pageable);
        log.debug("Fetching books: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        Page<Book> page = bookRepository.findAll(pageable);
        PageResponse<BookResponse> response = PageResponse.from(page.map(bookMapper::toResponse));
        log.debug("Fetched {} books on page {} of {}",
                response.content().size(), response.page(), response.totalPages());
        return response;
    }

    @Override
    @Cacheable(cacheNames = "book", key = "#id")
    @Transactional(readOnly = true)
    public BookResponse getById(Long id) {
        log.debug("Fetching book id={}", id);
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        return bookMapper.toResponse(book);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "books", allEntries = true)
    public BookResponse create(BookRequest request) {
        String isbn = normalizeIsbn(request.getIsbn());
        if (bookRepository.existsByIsbn(isbn)) {
            log.warn("Rejected book creation - duplicate ISBN {}", isbn);
            throw new DuplicateIsbnException(isbn);
        }
        Book book = bookMapper.toEntity(request);
        book.setIsbn(isbn);
        Book saved = bookRepository.save(book);
        log.info("Book created: id={}, title='{}', isbn={}", saved.getId(), saved.getTitle(), saved.getIsbn());
        return bookMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "book", key = "#id"),
            evict = @CacheEvict(cacheNames = "books", allEntries = true)
    )
    public BookResponse update(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        String isbn = normalizeIsbn(request.getIsbn());
        if (bookRepository.existsByIsbnAndIdNot(isbn, id)) {
            log.warn("Rejected book update id={} - ISBN {} already in use", id, isbn);
            throw new DuplicateIsbnException(isbn);
        }
        bookMapper.updateEntity(request, book);
        book.setIsbn(isbn);
        Book saved = bookRepository.save(book);
        log.info("Book updated: id={}, title='{}', isbn={}", id, saved.getTitle(), saved.getIsbn());
        return bookMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "book", key = "#id"),
            @CacheEvict(cacheNames = "books", allEntries = true)
    })
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        bookRepository.deleteById(id);
        log.info("Book deleted: id={}", id);
    }
}
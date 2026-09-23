package edu.ku.bookapi.service;

import edu.ku.bookapi.dto.BookRequest;
import edu.ku.bookapi.dto.BookResponse;
import edu.ku.bookapi.dto.PageResponse;
import edu.ku.bookapi.exception.BookNotFoundException;
import edu.ku.bookapi.exception.DuplicateIsbnException;
import edu.ku.bookapi.mapper.BookMapper;
import edu.ku.bookapi.model.Book;
import edu.ku.bookapi.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BookServiceImpl} using Mockito only (no Spring
 * context, no database) - the service logic is verified in isolation.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book book;
    private BookRequest request;

    @BeforeEach
    void setUp() {
        book = new Book("Clean Code", "Robert C. Martin", "9780132350884", 2008, "Software Engineering");
        book.setId(1L);
        book.setCreatedAt(LocalDateTime.now().minusDays(1));
        book.setUpdatedAt(LocalDateTime.now());

        request = BookRequest.builder()
                .title("Refactoring")
                .author("Martin Fowler")
                .isbn("978-0-13-475759-9")
                .publishedYear(2018)
                .category("Programming")
                .build();
    }

    /** Generic mapping stub: response mirrors whatever entity it receives. */
    private void stubMapper() {
        when(bookMapper.toResponse(any(Book.class))).thenAnswer(invocation -> {
            Book source = invocation.getArgument(0);
            return BookResponse.builder()
                    .id(source.getId())
                    .title(source.getTitle())
                    .author(source.getAuthor())
                    .isbn(source.getIsbn())
                    .publishedYear(source.getPublishedYear())
                    .category(source.getCategory())
                    .createdAt(source.getCreatedAt())
                    .updatedAt(source.getUpdatedAt())
                    .build();
        });
    }

    @Test
    void getAllReturnsPagedResponse() {
        Book second = new Book("Effective Java", "Joshua Bloch", "9780134685991", 2018, "Programming");
        second.setId(2L);
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id"));
        when(bookRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(book, second), pageable, 5));
        stubMapper();

        PageResponse<BookResponse> result = bookService.getAll(pageable);

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.first()).isTrue();
        assertThat(result.hasNext()).isTrue();
        verify(bookRepository).findAll(pageable);
    }

    @Test
    void getByIdReturnsBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        stubMapper();

        assertThat(bookService.getById(1L).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getById(99L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createNormalizesIsbnAndSaves() {
        when(bookRepository.existsByIsbn("9780134757599")).thenReturn(false);
        when(bookMapper.toEntity(request)).thenReturn(new Book());
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
        stubMapper();

        BookResponse response = bookService.create(request);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertThat(captor.getValue().getIsbn()).isEqualTo("9780134757599");
        assertThat(response.getIsbn()).isEqualTo("9780134757599");
    }

    @Test
    void createThrowsOnDuplicateIsbn() {
        when(bookRepository.existsByIsbn("9780134757599")).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(request))
                .isInstanceOf(DuplicateIsbnException.class)
                .hasMessageContaining("9780134757599");
        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateCopiesRequestOntoManagedEntity() {
        Book managed = new Book("Old Title", "Old Author", "9780132350884", 2000, "Old");
        managed.setId(1L);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(managed));
        when(bookRepository.existsByIsbnAndIdNot("9780134757599", 1L)).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
        stubMapper();

        BookResponse response = bookService.update(1L, request);

        verify(bookMapper).updateEntity(request, managed);
        assertThat(managed.getIsbn()).isEqualTo("9780134757599");
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void updateThrowsWhenMissing() {
        when(bookRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(42L, request))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void updateThrowsWhenIsbnOwnedByOtherBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.existsByIsbnAndIdNot("9780134757599", 1L)).thenReturn(true);

        assertThatThrownBy(() -> bookService.update(1L, request))
                .isInstanceOf(DuplicateIsbnException.class);
        verify(bookRepository, never()).save(any());
    }

    @Test
    void deleteRemovesBook() {
        when(bookRepository.existsById(1L)).thenReturn(true);

        bookService.delete(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(bookRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.delete(7L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("7");
        verify(bookRepository, never()).deleteById(any());
    }

    @Test
    void normalizeIsbnStripsSeparators() {
        assertThat(BookServiceImpl.normalizeIsbn("978-0-13-235088-4")).isEqualTo("9780132350884");
        assertThat(BookServiceImpl.normalizeIsbn(" 978 0132350884 ")).isEqualTo("9780132350884");
        assertThat(BookServiceImpl.normalizeIsbn("9780132350884")).isEqualTo("9780132350884");
    }
}
package edu.ku.bookapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ku.bookapi.dto.BookRequest;
import edu.ku.bookapi.dto.BookResponse;
import edu.ku.bookapi.dto.PageResponse;
import edu.ku.bookapi.exception.BookNotFoundException;
import edu.ku.bookapi.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer tests for {@link BookController} with a mocked {@link BookService}. */
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService bookService;

    private BookResponse cleanCode;
    private BookResponse effectiveJava;

    @BeforeEach
    void setUp() {
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 10, 0);
        cleanCode = BookResponse.builder()
                .id(1L).title("Clean Code").author("Robert C. Martin")
                .isbn("9780132350884").publishedYear(2008).category("Software Engineering")
                .createdAt(created).updatedAt(created).build();
        effectiveJava = BookResponse.builder()
                .id(2L).title("Effective Java").author("Joshua Bloch")
                .isbn("9780134685991").publishedYear(2018).category("Programming")
                .createdAt(created).updatedAt(created).build();
    }

    private static PageResponse<BookResponse> page(List<BookResponse> content, long total) {
        return new PageResponse<>(content, 0, Math.max(content.size(), 1), total, 1,
                true, true, false, false, "id,asc");
    }

    private static BookRequest validRequest() {
        return BookRequest.builder()
                .title("Refactoring").author("Martin Fowler")
                .isbn("9780134757599").publishedYear(2018).category("Programming")
                .build();
    }

    private String json(BookRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    @Test
    void getAllReturns200WithContent() throws Exception {
        when(bookService.getAll(any(Pageable.class)))
                .thenReturn(page(List.of(cleanCode, effectiveJava), 2));

        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.content[1].isbn").value("9780134685991"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllAppliesPaginationParameters() throws Exception {
        when(bookService.getAll(any(Pageable.class))).thenReturn(page(List.of(), 0));

        mockMvc.perform(get("/api/v1/books")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(bookService).getAll(argThat(p ->
                p.getPageNumber() == 0
                        && p.getPageSize() == 5
                        && p.getSort().getOrderFor("title") != null
                        && p.getSort().getOrderFor("title").isAscending()));
    }

    @Test
    void getByIdReturns200() throws Exception {
        when(bookService.getById(1L)).thenReturn(cleanCode);

        mockMvc.perform(get("/api/v1/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getByIdReturns404WithErrorBody() throws Exception {
        when(bookService.getById(99L)).thenThrow(new BookNotFoundException(99L));

        mockMvc.perform(get("/api/v1/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Book not found with id: 99"))
                .andExpect(jsonPath("$.path").value("/api/v1/books/99"));
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        BookResponse created = BookResponse.builder()
                .id(42L).title("Refactoring").author("Martin Fowler")
                .isbn("9780134757599").publishedYear(2018).category("Programming")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(bookService.create(any(BookRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/books/42")))
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void createRejectsInvalidPayloadWithFieldErrors() throws Exception {
        BookRequest invalid = BookRequest.builder()
                .title("").author("A".repeat(150)).isbn("123").publishedYear(1800).category("")
                .build();

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed. Check the 'validationErrors' field."))
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.author").exists())
                .andExpect(jsonPath("$.validationErrors.isbn").exists())
                .andExpect(jsonPath("$.validationErrors.publishedYear").exists())
                .andExpect(jsonPath("$.validationErrors.category").exists());
    }

    @Test
    void createRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body or invalid value types."));
    }

    @Test
    void createRejectsDuplicateIsbn() throws Exception {
        when(bookService.create(any(BookRequest.class)))
                .thenThrow(new edu.ku.bookapi.exception.DuplicateIsbnException("9780134757599"));

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A book with ISBN 9780134757599 already exists"));
    }

    @Test
    void updateReturns200WithUpdatedBody() throws Exception {
        BookResponse updated = BookResponse.builder()
                .id(1L).title("Clean Code (2nd Edition)").author("Robert C. Martin")
                .isbn("9780132350884").publishedYear(2008).category("Software Engineering")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(bookService.update(eq(1L), any(BookRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code (2nd Edition)"));
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        when(bookService.update(eq(99L), any(BookRequest.class)))
                .thenThrow(new BookNotFoundException(99L));

        mockMvc.perform(put("/api/v1/books/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRejectsInvalidPayload() throws Exception {
        BookRequest invalid = BookRequest.builder()
                .title("X").author("Y").isbn("9780134757599").publishedYear(1800).category("Z")
                .build();

        mockMvc.perform(put("/api/v1/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.publishedYear").exists());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/books/1"))
                .andExpect(status().isNoContent());

        verify(bookService).delete(1L);
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        org.mockito.Mockito.doThrow(new BookNotFoundException(99L)).when(bookService).delete(99L);

        mockMvc.perform(delete("/api/v1/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found with id: 99"));
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/books/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'not-a-number' for parameter 'id'."));
    }
}
package edu.ku.bookapi.exception;

import edu.ku.bookapi.model.Book;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for every branch of {@link GlobalExceptionHandler}: verifies the
 * HTTP status codes and the uniform {@link ErrorResponse} body.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/v1/books/1");
    }

    @Test
    void bookNotFoundReturns404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBookNotFound(new BookNotFoundException(1L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).contains("1");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/books/1");
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getValidationErrors()).isNull();
    }

    @Test
    void duplicateIsbnReturns409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleDuplicateIsbn(new DuplicateIsbnException("9780132350884"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).contains("9780132350884");
    }

    @Test
    void methodArgumentNotValidReturns400WithFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "bookRequest");
        bindingResult.addError(new FieldError("bookRequest", "title", "Title is required"));
        bindingResult.addError(new FieldError("bookRequest", "isbn", "ISBN must be valid"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getValidationErrors()).containsEntry("title", "Title is required");
        assertThat(response.getBody().getValidationErrors()).containsEntry("isbn", "ISBN must be valid");
    }

    @Test
    void constraintViolationReturns400() {
        ConstraintViolationException ex =
                new ConstraintViolationException("invalid", Collections.emptySet());

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unreadableBodyReturns400() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("bad json", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableMessage(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Malformed JSON");
    }

    @Test
    void typeMismatchReturns400() throws NoSuchMethodException {
        var parameter = new org.springframework.core.MethodParameter(
                getClass().getDeclaredMethod("sample", String.class), 0);
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", parameter, null);

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("abc").contains("id");
    }

    @SuppressWarnings("unused")
    private void sample(String value) {
        // only used to build a MethodParameter in tests
    }

    @Test
    void propertyReferenceReturns400() {
        PropertyReferenceException ex = new PropertyReferenceException("banana",
                org.springframework.data.util.TypeInformation.of(Book.class), Collections.emptyList());

        ResponseEntity<ErrorResponse> response = handler.handlePropertyReference(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("banana");
    }

    @Test
    void noResourceReturns404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "nope");

        ResponseEntity<ErrorResponse> response = handler.handleNoResource(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).contains("GET");
    }

    @Test
    void methodNotSupportedReturns405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("PATCH");

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void dataIntegrityReturns409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrity(new DataIntegrityViolationException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).contains("Data integrity violation");
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new IllegalStateException("secret internals"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).doesNotContain("secret internals");
    }
}
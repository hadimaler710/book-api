package edu.ku.bookapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central exception handling for every REST endpoint.
 *
 * <p>Translates technical and domain exceptions into consistent HTTP status
 * codes with a uniform {@link ErrorResponse} body:</p>
 *
 * <ul>
 *     <li>400 Bad Request - validation, malformed JSON, bad path variables, invalid sort</li>
 *     <li>404 Not Found - missing books or unknown endpoints</li>
 *     <li>405 Method Not Allowed - wrong HTTP verb</li>
 *     <li>409 Conflict - duplicate ISBN / integrity violations</li>
 *     <li>500 Internal Server Error - unexpected failures (details only in the log)</li>
 * </ul>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles "book does not exist" errors (HTTP 404).
     */
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookNotFound(BookNotFoundException ex, HttpServletRequest request) {
        log.warn("404 {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /**
     * Handles duplicate-ISBN conflicts (HTTP 409).
     */
    @ExceptionHandler(DuplicateIsbnException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateIsbn(DuplicateIsbnException ex, HttpServletRequest request) {
        log.warn("409 {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    /**
     * Handles {@code @Valid @RequestBody} failures (HTTP 400) and reports
     * every invalid field.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        log.warn("400 {}: validation failed for fields {}", request.getRequestURI(), fieldErrors.keySet());
        return build(HttpStatus.BAD_REQUEST, "Validation failed. Check the 'validationErrors' field.",
                request, fieldErrors);
    }

    /**
     * Handles constraint violations outside the request body (HTTP 400).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        Map<String, String> violations = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            violations.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage());
        }
        log.warn("400 {}: constraint violation(s) {}", request.getRequestURI(), violations.keySet());
        return build(HttpStatus.BAD_REQUEST, "Validation failed. Check the 'validationErrors' field.",
                request, violations);
    }

    /**
     * Handles malformed JSON bodies and wrong value types (HTTP 400).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex,
                                                                 HttpServletRequest request) {
        log.warn("400 {}: unreadable request body ({})", request.getRequestURI(),
                ex.getMostSpecificCause().getClass().getSimpleName());
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request body or invalid value types.",
                request, null);
    }

    /**
     * Handles path variables that cannot be converted, e.g. {@code /books/abc} (HTTP 400).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String message = String.format("Invalid value '%s' for parameter '%s'.", ex.getValue(), ex.getName());
        log.warn("400 {}: {}", request.getRequestURI(), message);
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    /**
     * Handles unknown sort/query properties, e.g. {@code ?sort=banana,asc} (HTTP 400).
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(PropertyReferenceException ex,
                                                                 HttpServletRequest request) {
        String message = String.format("Invalid sort or query property: '%s'.", ex.getPropertyName());
        log.warn("400 {}: {}", request.getRequestURI(), message);
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    /**
     * Handles sort fields rejected by the service whitelist (HTTP 400).
     */
    @ExceptionHandler(InvalidSortPropertyException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSortProperty(InvalidSortPropertyException ex,
                                                                   HttpServletRequest request) {
        log.warn("400 {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    /**
     * Handles requests to paths that no controller serves (HTTP 404).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        String message = String.format("No endpoint for %s %s.", ex.getHttpMethod(), ex.getResourcePath());
        log.warn("404 {}: {}", request.getRequestURI(), message);
        return build(HttpStatus.NOT_FOUND, message, request, null);
    }

    /**
     * Handles unsupported HTTP verbs with the proper 405 status.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        String message = String.format("HTTP method '%s' is not supported for this endpoint.", ex.getMethod());
        log.warn("405 {}: {}", request.getRequestURI(), message);
        return build(HttpStatus.METHOD_NOT_ALLOWED, message, request, null);
    }

    /**
     * Safety net for database-level integrity violations that bypass the
     * service-level checks (HTTP 409), e.g. concurrent inserts of the same ISBN.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        log.warn("409 {}: data integrity violation", request.getRequestURI());
        return build(HttpStatus.CONFLICT,
                "Data integrity violation: the request conflicts with existing data (e.g. duplicate ISBN).",
                request, null);
    }

    /**
     * Last-resort handler (HTTP 500): logs the full stack trace server-side and
     * returns a generic message so internals never leak to clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("500 {}: unexpected error", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred. Please contact the administrator.",
                request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
                                                Map<String, String> validationErrors) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .validationErrors(validationErrors == null || validationErrors.isEmpty() ? null : validationErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}

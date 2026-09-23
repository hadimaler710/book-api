package edu.ku.bookapi.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform error body returned by every failure handled by
 * {@link GlobalExceptionHandler}.
 *
 * <p>{@code validationErrors} is only present for bean-validation failures and
 * maps field names to their first error message. Null fields are omitted from
 * the JSON.</p>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** Server-side time of the failure. */
    private final LocalDateTime timestamp;

    /** HTTP status code. */
    private final int status;

    /** HTTP status reason phrase, e.g. "Not Found". */
    private final String error;

    /** Human-readable description of the problem. */
    private final String message;

    /** Request URI that caused the failure. */
    private final String path;

    /** Field-level validation errors (field name -> message), if any. */
    private final Map<String, String> validationErrors;
}
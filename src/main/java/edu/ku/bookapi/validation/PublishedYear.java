package edu.ku.bookapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a publication year lies between {@link #min()} (default 1900)
 * and the current year. Unlike a hard-coded {@code @Max(2026)} this keeps
 * working in future years without code changes.
 *
 * <p>{@code null} is considered valid here; pair with {@code @NotNull} for an
 * explicit "required" message.</p>
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PublishedYearValidator.class)
public @interface PublishedYear {

    /** Smallest allowed publication year. */
    int min() default 1900;

    /** Error message shown when validation fails. */
    String message() default "Published year must be between 1900 and the current year";

    /** Bean Validation groups. */
    Class<?>[] groups() default {};

    /** Bean Validation payload. */
    Class<? extends Payload>[] payload() default {};
}
package edu.ku.bookapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a value is a well-formed ISBN-13.
 *
 * <p>Hyphens and spaces are tolerated (e.g. {@code 978-0-13-235088-4}) and the
 * GS1 modulo-10 check digit is verified. {@code null} or blank values are
 * invalid; pair with {@code @NotBlank} for an explicit "required" message.</p>
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IsbnValidator.class)
public @interface Isbn {

    /** Error message shown when validation fails. */
    String message() default "ISBN must be a valid 13-digit ISBN-13 (hyphens and spaces are allowed)";

    /** Bean Validation groups. */
    Class<?>[] groups() default {};

    /** Bean Validation payload. */
    Class<? extends Payload>[] payload() default {};
}
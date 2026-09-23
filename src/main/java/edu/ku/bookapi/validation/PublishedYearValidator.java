package edu.ku.bookapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Year;

/**
 * Implementation of the {@link PublishedYear} constraint: the year must be at
 * least {@link #min()} and at most the current calendar year.
 */
public class PublishedYearValidator implements ConstraintValidator<PublishedYear, Integer> {

    private int min;

    @Override
    public void initialize(PublishedYear annotation) {
        this.min = annotation.min();
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null is handled by @NotNull with a dedicated message
        }
        return value >= min && value <= Year.now().getValue();
    }
}
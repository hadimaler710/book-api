package edu.ku.bookapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Implementation of the {@link Isbn} constraint.
 *
 * <p>Rules enforced:</p>
 * <ol>
 *     <li>Exactly 13 digits after optional hyphens/spaces are removed.</li>
 *     <li>The GS1 modulo-10 check digit is correct (weights 1 and 3,
 *     alternating from the left).</li>
 * </ol>
 */
public class IsbnValidator implements ConstraintValidator<Isbn, String> {

    private static final Pattern THIRTEEN_DIGITS = Pattern.compile("\\d{13}");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String digits = value.replaceAll("[\\s-]+", "");
        if (!THIRTEEN_DIGITS.matcher(digits).matches()) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 13; i++) {
            int digit = digits.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return sum % 10 == 0;
    }
}
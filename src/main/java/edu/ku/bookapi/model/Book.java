package edu.ku.bookapi.model;

public record Book(
        Long id,
        String title,
        String author,
        int availableCopies
) {
}

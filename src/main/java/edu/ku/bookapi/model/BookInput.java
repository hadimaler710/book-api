package edu.ku.bookapi.model;

public record BookInput(
        String title,
        String author,
        int availableCopies
) {
}

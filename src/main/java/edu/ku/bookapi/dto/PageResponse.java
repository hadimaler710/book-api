package edu.ku.bookapi.dto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * A stable, framework-agnostic pagination envelope.
 *
 * <p>Serializing Spring's {@code PageImpl} directly is fragile (breaking
 * changes between Spring Data versions and accidental exposure of internals),
 * so every paged endpoint is wrapped in this DTO instead. {@code content} holds
 * the actual page of items; the remaining fields are the pagination metadata.</p>
 *
 * @param <T> type of the page items
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious,
        String sort) {

    /**
     * Creates the envelope from a Spring Data {@link Page}.
     *
     * @param page the page returned by the repository/service
     * @param <T>  item type
     * @return pagination envelope with metadata
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious(),
                describe(page.getSort()));
    }

    private static String describe(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return "unsorted";
        }
        return sort.stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .reduce((a, b) -> a + ";" + b)
                .orElse("unsorted");
    }
}
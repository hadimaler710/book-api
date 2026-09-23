package edu.ku.bookapi.mapper;

import edu.ku.bookapi.dto.BookRequest;
import edu.ku.bookapi.dto.BookResponse;
import edu.ku.bookapi.model.Book;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper between the {@link Book} entity and its DTOs.
 *
 * <p>The implementation is generated at compile time (no reflection), which
 * keeps mapping fast and type-safe. The generated class is wired as a Spring
 * bean via {@code componentModel = SPRING}.</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BookMapper {

    /**
     * Maps an entity to its API response representation.
     *
     * @param book the persisted entity
     * @return the response DTO (including auditing timestamps)
     */
    BookResponse toResponse(Book book);

    /**
     * Maps a validated request to a fresh entity for insertion.
     * Auditing columns and the id are populated by JPA/auditing, not by the caller.
     *
     * @param request the validated request payload
     * @return a new, unpersisted entity
     */
    Book toEntity(BookRequest request);

    /**
     * Copies request values onto an existing (managed) entity for updates.
     *
     * <p>{@code null} request fields are ignored so a partial payload cannot
     * wipe existing data, and auditing/id fields are left untouched.</p>
     *
     * @param request the validated request payload
     * @param book    the managed entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(BookRequest request, @MappingTarget Book book);
}
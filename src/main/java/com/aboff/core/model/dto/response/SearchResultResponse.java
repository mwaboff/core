package com.aboff.core.model.dto.response;

import com.aboff.core.model.enums.SearchableEntityType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single search result returned by the full-text search API.
 * <p>
 * Each instance corresponds to a matched entity from the search index and contains
 * the entity's type, primary identifier, display name, relevance score, and an
 * optional expanded representation of the full entity.
 * </p>
 *
 * <p>
 * The {@code expandedEntity} field is only populated when the caller requests
 * expansion (e.g., {@code ?expand=entity}). It holds the fully hydrated response
 * object for the matched entity so that clients can avoid a second round-trip.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResultResponse {

    /**
     * The type of the matched entity (e.g., WEAPON, DOMAIN_CARD).
     */
    private SearchableEntityType type;

    /**
     * The primary key of the matched entity in its own table.
     */
    private Long id;

    /**
     * The display name of the matched entity.
     */
    private String name;

    /**
     * The relevance score assigned by the search engine (higher is more relevant).
     * May be {@code null} when relevance ranking is not applicable.
     */
    private Double relevanceScore;

    /**
     * The fully hydrated entity response object, populated only when expansion is requested.
     * The concrete type depends on the matched {@link #type}.
     */
    private Object expandedEntity;
}

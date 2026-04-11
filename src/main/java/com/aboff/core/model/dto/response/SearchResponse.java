package com.aboff.core.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated wrapper DTO for full-text search results.
 * <p>
 * Encapsulates a page of {@link SearchResultResponse} objects along with pagination
 * metadata and the original query string so that clients can reconstruct or continue
 * a search session without maintaining separate state.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Returned by the search API endpoint for queries such as:
 * </p>
 * <pre>
 * GET /api/search?q=flame+sword&amp;page=0&amp;size=20
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {

    /**
     * The list of matched search results for the current page.
     */
    private List<SearchResultResponse> results;

    /**
     * Total number of matching entities across all pages.
     */
    private long totalElements;

    /**
     * Total number of pages based on the page size.
     */
    private int totalPages;

    /**
     * Zero-based index of the current page.
     */
    private int currentPage;

    /**
     * Maximum number of results per page.
     */
    private int pageSize;

    /**
     * The original search query string submitted by the caller.
     */
    private String query;
}

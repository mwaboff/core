package com.aboff.core.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic wrapper for paginated API responses.
 * Provides standardized pagination metadata along with the content list.
 *
 * @param <T> The type of objects in the content list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    /**
     * The list of items for the current page
     */
    private List<T> content;

    /**
     * Total number of elements across all pages
     */
    private long totalElements;

    /**
     * Total number of pages based on the page size
     */
    private int totalPages;

    /**
     * Current page number (zero-based)
     */
    private int currentPage;

    /**
     * Number of items per page
     */
    private int pageSize;
}

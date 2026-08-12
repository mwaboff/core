package com.aboff.core.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of a {@code PATCH /api/admin/content/srd} batch.
 * <p>
 * Ids in the request that do not resolve to a row of {@code type} are reported in
 * {@code unknownIds} rather than failing the whole batch — the rows that did resolve are
 * still updated. This is what lets the admin UI show a partial-success summary.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSrdUpdateResponse {

    /** The content type the batch was applied to. */
    private String type;

    /** The srd value the batch applied. */
    private Boolean srd;

    /** Ids from the request that were found and updated. */
    private List<Long> updatedIds;

    /** Ids from the request that do not exist for {@code type}. */
    private List<Long> unknownIds;
}

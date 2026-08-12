package com.aboff.core.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Body of {@code PATCH /api/admin/content/srd} — the bulk SRD-flagging tool.
 * <p>
 * {@code type} is a raw string rather than a {@link com.aboff.core.model.enums.SearchableEntityType}
 * so an unrecognized value fails with the service layer's own descriptive 400 rather than
 * Jackson's opaque enum-binding error message. Matches the type-key strings the admin card
 * search UI already sends (the {@code SearchableEntityType} constant name, e.g.
 * {@code "WEAPON"}, {@code "SUBCLASS_CARD"}).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSrdUpdateRequest {

    /** The content type key, matching a {@link com.aboff.core.model.enums.SearchableEntityType} name. */
    @NotNull(message = "Type is required")
    private String type;

    /** The ids to flag or unflag. Ids that do not exist are reported back, not rejected. */
    @NotEmpty(message = "At least one id is required")
    private List<Long> ids;

    /** {@code true} to mark the matched rows SRD, {@code false} to unmark them. */
    @NotNull(message = "srd is required")
    private Boolean srd;
}

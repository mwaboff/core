package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.CostTagCategory;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for CardCostTag entities.
 * Represents a cost, limitation, or timing tag that can be displayed on cards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardCostTagResponse implements Restrictable {
    /**
     * Unique identifier for the cost tag
     */
    private Long id;

    /**
     * Display label for the cost tag (e.g., "3 Hope", "1/session")
     */
    private String label;

    /**
     * Category of the cost tag (COST, LIMITATION, TIMING)
     */
    private CostTagCategory category;

    /**
     * Whether this cost tag is SRD-licensed content, freely usable without owning the
     * sourcebook it was printed in.
     */
    private Boolean srd;

    /**
     * Timestamp when the cost tag was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the cost tag was last modified
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the cost tag was soft-deleted (null if not deleted)
     */
    private LocalDateTime deletedAt;

    /**
     * The name of the expansion this cost tag belongs to. Only set on a redacted stub — a
     * CardCostTag carries no expansion of its own, so this is always null in practice.
     */
    private String expansionName;

    /**
     * True if this response is a redacted stub for gated non-SRD content the caller may not
     * view. When true, every other field except {@link #id} and {@link #expansionName} is
     * unset.
     */
    private Boolean restricted;

    /**
     * Restrictable's setter for the cost tag's display name — a CardCostTagResponse has no
     * {@code name} field (it uses {@link #label}), so this is a no-op. Never called by
     * {@link com.aboff.core.util.ContentRedaction#stub}, which never sets a name on a stub.
     *
     * @param name unused
     */
    @Override
    public void setName(String name) {
        // No-op: CardCostTagResponse has no "name" field.
    }
}

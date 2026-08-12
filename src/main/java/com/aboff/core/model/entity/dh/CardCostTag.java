package com.aboff.core.model.entity.dh;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.model.enums.SearchableEntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Entity representing a cost or limitation tag for cards in the Daggerheart TTRPG system.
 * <p>
 * Cost tags are display strings rendered as badges/chips on card views to convey
 * cost, limitation, or timing information (e.g., "3 Hope", "1/session", "Close range").
 * Tags are shared across cards via a many-to-many relationship.
 * </p>
 */
@Entity
@SearchIndexed(type = SearchableEntityType.CARD_COST_TAG)
@Table(name = "card_cost_tags")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardCostTag extends BaseEntity {

    /**
     * The display label for the cost tag (e.g., "3 Hope", "1/session").
     */
    @Column(nullable = false, length = 200)
    private String label;

    /**
     * The category of this cost tag, used for frontend grouping and styling.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CostTagCategory category;

    /**
     * Indicates whether this cost tag is SRD-licensed content, freely usable without owning
     * the sourcebook it was printed in. Defaults to false at creation time; only an explicit
     * SRD flag opens the cost tag to users who have not been granted expansion access. See
     * {@code ContentAccessService} for how this is enforced.
     */
    @Column(name = "srd", nullable = false)
    @Builder.Default
    private Boolean srd = false;

    /**
     * Timestamp indicating when this cost tag was soft-deleted.
     * If null, the cost tag is active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this cost tag has been soft-deleted.
     *
     * @return true if the cost tag is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the cost tag by setting the deleted_at timestamp.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted cost tag.
     */
    public void restore() {
        this.deletedAt = null;
    }
}

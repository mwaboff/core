package com.aboff.core.model.entity.dh;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.SearchableEntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a transformation card in the Daggerheart TTRPG system.
 * <p>
 * Transformation cards are a standalone content type, modeled the same way as
 * {@link Class} or {@link Domain} — they are <strong>not</strong> a {@link Card} subtype and
 * are <strong>not</strong> {@link DomainCard} rows. They deliberately have no relationship to
 * the {@code cards}/{@code domain_cards} tables, so creating or holding transformation cards
 * must never count against a character's 5-card domain-card loadout cap.
 * </p>
 */
@Entity
@SearchIndexed(type = SearchableEntityType.TRANSFORMATION_CARD)
@Table(name = "transformation_cards")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransformationCard extends BaseEntity {

    /**
     * The name of the transformation card.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Detailed description of the transformation card and its effects.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The expansion this transformation card belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id", nullable = false)
    private Expansion expansion;

    /**
     * Features associated with this transformation card.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "transformation_card_features",
        joinColumns = @JoinColumn(name = "transformation_card_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    @Builder.Default
    private Set<Feature> features = new HashSet<>();

    /**
     * Timestamp indicating when this transformation card was soft-deleted.
     * If null, the transformation card is active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this transformation card has been soft-deleted.
     *
     * @return true if the transformation card is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the transformation card by setting the deleted_at timestamp.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted transformation card.
     */
    public void restore() {
        this.deletedAt = null;
    }
}

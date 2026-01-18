package com.aboff.core.model.entity;

import com.aboff.core.model.enums.CardType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Abstract base entity representing a card in the Daggerheart TTRPG system.
 * <p>
 * This class uses JOINED inheritance strategy, where each card type (Ancestry,
 * Community, Subclass, Domain) has its own table with type-specific fields,
 * while common fields are stored in the base cards table.
 * </p>
 * <p>
 * The card_type discriminator column determines which subclass a record belongs to.
 * </p>
 */
@Entity
@Table(name = "cards")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "card_type", discriminatorType = DiscriminatorType.STRING)
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class Card extends BaseEntity {

    /**
     * The name of the card.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Detailed description of the card and its effects.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The type of card (ANCESTRY, COMMUNITY, SUBCLASS, DOMAIN).
     * This field serves as the discriminator for inheritance.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, insertable = false, updatable = false, length = 20)
    private CardType cardType;

    /**
     * The expansion this card belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id", nullable = false)
    private Expansion expansion;

    /**
     * Indicates whether this card is from official game content.
     */
    @Column(name = "is_official", nullable = false)
    private Boolean isOfficial;

    /**
     * URL to the background image for this card.
     */
    @Column(name = "background_image_url", length = 500)
    private String backgroundImageUrl;

    /**
     * The features granted by this card.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "card_features",
        joinColumns = @JoinColumn(name = "card_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    private Set<Feature> features;

    /**
     * Timestamp indicating when this card was soft-deleted.
     * If null, the card is active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this card has been soft-deleted.
     *
     * @return true if the card is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the card by setting the deleted_at timestamp.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted card.
     */
    public void restore() {
        this.deletedAt = null;
    }
}

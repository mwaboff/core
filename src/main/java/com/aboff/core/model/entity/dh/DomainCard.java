package com.aboff.core.model.entity.dh;

import com.aboff.core.model.enums.DomainCardType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a domain card in the Daggerheart TTRPG system.
 * <p>
 * Domain cards are magical or specialized abilities tied to specific domains.
 * They have various types (Spell, Grimoire, Ability, Transformation, Wild),
 * a level requirement, and may have a recall cost.
 * </p>
 */
@Entity
@Table(name = "domain_cards")
@DiscriminatorValue("DOMAIN")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DomainCard extends Card {

    /**
     * The domain this card belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "associated_domain_id", nullable = false)
    private Domain associatedDomain;

    /**
     * The level requirement for this domain card.
     * Can be any positive integer value - no maximum constraint.
     */
    @Column(nullable = false)
    private Integer level;

    /**
     * The cost to recall/use this card.
     * Must be zero or positive.
     */
    @Column(name = "recall_cost", nullable = false)
    private Integer recallCost;

    /**
     * The type of domain card (SPELL, GRIMOIRE, ABILITY, TRANSFORMATION, WILD).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "domain_card_type", nullable = false, length = 20)
    private DomainCardType type;
}

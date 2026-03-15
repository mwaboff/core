package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing the association between a character sheet and a domain card
 * in the Daggerheart TTRPG system.
 * <p>
 * This join entity tracks which domain cards a character possesses and whether
 * each card is currently equipped (active). Characters have a maximum of 5
 * equipped domain cards at any time.
 * </p>
 */
@Entity
@Table(name = "character_sheet_domain_cards_equipped")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSheetDomainCard extends BaseEntity {

    /**
     * The character sheet that owns this domain card association.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    /**
     * The domain card associated with this character.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domain_card_id", nullable = false)
    private DomainCard domainCard;

    /**
     * Whether this domain card is currently equipped (active).
     * Characters may have a maximum of 5 equipped domain cards.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean equipped = false;
}

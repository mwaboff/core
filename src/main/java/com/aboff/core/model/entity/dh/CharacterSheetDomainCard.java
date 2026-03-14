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
 * Entity representing the association between a character sheet and a domain card in the
 * Daggerheart TTRPG system.
 * <p>
 * This entity replaces the previous many-to-many join table relationship between
 * {@link CharacterSheet} and {@link DomainCard}, adding an {@code equipped} flag to
 * distinguish between domain cards that are actively equipped versus those stored
 * in the character's vault.
 * </p>
 * <p>
 * Each character sheet can have multiple domain cards, but each domain card can only
 * appear once per character sheet (enforced by a unique constraint on the combination
 * of character_sheet_id and domain_card_id).
 * </p>
 */
@Entity
@Table(
    name = "character_sheet_domain_cards",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_cs_domain_card",
        columnNames = {"character_sheet_id", "domain_card_id"}
    )
)
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSheetDomainCard extends BaseEntity {

    /**
     * The character sheet this domain card association belongs to.
     * When the character sheet is deleted, this association is also removed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    /**
     * The domain card associated with the character sheet.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_card_id", nullable = false)
    private DomainCard domainCard;

    /**
     * Whether this domain card is currently equipped (active) or stored in the vault.
     * Equipped cards are readily available for use during gameplay, while vault cards
     * are stored but not immediately accessible.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean equipped = false;
}

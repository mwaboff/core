package com.aboff.core.model.entity.dh;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.model.enums.SubclassLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a subclass card in the Daggerheart TTRPG system.
 * <p>
 * Subclass cards provide specialization options for character classes.
 * They belong to a {@link SubclassPath} which groups related cards and holds
 * shared attributes like associated class, domains, and spellcasting trait.
 * Each card has a level (Foundation, Specialization, or Mastery) that indicates
 * when it becomes available.
 * </p>
 */
@Entity
@SearchIndexed(type = SearchableEntityType.SUBCLASS_CARD)
@Table(name = "subclass_cards")
@DiscriminatorValue("SUBCLASS")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubclassCard extends Card {

    /**
     * The subclass path this card belongs to.
     * Groups related subclass cards and holds shared attributes like
     * associated class, domains, and spellcasting trait.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclass_path_id", nullable = false)
    private SubclassPath subclassPath;

    /**
     * The level at which this subclass becomes available.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubclassLevel level;
}

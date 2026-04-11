package com.aboff.core.model.entity.dh;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.enums.SearchableEntityType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing an ancestry card in the Daggerheart TTRPG system.
 * <p>
 * Ancestry cards define a character's heritage and racial traits.
 * Mixed ancestry cards ({@code isMixed = true}) combine features from
 * two different ancestries and are always user-created (non-official).
 * </p>
 */
@Entity
@SearchIndexed(type = SearchableEntityType.ANCESTRY_CARD)
@Table(name = "ancestry_cards")
@DiscriminatorValue("ANCESTRY")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
public class AncestryCard extends Card {

    /**
     * Whether this ancestry card represents a mixed ancestry
     * combining features from two different ancestries.
     */
    @Column(name = "is_mixed", nullable = false)
    @Builder.Default
    private Boolean isMixed = false;
}

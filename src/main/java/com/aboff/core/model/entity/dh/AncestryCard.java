package com.aboff.core.model.entity.dh;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing an ancestry card in the Daggerheart TTRPG system.
 * <p>
 * Ancestry cards define a character's heritage and racial traits.
 * This entity extends the base Card class with no additional fields,
 * as all necessary information is inherited from the parent.
 * </p>
 */
@Entity
@Table(name = "ancestry_cards")
@DiscriminatorValue("ANCESTRY")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class AncestryCard extends Card {
    // No additional fields - all data is inherited from Card
}

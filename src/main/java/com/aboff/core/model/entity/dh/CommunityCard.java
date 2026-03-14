package com.aboff.core.model.entity.dh;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a community card in the Daggerheart TTRPG system.
 * <p>
 * Community cards define a character's social background and community ties.
 * This entity extends the base Card class with no additional fields,
 * as all necessary information is inherited from the parent.
 * </p>
 */
@Entity
@Table(name = "community_cards")
@DiscriminatorValue("COMMUNITY")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
public class CommunityCard extends Card {
    // No additional fields - all data is inherited from Card
}

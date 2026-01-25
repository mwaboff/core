package com.aboff.core.model.entity.dh;

import com.aboff.core.model.enums.SubclassLevel;
import com.aboff.core.model.enums.Trait;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Set;

/**
 * Entity representing a subclass card in the Daggerheart TTRPG system.
 * <p>
 * Subclass cards provide specialization options for character classes.
 * They are associated with a specific class and have a level (Foundation,
 * Specialization, or Mastery) that indicates when they become available.
 * </p>
 */
@Entity
@Table(name = "subclass_cards")
@DiscriminatorValue("SUBCLASS")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubclassCard extends Card {

    /**
     * The class that this subclass card is associated with.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "associated_class_id", nullable = false)
    private Class associatedClass;

    /**
     * The level at which this subclass becomes available.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubclassLevel level;

    /**
     * The trait used for spellcasting with this subclass.
     * Optional field - only applicable for subclasses that use spellcasting.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "spellcasting_trait", length = 20)
    private Trait spellcastingTrait;

    /**
     * The domains associated with this subclass.
     * May grant access to specific domain cards or abilities.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "subclass_domains",
        joinColumns = @JoinColumn(name = "subclass_card_id"),
        inverseJoinColumns = @JoinColumn(name = "domain_id")
    )
    private Set<Domain> associatedDomains;
}

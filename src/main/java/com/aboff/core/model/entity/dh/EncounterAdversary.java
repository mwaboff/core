package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Join entity representing a single adversary instance within an encounter.
 * <p>
 * This entity tracks individual adversary instances in encounters.
 * Each entry represents one unique adversary, so multiple instances of the
 * same adversary type require multiple EncounterAdversary records.
 * </p>
 */
@Entity
@Table(name = "encounter_adversaries")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterAdversary extends BaseEntity {

    /**
     * The encounter this adversary belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    /**
     * The adversary included in the encounter.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adversary_id", nullable = false)
    private Adversary adversary;
}

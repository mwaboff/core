package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.CountdownLoop;
import com.aboff.core.model.enums.CountdownType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a GM's countdown within a campaign in the Daggerheart TTRPG system.
 * <p>
 * A countdown "represents a period of time or series of events preceding a future effect"
 * (SRD p. 68). It begins at {@code startingValue}, advances toward 0, and triggers its
 * effect on reaching 0.
 * </p>
 * <p>
 * Modelled on {@link CampaignInvite} for its unidirectional campaign back-reference —
 * {@code Campaign} deliberately has no inverse collection, so the relationship is read
 * through {@code CountdownRepository} and cleaned up by the FK's {@code ON DELETE CASCADE}.
 * Like {@code CharacterSheetCondition}, each row carries its own per-row state, so it gets a
 * dedicated CRUD surface rather than being folded into the campaign's own update payload.
 * </p>
 * <p>
 * Countdowns are GM-only state, like {@code Campaign.gmNotes}, and are not search-indexed.
 * </p>
 */
@Entity
@Table(name = "countdowns")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Countdown extends BaseEntity {

    /**
     * The campaign this countdown belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /**
     * The GM-authored name of the countdown, e.g. "The ritual completes".
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * The advancement mode, which determines when this countdown should be ticked.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "countdown_type", nullable = false, length = 20)
    private CountdownType type;

    /**
     * What happens after the effect triggers at 0.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "loop_behavior", nullable = false, length = 20)
    @Builder.Default
    private CountdownLoop loopBehavior = CountdownLoop.NONE;

    /**
     * The value this countdown resets to when it loops.
     * <p>
     * Deliberately mutable rather than fixed configuration: an increasing or decreasing loop
     * shifts its own starting value by 1 every time it loops (Core Rulebook p. 163). May reach 0
     * for a decreasing countdown that has run out of loops — see {@link #isSpent()}.
     * </p>
     */
    @Column(name = "starting_value", nullable = false)
    private Integer startingValue;

    /**
     * How many segments remain before the effect triggers.
     */
    @Column(name = "current_value", nullable = false)
    private Integer currentValue;

    /**
     * Optional GM note describing what happens when the countdown reaches 0.
     */
    @Column(columnDefinition = "TEXT")
    private String note;

    /**
     * Sort weight within the campaign's countdown list, ascending. Ties break by id.
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Whether a decreasing countdown has run out of loops entirely.
     * <p>
     * Only reachable via {@link CountdownLoop#LOOP_DECREASING}: the Core Rulebook (p. 163) gives
     * decreasing countdowns a finite life, ending when the starting value itself decays to 0.
     * </p>
     *
     * @return true if this countdown has decayed past its final loop
     */
    public boolean isSpent() {
        return startingValue == 0;
    }

    /**
     * Applies this countdown's loop behaviour, if any, now that its effect has triggered.
     * <p>
     * Called when {@code currentValue} reaches 0. A non-looping countdown simply rests at 0
     * until the GM resets or deletes it.
     * </p>
     * <p>
     * Decreasing countdowns are the subtle case. Per the Core Rulebook (p. 163) they do not loop
     * forever: "Once a decreasing countdown reaches 0, a major event triggers—maybe a cave the
     * PCs are struggling to escape from finally collapses". That 0 is the <em>starting</em> value
     * decaying away, not the current value, so the final decrement leaves the countdown spent
     * rather than resetting it again.
     * </p>
     */
    public void applyLoop() {
        switch (loopBehavior) {
            case NONE -> {
                // Rests at 0; the GM decides what happens next.
            }
            case LOOP -> currentValue = startingValue;
            case LOOP_INCREASING -> {
                startingValue = startingValue + 1;
                currentValue = startingValue;
            }
            case LOOP_DECREASING -> {
                if (isSpent()) break;
                startingValue = startingValue - 1;
                currentValue = startingValue;
            }
        }
    }
}

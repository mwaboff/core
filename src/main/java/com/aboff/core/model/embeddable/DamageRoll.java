package com.aboff.core.model.embeddable;

import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Embeddable class representing a damage roll in the Daggerheart TTRPG system.
 *
 * <p>Damage rolls follow the notation: {@code [count]dX[+/-modifier] [damageType]}
 * <ul>
 *   <li>{@code count} - Optional number of dice to roll. If null, uses character's proficiency.</li>
 *   <li>{@code dX} - The type of die (d4, d6, d8, d10, d12, d20)</li>
 *   <li>{@code modifier} - Optional bonus or penalty to add to the roll</li>
 *   <li>{@code damageType} - The type of damage (phy for physical, mag for magic)</li>
 * </ul>
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code "d10+3 mag"} - Roll proficiency d10s, add 3, magic damage</li>
 *   <li>{@code "2d12 phy"} - Roll 2d12, physical damage</li>
 *   <li>{@code "d6-1 phy"} - Roll proficiency d6s, subtract 1, physical damage</li>
 * </ul>
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DamageRoll {

    /**
     * Pattern for parsing damage notation.
     * Groups: 1=diceCount (optional), 2=diceType, 3=modifier with sign (optional), 4=damageType
     *
     * <p>{@code phy/mag} denotes the dual physical-or-magic damage type (see
     * {@link DamageType#PHYSICAL_AND_MAGIC}), where the wielder elects one or the other per
     * attack — it is not a combined damage roll.
     */
    private static final Pattern DAMAGE_PATTERN = Pattern.compile(
            "^(\\d+)?d(\\d+)([+-]\\d+)?\\s+(phy/mag|phy|mag)$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * The number of dice to roll. If null, the character's proficiency score is used.
     */
    @Column(name = "dice_count")
    private Integer diceCount;

    /**
     * The type of die to roll (d4, d6, d8, d10, d12, d20).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dice_type", nullable = false)
    private DiceType diceType;

    /**
     * The modifier to add to or subtract from the roll. Can be positive, negative, or null (no modifier).
     */
    @Column(name = "modifier")
    private Integer modifier;

    /**
     * The type of damage dealt (physical or magic).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "damage_type", nullable = false)
    private DamageType damageType;

    /**
     * Parses a damage notation string and creates a DamageRoll instance.
     *
     * <p>Accepts formats like:
     * <ul>
     *   <li>{@code "d10+3 mag"} - proficiency-based roll with modifier</li>
     *   <li>{@code "2d12 phy"} - explicit dice count, no modifier</li>
     *   <li>{@code "d6-1 phy"} - proficiency-based roll with negative modifier</li>
     * </ul>
     *
     * @param notation the damage notation string to parse
     * @return a new DamageRoll instance
     * @throws IllegalArgumentException if the notation is null, blank, or invalid
     */
    public static DamageRoll parse(String notation) {
        if (notation == null || notation.isBlank()) {
            throw new IllegalArgumentException("Damage notation cannot be null or blank");
        }

        Matcher matcher = DAMAGE_PATTERN.matcher(notation.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid damage notation: " + notation);
        }

        Integer diceCount = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : null;
        DiceType diceType = DiceType.fromCode("d" + matcher.group(2));
        Integer modifier = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : null;
        DamageType damageType = DamageType.fromCode(matcher.group(4));

        return DamageRoll.builder()
                .diceCount(diceCount)
                .diceType(diceType)
                .modifier(modifier)
                .damageType(damageType)
                .build();
    }

    /**
     * Returns whether this damage roll uses the character's proficiency for dice count.
     *
     * @return true if diceCount is null (uses proficiency), false otherwise
     */
    public boolean usesProficiency() {
        return diceCount == null;
    }

    /**
     * Formats this damage roll as a standard notation string.
     *
     * <p>Examples: {@code "d10+3 mag"}, {@code "2d12 phy"}, {@code "d6-1 phy"}
     *
     * @return the formatted damage notation string
     */
    public String toNotation() {
        StringBuilder sb = new StringBuilder();

        if (diceType != null) {
            if (diceCount != null) {
                sb.append(diceCount);
            }
            sb.append(diceType.getCode());
        }

        if (modifier != null && modifier != 0) {
            if (sb.length() > 0 && modifier > 0) {
                sb.append("+");
            }
            sb.append(modifier);
        }

        if (damageType != null) {
            sb.append(" ").append(damageType.getCode());
        }

        return sb.toString();
    }

    /**
     * Returns the damage notation string representation of this damage roll.
     *
     * @return the formatted damage notation string
     */
    @Override
    public String toString() {
        return toNotation();
    }
}

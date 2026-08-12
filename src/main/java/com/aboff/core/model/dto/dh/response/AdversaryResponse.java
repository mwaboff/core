package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.enums.AdversaryType;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for Adversary entities.
 * Represents adversaries (enemies/NPCs) in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * </p>
 * <ul>
 *   <li>By default: returns relationship IDs only</li>
 *   <li>With ?expand=expansion: includes full expansion object</li>
 *   <li>With ?expand=creator: includes creator user object</li>
 *   <li>With ?expand=originalAdversary: includes full original adversary object</li>
 *   <li>With ?expand=evolvesIntoAdversary: includes full evolves-into adversary object</li>
 *   <li>With ?expand=experiences: includes full experience objects</li>
 *   <li>With ?expand=features: includes full feature objects</li>
 *   <li>Multiple expansions can be comma-separated</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdversaryResponse implements Restrictable {

    /**
     * Unique identifier for the adversary.
     */
    private Long id;

    /**
     * Name of the adversary.
     */
    private String name;

    /**
     * Power tier of the adversary (1-4).
     */
    private Integer tier;

    /**
     * The tactical role/type of the adversary.
     */
    private AdversaryType adversaryType;

    /**
     * General description of the adversary.
     */
    private String description;

    /**
     * Motives and tactical guidance for the GM.
     */
    private String motivesAndTactics;

    /**
     * Difficulty rating of the adversary.
     */
    private Integer difficulty;

    /**
     * Damage threshold for major injuries.
     */
    private Integer majorThreshold;

    /**
     * Damage threshold for severe injuries.
     */
    private Integer severeThreshold;

    /**
     * Maximum hit points.
     */
    private Integer hitPointMax;

    /**
     * Currently marked hit points.
     */
    private Integer hitPointMarked;

    /**
     * Maximum stress points.
     */
    private Integer stressMax;

    /**
     * Currently marked stress points.
     */
    private Integer stressMarked;

    /**
     * Modifier applied to attack rolls.
     */
    private Integer attackModifier;

    /**
     * Name of the adversary's weapon or attack.
     */
    private String weaponName;

    /**
     * Effective range of the adversary's attack.
     */
    private Range attackRange;

    /**
     * Damage roll information for the adversary's attack.
     */
    private DamageRollResponse damage;

    /**
     * Whether this adversary is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this adversary is SRD-licensed content, freely usable without owning the
     * sourcebook it was printed in. Never populated on a redacted stub.
     */
    private Boolean srd;

    /**
     * Whether this adversary is publicly visible to other users.
     */
    private Boolean isPublic;

    /**
     * ID of the expansion this adversary belongs to (always included).
     */
    private Long expansionId;

    /**
     * Name of the expansion this adversary belongs to (always included). On a redacted stub,
     * this is the only content-identifying field carried, so the frontend can tell the viewer
     * which book to buy without exposing the adversary's real content.
     */
    private String expansionName;

    /**
     * Full expansion object (included only when ?expand=expansion is specified).
     */
    private ExpansionResponse expansion;

    /**
     * ID of the original adversary if this is a copy (null if original).
     */
    private Long originalAdversaryId;

    /**
     * Full original adversary object (included only when ?expand=originalAdversary is specified).
     */
    private AdversaryResponse originalAdversary;

    /**
     * ID of the adversary this one evolves into, for evolution pairs (null if none).
     */
    private Long evolvesIntoAdversaryId;

    /**
     * Full evolves-into adversary object (included only when ?expand=evolvesIntoAdversary is specified).
     */
    private AdversaryResponse evolvesIntoAdversary;

    /**
     * ID of the user who created this adversary (always included).
     */
    private Long creatorId;

    /**
     * Full creator user object (included only when ?expand=creator is specified).
     */
    private UserResponse creator;

    /**
     * IDs of associated experiences (always included).
     */
    private Set<Long> experienceIds;

    /**
     * Full experience objects (included only when ?expand=experiences is specified).
     */
    private Set<ExperienceResponse> experiences;

    /**
     * IDs of associated features (always included).
     */
    private Set<Long> featureIds;

    /**
     * Full feature objects (included only when ?expand=features is specified).
     */
    private Set<FeatureResponse> features;

    /**
     * Timestamp when the adversary was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the adversary was last modified.
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the adversary was soft-deleted (null if not deleted).
     */
    private LocalDateTime deletedAt;

    /**
     * True when this response is a redacted stub for gated non-SRD content the caller may not
     * browse directly. When true, every field except {@code id}, {@code expansionName}, and
     * this one is omitted from the response.
     */
    private Boolean restricted;

    /**
     * Nested DTO for damage roll information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamageRollResponse {

        /**
         * The number of dice to roll.
         */
        private Integer diceCount;

        /**
         * The type of die to roll.
         */
        private DiceType diceType;

        /**
         * Optional modifier to add to the roll.
         */
        private Integer modifier;

        /**
         * The type of damage dealt.
         */
        private DamageType damageType;

        /**
         * The formatted damage notation (e.g., "2d10+3 phy").
         */
        private String notation;
    }
}

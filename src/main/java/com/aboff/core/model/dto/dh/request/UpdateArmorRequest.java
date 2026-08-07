package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing Armor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateArmorRequest {

    /**
     * Name of the armor.
     */
    @Size(max = 200, message = "Armor name must not exceed 200 characters")
    private String name;

    /**
     * ID of the expansion this armor belongs to.
     */
    private Long expansionId;

    /**
     * The tier level of the armor (1–4).
     */
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must be at most 4")
    private Integer tier;

    /**
     * Whether this armor is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this armor should be visible to every user. Honoured only for MODERATOR+;
     * coerced to false otherwise.
     */
    private Boolean isPublic;

    /**
     * Clears the expansion, marking the armor as belonging to no sourcebook.
     * <p>
     * A JSON {@code null} for {@code expansionId} is indistinguishable from the field being
     * omitted, and omitted means "leave unchanged". This flag is the only way to actually
     * remove an expansion.
     * </p>
     */
    private Boolean clearExpansion;

    /**
     * Campaigns to share this armor with, replacing any existing tags. Null leaves tags
     * untouched; an empty list removes them all.
     */
    private List<Long> campaignIds;

    /**
     * The minimum damage required to inflict a major injury.
     */
    @Positive(message = "Base major threshold must be positive")
    private Integer baseMajorThreshold;

    /**
     * The minimum damage required to inflict a severe injury.
     */
    @Positive(message = "Base severe threshold must be positive")
    private Integer baseSevereThreshold;

    /**
     * The armor's base defensive score.
     */
    @PositiveOrZero(message = "Base score must be zero or positive")
    private Integer baseScore;

    /**
     * Optional list of feature IDs to associate with this armor.
     */
    private List<Long> featureIds;

    /**
     * Features to find or create inline. Used if featureIds is not provided. featureIds takes precedence.
     */
    @Valid
    private List<FeatureInput> features;

}

package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new Armor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateArmorRequest {

    /**
     * Name of the armor.
     */
    @NotBlank(message = "Armor name is required")
    @Size(max = 200, message = "Armor name must not exceed 200 characters")
    private String name;

    /**
     * ID of the expansion this armor belongs to.
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * The tier level of the armor (1–4).
     */
    @NotNull(message = "Tier is required")
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must be at most 4")
    private Integer tier;

    /**
     * Whether this armor is from official game content.
     */
    @NotNull(message = "isOfficial is required")
    private Boolean isOfficial;

    /**
     * The minimum damage required to inflict a major injury.
     */
    @NotNull(message = "Base major threshold is required")
    @Positive(message = "Base major threshold must be positive")
    private Integer baseMajorThreshold;

    /**
     * The minimum damage required to inflict a severe injury.
     */
    @NotNull(message = "Base severe threshold is required")
    @Positive(message = "Base severe threshold must be positive")
    private Integer baseSevereThreshold;

    /**
     * The armor's base defensive score.
     */
    @NotNull(message = "Base score is required")
    @PositiveOrZero(message = "Base score must be zero or positive")
    private Integer baseScore;

    /**
     * Optional ID of the feature granted by this armor.
     */
    private Long featureId;

    /**
     * Feature to find or create inline. Used if featureId is not provided. featureId takes precedence.
     */
    @Valid
    private FeatureInput feature;

    /**
     * Optional ID of the original armor if this is a custom copy.
     */
    private Long originalArmorId;
}

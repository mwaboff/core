package com.aboff.core.model.dto.dh.request;

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
    @NotBlank(message = "Armor name is required")
    @Size(max = 200, message = "Armor name must not exceed 200 characters")
    private String name;

    /**
     * ID of the expansion this armor belongs to.
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

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
     * Optional ID of the original armor if this is a custom copy.
     */
    private Long originalArmorId;
}

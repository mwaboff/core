package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for a user creating their own armor.
 * <p>
 * Separate from {@link CreateArmorRequest} for the reasons documented on
 * {@link CreateCustomWeaponRequest}: that type serves the strict admin import pipeline, this
 * one serves user authoring, and neither should be able to stand in for the other.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomArmorRequest {

    /**
     * Name of the armor.
     */
    @NotBlank(message = "Armor name is required")
    @Size(max = 200, message = "Armor name must not exceed 200 characters")
    private String name;

    /**
     * The tier level of the armor (1–4).
     */
    @NotNull(message = "Tier is required")
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must be at most 4")
    private Integer tier;

    /**
     * Whether this armor should be visible to every user. Honoured only for MODERATOR+;
     * coerced to false otherwise.
     */
    private Boolean isPublic;

    /**
     * Campaigns to share this armor with. The creator must be involved in each campaign
     * they name.
     */
    private List<Long> campaignIds;

    /**
     * The minimum damage required to inflict a major injury on the wearer.
     */
    @NotNull(message = "Base major threshold is required")
    @Min(value = 1, message = "Base major threshold must be at least 1")
    private Integer baseMajorThreshold;

    /**
     * The minimum damage required to inflict a severe injury on the wearer.
     * Must be greater than or equal to the major threshold.
     */
    @NotNull(message = "Base severe threshold is required")
    @Min(value = 1, message = "Base severe threshold must be at least 1")
    private Integer baseSevereThreshold;

    /**
     * The armor score, representing how many Armor Slots the wearer can mark.
     */
    @NotNull(message = "Base score is required")
    @Min(value = 1, message = "Base score must be at least 1")
    private Integer baseScore;

    /**
     * Features granted by this armor, created inline. Capped as a runaway guard — see
     * {@link CreateCustomWeaponRequest#getFeatures()}.
     */
    @Valid
    @Size(max = 20, message = "An armor may not have more than 20 features")
    private List<FeatureInput> features;
}

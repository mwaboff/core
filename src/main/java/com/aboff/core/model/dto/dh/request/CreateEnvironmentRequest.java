package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.EnvironmentType;
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
 * Request DTO for creating a new Environment.
 * <p>
 * Exactly one of {@link #difficulty} or {@link #difficultySpecial} must be
 * provided -- see {@link com.aboff.core.model.entity.dh.Environment} for why
 * these two fields are mutually exclusive rather than difficulty simply being
 * nullable.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEnvironmentRequest {

    /**
     * Name of the environment.
     */
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * Power tier of the environment (1-4).
     */
    @NotNull(message = "Tier is required")
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must not exceed 4")
    private Integer tier;

    /**
     * The narrative role this environment plays.
     */
    @NotNull(message = "Environment type is required")
    private EnvironmentType environmentType;

    /**
     * General description of the environment/scene.
     */
    private String description;

    /**
     * The printed "Impulses" line.
     */
    private String impulses;

    /**
     * Numeric Difficulty rating. Mutually exclusive with {@link #difficultySpecial} --
     * exactly one of the two must be provided.
     */
    @Min(value = 1, message = "Difficulty must be at least 1")
    private Integer difficulty;

    /**
     * Verbatim printed Difficulty text used when the book overrides the numeric
     * rating with rules text (e.g. "Special (see 'Relative Strength')"). Mutually
     * exclusive with {@link #difficulty} -- exactly one of the two must be provided.
     */
    @Size(max = 255, message = "Difficulty special text must not exceed 255 characters")
    private String difficultySpecial;

    /**
     * Verbatim printed "Potential adversaries" line.
     */
    private String potentialAdversaries;

    /**
     * ID of the expansion this environment belongs to.
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * Whether this environment is from official game content.
     */
    @NotNull(message = "isOfficial is required")
    private Boolean isOfficial;

    /**
     * Whether this environment is publicly visible to other users.
     */
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * IDs of features to associate with this environment.
     */
    private List<Long> featureIds;

    /**
     * Features to find or create inline. Merged with featureIds if both provided.
     */
    @Valid
    private List<FeatureInput> features;
}

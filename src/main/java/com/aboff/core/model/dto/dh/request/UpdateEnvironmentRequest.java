package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.EnvironmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing Environment.
 * All fields are optional to support partial updates.
 * <p>
 * If either {@link #difficulty} or {@link #difficultySpecial} is provided, the
 * service re-validates that exactly one of the two is set on the resulting
 * entity -- see {@link com.aboff.core.model.entity.dh.Environment}.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEnvironmentRequest {

    /**
     * Name of the environment.
     */
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * Power tier of the environment (1-4).
     */
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must not exceed 4")
    private Integer tier;

    /**
     * The narrative role this environment plays.
     */
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
     * Numeric Difficulty rating. Setting this clears {@link #difficultySpecial}
     * only if this request also explicitly clears it -- see
     * {@code EnvironmentService} for the exact swap semantics.
     */
    @Min(value = 1, message = "Difficulty must be at least 1")
    private Integer difficulty;

    /**
     * Verbatim printed Difficulty text (e.g. "Special (see 'Relative Strength')").
     */
    @Size(max = 255, message = "Difficulty special text must not exceed 255 characters")
    private String difficultySpecial;

    /**
     * Explicit flag to clear {@link #difficulty} back to null when switching an
     * environment to a "Special" difficulty. Partial-update requests only apply
     * non-null fields, so a plain null difficulty can't otherwise distinguish
     * "leave unchanged" from "clear this field."
     */
    private Boolean clearDifficulty;

    /**
     * Explicit flag to clear {@link #difficultySpecial} back to null when
     * switching an environment to a numeric difficulty. See {@link #clearDifficulty}.
     */
    private Boolean clearDifficultySpecial;

    /**
     * Verbatim printed "Potential adversaries" line.
     */
    private String potentialAdversaries;

    /**
     * ID of the expansion this environment belongs to.
     */
    private Long expansionId;

    /**
     * Whether this environment is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this environment is publicly visible to other users.
     */
    private Boolean isPublic;

    /**
     * IDs of features to associate with this environment.
     * Replaces existing features when provided.
     */
    private List<Long> featureIds;

    /**
     * Features to find or create inline. Merged with featureIds if both provided.
     */
    @Valid
    private List<FeatureInput> features;
}

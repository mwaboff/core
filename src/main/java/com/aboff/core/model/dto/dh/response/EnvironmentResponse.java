package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.enums.EnvironmentType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Environment entities.
 * Represents GM-facing scene stat blocks in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * </p>
 * <ul>
 *   <li>By default: returns relationship IDs only</li>
 *   <li>With ?expand=expansion: includes full expansion object</li>
 *   <li>With ?expand=creator: includes creator user object</li>
 *   <li>With ?expand=features: includes full feature objects</li>
 *   <li>Multiple expansions can be comma-separated</li>
 * </ul>
 * <p>
 * Exactly one of {@link #difficulty} or {@link #difficultySpecial} is
 * populated for any given environment -- see
 * {@link com.aboff.core.model.entity.dh.Environment} for why.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvironmentResponse {

    /**
     * Unique identifier for the environment.
     */
    private Long id;

    /**
     * Name of the environment.
     */
    private String name;

    /**
     * Power tier of the environment (1-4).
     */
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
     * Numeric Difficulty rating, when the book prints one (null if
     * {@link #difficultySpecial} is set instead).
     */
    private Integer difficulty;

    /**
     * Verbatim printed Difficulty text, when the book overrides the numeric
     * rating with rules text (null if {@link #difficulty} is set instead).
     */
    private String difficultySpecial;

    /**
     * Verbatim printed "Potential adversaries" line.
     */
    private String potentialAdversaries;

    /**
     * Whether this environment is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this environment is publicly visible to other users.
     */
    private Boolean isPublic;

    /**
     * ID of the expansion this environment belongs to (always included).
     */
    private Long expansionId;

    /**
     * Full expansion object (included only when ?expand=expansion is specified).
     */
    private ExpansionResponse expansion;

    /**
     * ID of the user who created this environment (always included).
     */
    private Long creatorId;

    /**
     * Full creator user object (included only when ?expand=creator is specified).
     */
    private UserResponse creator;

    /**
     * IDs of associated features (always included).
     */
    private List<Long> featureIds;

    /**
     * Full feature objects (included only when ?expand=features is specified).
     */
    private List<FeatureResponse> features;

    /**
     * Timestamp when the environment was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the environment was last modified.
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the environment was soft-deleted (null if not deleted).
     */
    private LocalDateTime deletedAt;
}

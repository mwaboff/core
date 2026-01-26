package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.dto.response.UserResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Experience entities.
 * Represents an experience entry for a character in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * - By default: returns IDs only for relationships
 * - With ?expand=characterSheet: includes full character sheet object
 * - With ?expand=createdBy: includes full user object
 * - Multiple expansions can be comma-separated: ?expand=characterSheet,createdBy
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExperienceResponse {

    /**
     * Unique identifier for the experience
     */
    private Long id;

    /**
     * ID of the character sheet this experience belongs to (always included)
     */
    private Long characterSheetId;

    /**
     * Full character sheet object (included only when ?expand=characterSheet is specified)
     */
    private CharacterSheetResponse characterSheet;

    /**
     * ID of the user who created this experience (always included)
     */
    private Long createdById;

    /**
     * Full user object (included only when ?expand=createdBy is specified)
     */
    private UserResponse createdBy;

    /**
     * Detailed description of the experience
     */
    private String description;

    /**
     * The bonus modifier granted by this experience
     */
    private Integer modifier;

    /**
     * Timestamp when the experience was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the experience was last modified
     */
    private LocalDateTime lastModifiedAt;
}

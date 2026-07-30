package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for CharacterSheetCondition entities.
 * Represents a character's instance of a catalogue {@link ConditionResponse}, with its own
 * {@code magnitude} snapshot.
 * <p>
 * Supports expansion via the ?expand parameter:
 * <ul>
 *   <li>By default: returns characterSheetId and conditionId only</li>
 *   <li>With ?expand=characterSheet: includes full character sheet object</li>
 *   <li>With ?expand=condition: includes full condition object</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CharacterSheetConditionResponse {

    /**
     * Unique identifier for this condition instance.
     */
    private Long id;

    /**
     * ID of the character sheet this condition instance applies to (always included).
     */
    private Long characterSheetId;

    /**
     * Full character sheet object (included only when ?expand=characterSheet is specified).
     */
    private CharacterSheetResponse characterSheet;

    /**
     * ID of the catalogue condition being applied (always included).
     */
    private Long conditionId;

    /**
     * Full condition object (included only when ?expand=condition is specified).
     */
    private ConditionResponse condition;

    /**
     * The magnitude (stack count or intensity) of this condition instance, where applicable.
     */
    private Integer magnitude;

    /**
     * Timestamp when this condition instance was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when this condition instance was last modified.
     */
    private LocalDateTime lastModifiedAt;
}

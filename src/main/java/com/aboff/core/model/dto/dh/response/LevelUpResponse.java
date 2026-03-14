package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO returned after a successful character level-up.
 * <p>
 * Contains the updated character sheet, the ID of the advancement log entry
 * created for audit purposes, and a human-readable summary of all changes applied.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LevelUpResponse {

    /**
     * The updated character sheet after leveling up.
     */
    private CharacterSheetResponse characterSheet;

    /**
     * ID of the advancement log entry created for this level-up.
     */
    private Long advancementLogId;

    /**
     * Human-readable summary of changes applied during the level-up.
     */
    private List<String> appliedChanges;
}

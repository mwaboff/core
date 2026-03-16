package com.aboff.core.model.dto.dh.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing the result of a successful level-up operation.
 * <p>
 * Contains the updated character sheet, the advancement log ID for undo support,
 * and a human-readable list of changes applied.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelUpResponse {

    /**
     * The updated character sheet after level-up.
     */
    private CharacterSheetResponse characterSheet;

    /**
     * The ID of the saved advancement log entry.
     */
    private Long advancementLogId;

    /**
     * Human-readable list of changes applied during this level-up.
     */
    private List<String> appliedChanges;
}

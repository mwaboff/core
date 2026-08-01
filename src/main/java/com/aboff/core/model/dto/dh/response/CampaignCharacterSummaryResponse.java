package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lightweight summary of a character sheet within a campaign context.
 * <p>
 * Used in campaign GET with {@code ?expand=characterSummaries} to provide
 * enriched character data without the full CharacterSheetResponse payload.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignCharacterSummaryResponse {

    /**
     * Character sheet ID
     */
    private Long id;

    /**
     * Character name
     */
    private String name;

    /**
     * Character level
     */
    private Integer level;

    /**
     * ID of the character's owner
     */
    private Long ownerId;

    /**
     * Username of the character's owner
     */
    private String ownerUsername;

    /**
     * Names of the character's ancestry cards
     */
    private List<String> ancestryNames;

    /**
     * Names of the character's subclass cards
     */
    private List<String> subclassNames;

    /**
     * Names of the character's class cards (derived from subclass card class names)
     */
    private List<String> classNames;

    /**
     * Whether the Game Master has enabled transformations for this character.
     * Lets the GM roster render the current gate state without fetching each sheet.
     */
    private boolean transformationEnabled;

    /**
     * ID of the transformation card assigned to this character, or null if none.
     */
    private Long transformationCardId;

    /**
     * Name of the transformation card assigned to this character, or null if none.
     */
    private String transformationCardName;
}

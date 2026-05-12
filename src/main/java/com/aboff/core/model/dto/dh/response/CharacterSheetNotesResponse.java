package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Slim response DTO for the character-sheet notes endpoint.
 *
 * <p>Returned by the dedicated notes GET and PATCH endpoints to allow cheap polling
 * and autosave-on-keystroke without transferring the full {@link CharacterSheetResponse}
 * payload. Contains only the character sheet's identifier, current notes content,
 * and the last-modified timestamp.
 *
 * <p>The {@code notes} field is nullable; a {@code null} value indicates that no notes
 * have been stored yet for this character sheet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CharacterSheetNotesResponse {

    /**
     * Unique identifier of the character sheet.
     */
    private Long id;

    /**
     * Current notes content for the character sheet, or {@code null} if no notes have been set.
     */
    private String notes;

    /**
     * Timestamp of the most recent modification to the character sheet.
     */
    private LocalDateTime lastModifiedAt;
}

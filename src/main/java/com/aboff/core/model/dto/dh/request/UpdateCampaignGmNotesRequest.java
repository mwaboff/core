package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a campaign's game master notes.
 * <p>
 * The value replaces the stored notes wholesale; an empty string clears them.
 * The notes are sanitized before persistence, so the stored value may differ
 * from the submitted one.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCampaignGmNotesRequest {

    /**
     * The campaign's new game master notes. Required (send {@code ""} to clear),
     * and must not exceed 50,000 characters.
     */
    @NotNull(message = "GM notes are required")
    @Size(max = 50000, message = "GM notes must not exceed 50000 characters")
    private String gmNotes;
}

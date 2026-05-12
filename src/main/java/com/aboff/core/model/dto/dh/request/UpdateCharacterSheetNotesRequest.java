package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating the notes field on a character sheet.
 *
 * <p>Supports autosave-on-keystroke scenarios via PATCH. An empty string is accepted and
 * clears the notes. A null value is rejected with HTTP 400; callers must send {@code ""}
 * to explicitly clear notes rather than omitting the field.
 *
 * <p>Notes are subject to a 10,000-character length cap and are sanitized server-side via
 * {@link com.aboff.core.util.MarkdownSanitizerUtil} before being persisted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCharacterSheetNotesRequest {

    /**
     * Free-text markdown notes for the character sheet.
     * Must not be null; empty string is allowed to clear existing notes.
     */
    @NotNull(message = "notes must not be null")
    @Size(max = 10000, message = "notes must not exceed 10000 characters")
    private String notes;
}

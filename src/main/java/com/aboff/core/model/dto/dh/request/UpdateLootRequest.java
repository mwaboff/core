package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing Loot item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLootRequest {

    /**
     * Name of the loot item.
     */
    @NotBlank(message = "Loot name is required")
    @Size(max = 200, message = "Loot name must not exceed 200 characters")
    private String name;

    /**
     * ID of the expansion this loot belongs to.
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * Whether this loot is from official game content.
     */
    @NotNull(message = "isOfficial is required")
    private Boolean isOfficial;

    /**
     * Optional description of the loot item.
     */
    private String description;

    /**
     * Optional ID of the original loot if this is a custom copy.
     */
    private Long originalLootId;
}

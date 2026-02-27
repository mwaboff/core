package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.Trait;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Input DTO for inline find-or-create of a SubclassPath when creating or updating SubclassCards.
 * <p>
 * When provided instead of a subclassPathId, the system will look up an existing path
 * by name and class, or create a new one if no match is found.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubclassPathInput {

    /**
     * Name of the subclass path to find or create.
     */
    @NotBlank(message = "Path name is required")
    @Size(max = 200, message = "Path name must not exceed 200 characters")
    private String name;

    /**
     * IDs of domains to associate with the path.
     * Used only when creating a new path.
     */
    private List<Long> associatedDomainIds;

    /**
     * The spellcasting trait for the path.
     * Used only when creating a new path.
     */
    private Trait spellcastingTrait;
}

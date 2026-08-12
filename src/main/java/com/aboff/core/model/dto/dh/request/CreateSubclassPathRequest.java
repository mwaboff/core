package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.Trait;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new SubclassPath.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubclassPathRequest {

    /**
     * Name of the subclass path.
     */
    @NotBlank(message = "Path name is required")
    @Size(max = 200, message = "Path name must not exceed 200 characters")
    private String name;

    /**
     * ID of the class this path belongs to.
     */
    @NotNull(message = "Associated class ID is required")
    private Long associatedClassId;

    /**
     * ID of the expansion this path belongs to.
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * The spellcasting trait for the path, if applicable.
     */
    private Trait spellcastingTrait;

    /**
     * Whether this path (and by cascade, its Foundation/Specialization/Mastery cards) is
     * SRD-licensed content. Optional and ADMIN+ only — see
     * {@code ContentAccessService#resolveSrd}. Deliberately not {@code @NotNull}: bulk import
     * payloads omit this field and must keep working unchanged.
     */
    private Boolean srd;

    /**
     * IDs of domains to associate with this path.
     */
    private List<Long> associatedDomainIds;
}

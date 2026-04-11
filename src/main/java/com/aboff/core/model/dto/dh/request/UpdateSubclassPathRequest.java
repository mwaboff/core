package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.Trait;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing SubclassPath.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubclassPathRequest {

    /**
     * Name of the subclass path.
     */
    @Size(max = 200, message = "Path name must not exceed 200 characters")
    private String name;

    /**
     * ID of the class this path belongs to.
     */
    private Long associatedClassId;

    /**
     * ID of the expansion this path belongs to.
     */
    private Long expansionId;

    /**
     * The spellcasting trait for the path, if applicable.
     */
    private Trait spellcastingTrait;

    /**
     * IDs of domains to associate with this path.
     */
    private List<Long> associatedDomainIds;
}

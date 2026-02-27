package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.SubclassLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new SubclassCard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubclassCardRequest {
    /**
     * Name of the card
     */
    @NotBlank(message = "Card name is required")
    @Size(max = 200, message = "Card name must not exceed 200 characters")
    private String name;

    /**
     * Detailed description of the card
     */
    private String description;

    /**
     * ID of the expansion this card belongs to
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * Whether this card is from official game content
     */
    @NotNull(message = "isOfficial is required")
    private Boolean isOfficial;

    /**
     * URL to the background image for this card
     */
    @Size(max = 500, message = "Background image URL must not exceed 500 characters")
    private String backgroundImageUrl;

    /**
     * IDs of features granted by this card
     */
    private List<Long> featureIds;

    /**
     * IDs of cost tags associated with this card
     */
    private List<Long> costTagIds;

    /**
     * Cost tags to find or create by label. Merged with costTagIds if both provided.
     */
    @Valid
    private List<CostTagInput> costTags;

    /**
     * ID of an existing subclass path to associate with this card.
     * Either subclassPathId or subclassPath (with associatedClassId) must be provided.
     */
    private Long subclassPathId;

    /**
     * Inline subclass path input for find-or-create.
     * When provided, associatedClassId must also be specified.
     */
    @Valid
    private SubclassPathInput subclassPath;

    /**
     * ID of the class for path resolution when using inline subclassPath input.
     * Required when subclassPath is provided, ignored when subclassPathId is used.
     */
    private Long associatedClassId;

    /**
     * The level at which this subclass becomes available
     */
    @NotNull(message = "Subclass level is required")
    private SubclassLevel level;
}

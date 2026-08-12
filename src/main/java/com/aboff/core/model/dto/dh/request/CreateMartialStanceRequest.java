package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new MartialStance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMartialStanceRequest {

    /**
     * Name of the martial stance.
     */
    @NotBlank(message = "Martial stance name is required")
    @Size(max = 200, message = "Martial stance name must not exceed 200 characters")
    private String name;

    /**
     * ID of the expansion this martial stance belongs to.
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * The tier level of the martial stance (1–4), gating which stances a character can know.
     */
    @NotNull(message = "Tier is required")
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must be at most 4")
    private Integer tier;

    /**
     * Whether this martial stance is from official game content.
     */
    @NotNull(message = "isOfficial is required")
    private Boolean isOfficial;

    /**
     * Whether this martial stance is SRD-licensed content, freely usable without owning the
     * sourcebook it was printed in. Optional and honoured only for ADMIN+ — see
     * {@code ContentAccessService#resolveSrd}. Omitted by existing bulk-import payloads, which
     * must keep working, so this is never required.
     */
    private Boolean srd;

    /**
     * Effect text of the martial stance.
     */
    private String description;

    /**
     * Optional list of existing feature IDs to associate with this martial stance.
     */
    private List<Long> featureIds;

    /**
     * Optional list of features to find or create inline.
     */
    @Valid
    private List<FeatureInput> features;

    /**
     * Optional ID of the original martial stance if this is a custom copy.
     */
    private Long originalMartialStanceId;
}

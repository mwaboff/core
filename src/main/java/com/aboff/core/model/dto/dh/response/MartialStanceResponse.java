package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for MartialStance entities.
 * Represents martial stances (Hope & Fear's modal "Stance Fighter" combat states) in the
 * Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * <ul>
 *   <li>By default: returns expansionId, featureIds, originalMartialStanceId only</li>
 *   <li>With ?expand=expansion: includes full expansion object</li>
 *   <li>With ?expand=features: includes full feature objects</li>
 *   <li>With ?expand=originalMartialStance: includes full original martial stance object</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MartialStanceResponse implements Restrictable {

    /**
     * Unique identifier for the martial stance.
     */
    private Long id;

    /**
     * Name of the martial stance.
     */
    private String name;

    /**
     * ID of the expansion this martial stance belongs to (always included).
     */
    private Long expansionId;

    /**
     * Name of the expansion this martial stance belongs to (always included). On a redacted
     * stub, this is the only content-identifying field carried, so the frontend can tell the
     * viewer which book to buy without exposing the stance's real content.
     */
    private String expansionName;

    /**
     * True when this response is a redacted stub for gated non-SRD content the caller may not
     * view; every field but {@code id}, {@code restricted}, and {@code expansionName} is absent.
     */
    private Boolean restricted;

    /**
     * Full expansion object (included only when ?expand=expansion is specified).
     */
    private ExpansionResponse expansion;

    /**
     * The tier level of the martial stance (1–4).
     */
    private Integer tier;

    /**
     * Whether this martial stance is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this martial stance is SRD-licensed content, freely usable without owning the
     * sourcebook it was printed in.
     */
    private Boolean srd;

    /**
     * Effect text of the martial stance.
     */
    private String description;

    /**
     * IDs of features granted by this martial stance (always included when present).
     */
    private List<Long> featureIds;

    /**
     * Full feature objects (included only when ?expand=features is specified).
     */
    private List<FeatureResponse> features;

    /**
     * ID of the original martial stance if this is a custom copy (null if original).
     */
    private Long originalMartialStanceId;

    /**
     * Full original martial stance object (included only when ?expand=originalMartialStance
     * is specified).
     */
    private MartialStanceResponse originalMartialStance;

    /**
     * Timestamp when the martial stance was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the martial stance was last modified.
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the martial stance was soft-deleted (null if not deleted).
     */
    private LocalDateTime deletedAt;
}

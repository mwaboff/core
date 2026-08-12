package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.Trait;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for SubclassPath entities.
 * Represents a subclass path grouping in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * - By default: returns IDs only for relationships
 * - With ?expand=associatedClass: includes full class object
 * - With ?expand=associatedDomains: includes full domain objects
 * - With ?expand=expansion: includes full expansion object
 * - Multiple expansions can be comma-separated: ?expand=associatedClass,associatedDomains,expansion
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubclassPathResponse implements Restrictable {

    /**
     * Unique identifier for the subclass path.
     */
    private Long id;

    /**
     * Name of the subclass path.
     */
    private String name;

    /**
     * Whether this path (and by cascade, every card in it) is SRD-licensed content, freely
     * usable without owning the sourcebook it was printed in.
     */
    private Boolean srd;

    /**
     * ID of the associated class (always included).
     */
    private Long associatedClassId;

    /**
     * Full class object (included only when ?expand=associatedClass is specified).
     */
    private ClassResponse associatedClass;

    /**
     * The spellcasting trait for this path, including metadata.
     * Null if the path does not involve spellcasting.
     */
    private TraitInfo spellcastingTrait;

    /**
     * IDs of associated domains (always included).
     */
    private List<Long> associatedDomainIds;

    /**
     * Full domain objects (included only when ?expand=associatedDomains is specified).
     */
    private List<DomainResponse> associatedDomains;

    /**
     * ID of the expansion this path belongs to (always included).
     */
    private Long expansionId;

    /**
     * Full expansion object (included only when ?expand=expansion is specified).
     */
    private ExpansionResponse expansion;

    /**
     * Timestamp when the subclass path was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the subclass path was last modified.
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the subclass path was soft-deleted (null if not deleted).
     */
    private LocalDateTime deletedAt;

    /**
     * The display name of the expansion this path belongs to. Set on a redacted stub so the
     * caller can tell which book to buy, even though {@link #expansion} itself is unset.
     */
    private String expansionName;

    /**
     * True if this response is a redacted stub for gated non-SRD content the caller may not
     * view. When true, every other field except {@link #id} and {@link #expansionName} is
     * unset.
     */
    private Boolean restricted;

    /**
     * Nested class containing trait information with metadata.
     * Includes the trait name along with its description and usage examples.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraitInfo {

        /**
         * The trait name (e.g., AGILITY, KNOWLEDGE).
         */
        private Trait trait;

        /**
         * Description of what the trait represents.
         */
        private String description;

        /**
         * Examples of when this trait is used.
         */
        private String examples;
    }
}

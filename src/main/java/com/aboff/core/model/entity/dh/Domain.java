package com.aboff.core.model.entity.dh;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.SearchableEntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Entity representing a domain in the Daggerheart TTRPG system.
 * <p>
 * Domains represent magical or thematic categories that cards and classes
 * can be associated with (e.g., Fire, Ice, Nature, etc.).
 * </p>
 */
@Entity
@SearchIndexed(type = SearchableEntityType.DOMAIN)
@Table(name = "domains")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Domain extends BaseEntity {

    /**
     * The unique name of the domain.
     * Must be unique across all domains and not null.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * URL to the icon representing this domain.
     */
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    /**
     * Detailed description of the domain and its characteristics.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Indicates whether this domain is from official game content.
     * Defaults to true at creation time, applied by the service layer.
     */
    @Column(name = "is_official", nullable = false)
    private Boolean isOfficial;

    /**
     * Indicates whether this domain is SRD-licensed content, freely usable without owning
     * the sourcebook it was printed in. Defaults to false at creation time; only an explicit
     * SRD flag opens the domain to users who have not been granted expansion access. See
     * {@code ContentAccessService} for how this is enforced.
     */
    @Column(name = "srd", nullable = false)
    @Builder.Default
    private Boolean srd = false;

    /**
     * The expansion this domain belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id", nullable = false)
    private Expansion expansion;

    /**
     * Timestamp indicating when this domain was soft-deleted.
     * If null, the domain is active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this domain has been soft-deleted.
     *
     * @return true if the domain is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the domain by setting the deleted_at timestamp.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted domain.
     */
    public void restore() {
        this.deletedAt = null;
    }
}

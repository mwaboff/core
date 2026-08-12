package com.aboff.core.model.entity.dh;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.EnvironmentType;
import com.aboff.core.model.enums.SearchableEntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing an environment in the Daggerheart TTRPG system.
 * <p>
 * Environments are GM-facing stat blocks that set the scene for a scenario --
 * Impulses, a Difficulty, Potential Adversaries, and features -- across four
 * types (Exploration, Traversal, Event, Social) and four tiers. Unlike
 * adversaries, weapons, or armor, an environment is never selected or equipped
 * by a player; it exists purely as GM reference content.
 * </p>
 * <p>
 * <strong>Difficulty representation:</strong> most environments print a plain
 * numeric Difficulty, but at least one core-book environment prints
 * {@code Difficulty: Special (see "Relative Strength")} instead of a number --
 * a deliberate rules callout, not an absent stat. To preserve that printed
 * text rather than discarding it, {@code difficulty} and
 * {@code difficultySpecial} are mutually exclusive: exactly one is set on any
 * given environment (enforced by a database CHECK constraint), never both and
 * never neither.
 * </p>
 * <p>
 * Mirrors {@link Adversary}'s shape for soft-delete, official/public content
 * management, and creator tracking -- see that class for the permission model
 * these fields support.
 * </p>
 */
@Entity
@SearchIndexed(type = SearchableEntityType.ENVIRONMENT)
@Table(name = "environments")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Environment extends BaseEntity {

    /**
     * The environment's name.
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * The environment's tier level (1-4).
     */
    @Column(name = "tier", nullable = false)
    private Integer tier;

    /**
     * The narrative role this environment plays (Exploration, Traversal, Event, Social).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", nullable = false, length = 50)
    private EnvironmentType environmentType;

    /**
     * General description of the environment/scene.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * The printed "Impulses" line -- short GM guidance on what this environment wants.
     */
    @Column(name = "impulses", columnDefinition = "TEXT")
    private String impulses;

    /**
     * Numeric Difficulty rating, when the book prints one.
     * Mutually exclusive with {@link #difficultySpecial}; exactly one is set.
     */
    @Column(name = "difficulty")
    private Integer difficulty;

    /**
     * The verbatim printed Difficulty text when the book overrides the numeric
     * rating with rules text (e.g. {@code "Special (see "Relative Strength")"}).
     * Mutually exclusive with {@link #difficulty}; exactly one is set.
     */
    @Column(name = "difficulty_special", length = 255)
    private String difficultySpecial;

    /**
     * The verbatim printed "Potential adversaries" line. Kept as free text rather
     * than an FK relation: the printed value groups adversaries under an editorial
     * label that is not itself an adversary row, and members reference other
     * adversaries by name, some unresolvable within a single source book.
     */
    @Column(name = "potential_adversaries", columnDefinition = "TEXT")
    private String potentialAdversaries;

    /**
     * Indicates whether this environment is from official game content.
     */
    @Column(name = "is_official", nullable = false)
    @Builder.Default
    private Boolean isOfficial = false;

    /**
     * Indicates whether this custom environment is publicly visible.
     */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * Indicates whether this environment is SRD-licensed content, freely usable without
     * owning the sourcebook it was printed in. Defaults to false at creation time; only an
     * explicit SRD flag opens the environment to users who have not been granted expansion
     * access. See {@code ContentAccessService} for how this is enforced.
     */
    @Column(name = "srd", nullable = false)
    @Builder.Default
    private Boolean srd = false;

    /**
     * The expansion this environment belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id", nullable = false)
    private Expansion expansion;

    /**
     * The user who created this environment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User createdBy;

    /**
     * Features associated with this environment (feature_type = ENVIRONMENT).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "environment_features",
        joinColumns = @JoinColumn(name = "environment_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    @Builder.Default
    private Set<Feature> features = new HashSet<>();

    /**
     * Timestamp indicating when this environment was soft-deleted.
     * If null, the environment is active and available.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this environment has been soft-deleted.
     *
     * @return true if the environment is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the environment by setting the deleted_at timestamp to the current time.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted environment by clearing the deleted_at timestamp.
     */
    public void restore() {
        this.deletedAt = null;
    }
}

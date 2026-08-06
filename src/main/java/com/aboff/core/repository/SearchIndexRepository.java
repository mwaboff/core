package com.aboff.core.repository;

import com.aboff.core.model.entity.SearchIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link SearchIndex} entity.
 * <p>
 * Provides full-text search (FTS) queries backed by PostgreSQL {@code tsvector} and
 * {@code ts_rank}, as well as utility methods for managing search index rows during
 * entity lifecycle events (create, update, soft-delete, restore).
 * </p>
 *
 * <h2>Full-Text Search</h2>
 * The primary search method uses {@code plainto_tsquery('english', :query)} to convert
 * the caller's input into a tsquery, then ranks results via {@code ts_rank}. All filter
 * parameters are nullable; a {@code null} value disables that filter.
 *
 * <h2>Access Control</h2>
 * Search results are restricted to rows that satisfy at least one of:
 * <ul>
 *   <li>Official content ({@code is_official = true})</li>
 *   <li>Public content ({@code is_public = true})</li>
 *   <li>Content created by the requesting user ({@code created_by_user_id = :userId})</li>
 *   <li>System content with no owner ({@code created_by_user_id IS NULL})</li>
 *   <li>Content shared with a campaign the requesting user is involved in
 *       ({@code shared_campaign_ids && :memberCampaignIds})</li>
 *   <li>Privileged users (MODERATOR or above) bypass access control entirely</li>
 * </ul>
 *
 * <h2>Upsert</h2>
 * The {@link #upsertSearchIndex} method performs a PostgreSQL {@code INSERT ... ON CONFLICT}
 * upsert so that re-indexing an entity is idempotent.
 */
@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Long> {

    // -------------------------------------------------------------------------
    // Full-text search
    // -------------------------------------------------------------------------

    /**
     * Performs a paginated full-text search across the search index, applying optional
     * filters and access-control rules.
     *
     * <p>Results are ordered by descending {@code ts_rank} relevance score. The
     * returned {@code Object[]} rows contain all {@code search_index} columns plus
     * the computed {@code relevance_score} as the final element.</p>
     *
     * <p>Each nullable filter parameter is skipped when passed as {@code null}, allowing
     * callers to mix and match filters freely.</p>
     *
     * @param query               the user-supplied search string; converted to a tsquery via
     *                            {@code plainto_tsquery('english', :query)}
     * @param filterByEntityTypes {@code true} to restrict results to {@code entityTypes};
     *                            {@code false} to include all types (and {@code entityTypes}
     *                            is ignored — pass any non-null list such as a single-element
     *                            sentinel to satisfy the IN clause)
     * @param entityTypes         list of entity type strings to restrict results to when
     *                            {@code filterByEntityTypes} is {@code true}; must be non-null
     *                            and non-empty to keep the IN clause valid
     * @param tier             optional tier level filter; {@code null} disables
     * @param expansionId      optional expansion foreign key filter; {@code null} disables
     * @param isOfficial       optional official-content filter; {@code null} disables
     * @param cardType         optional card type filter; {@code null} disables
     * @param featureType      optional feature type filter; {@code null} disables
     * @param adversaryType    optional adversary type filter; {@code null} disables
     * @param domainCardType   optional domain card type filter; {@code null} disables
     * @param associatedDomainId optional associated domain foreign key filter; {@code null} disables
     * @param trait            optional trait filter; {@code null} disables
     * @param range            optional range filter; {@code null} disables
     * @param burden           optional burden filter; {@code null} disables
     * @param isConsumable     optional consumable flag filter; {@code null} disables
     * @param userId           the ID of the requesting user, used for access control
     * @param memberCampaignIds PostgreSQL {@code bigint[]} literal of the campaigns the requesting
     *                         user is involved in, e.g. {@code "{3,7}"}. Unlike the filter
     *                         parameters this one <em>grants</em> access rather than narrowing it,
     *                         so {@code "{}"} must be passed for a user in no campaigns —
     *                         an empty array overlaps nothing. See
     *                         {@link com.aboff.core.util.PostgresArrayUtil}
     * @param isPrivileged     {@code true} if the requesting user is MODERATOR or above,
     *                         bypassing ownership-based access restrictions
     * @param pageable         pagination and sort parameters
     * @return a page of raw {@code Object[]} rows, each row containing all
     *         {@code search_index} columns plus a trailing {@code relevance_score} double
     */
    @Query(
        value = """
            SELECT si.*, ts_rank(si.search_vector, plainto_tsquery('english', :query)) AS relevance_score
            FROM search_index si
            WHERE si.deleted_at IS NULL
              AND si.search_vector @@ plainto_tsquery('english', :query)
              AND (
                    si.is_official = true
                    OR si.is_public = true
                    OR si.created_by_user_id = :userId
                    OR si.created_by_user_id IS NULL
                    OR si.shared_campaign_ids && CAST(:memberCampaignIds AS bigint[])
                    OR :isPrivileged = true
              )
              AND (:filterByEntityTypes = false OR si.entity_type IN (:entityTypes))
              AND (CAST(:tier AS integer) IS NULL OR si.tier = :tier)
              AND (CAST(:expansionId AS bigint) IS NULL OR si.expansion_id = :expansionId)
              AND (CAST(:isOfficial AS boolean) IS NULL OR si.is_official = :isOfficial)
              AND (CAST(:cardType AS text) IS NULL OR si.card_type = :cardType)
              AND (CAST(:featureType AS text) IS NULL OR si.feature_type = :featureType)
              AND (CAST(:adversaryType AS text) IS NULL OR si.adversary_type = :adversaryType)
              AND (CAST(:domainCardType AS text) IS NULL OR si.domain_card_type = :domainCardType)
              AND (CAST(:associatedDomainId AS bigint) IS NULL OR si.associated_domain_id = :associatedDomainId)
              AND (CAST(:trait AS text) IS NULL OR si.trait = :trait)
              AND (CAST(:range AS text) IS NULL OR si.range = :range)
              AND (CAST(:burden AS text) IS NULL OR si.burden = :burden)
              AND (CAST(:isConsumable AS boolean) IS NULL OR si.is_consumable = :isConsumable)
            ORDER BY relevance_score DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM search_index si
            WHERE si.deleted_at IS NULL
              AND si.search_vector @@ plainto_tsquery('english', :query)
              AND (
                    si.is_official = true
                    OR si.is_public = true
                    OR si.created_by_user_id = :userId
                    OR si.created_by_user_id IS NULL
                    OR si.shared_campaign_ids && CAST(:memberCampaignIds AS bigint[])
                    OR :isPrivileged = true
              )
              AND (:filterByEntityTypes = false OR si.entity_type IN (:entityTypes))
              AND (CAST(:tier AS integer) IS NULL OR si.tier = :tier)
              AND (CAST(:expansionId AS bigint) IS NULL OR si.expansion_id = :expansionId)
              AND (CAST(:isOfficial AS boolean) IS NULL OR si.is_official = :isOfficial)
              AND (CAST(:cardType AS text) IS NULL OR si.card_type = :cardType)
              AND (CAST(:featureType AS text) IS NULL OR si.feature_type = :featureType)
              AND (CAST(:adversaryType AS text) IS NULL OR si.adversary_type = :adversaryType)
              AND (CAST(:domainCardType AS text) IS NULL OR si.domain_card_type = :domainCardType)
              AND (CAST(:associatedDomainId AS bigint) IS NULL OR si.associated_domain_id = :associatedDomainId)
              AND (CAST(:trait AS text) IS NULL OR si.trait = :trait)
              AND (CAST(:range AS text) IS NULL OR si.range = :range)
              AND (CAST(:burden AS text) IS NULL OR si.burden = :burden)
              AND (CAST(:isConsumable AS boolean) IS NULL OR si.is_consumable = :isConsumable)
            """,
        nativeQuery = true
    )
    Page<Object[]> search(
            @Param("query") String query,
            @Param("filterByEntityTypes") boolean filterByEntityTypes,
            @Param("entityTypes") List<String> entityTypes,
            @Param("tier") Integer tier,
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("cardType") String cardType,
            @Param("featureType") String featureType,
            @Param("adversaryType") String adversaryType,
            @Param("domainCardType") String domainCardType,
            @Param("associatedDomainId") Long associatedDomainId,
            @Param("trait") String trait,
            @Param("range") String range,
            @Param("burden") String burden,
            @Param("isConsumable") Boolean isConsumable,
            @Param("userId") Long userId,
            @Param("memberCampaignIds") String memberCampaignIds,
            @Param("isPrivileged") boolean isPrivileged,
            Pageable pageable
    );

    // -------------------------------------------------------------------------
    // Lookup helpers
    // -------------------------------------------------------------------------

    /**
     * Finds a search index entry by its entity type and entity ID combination.
     *
     * @param entityType the entity type string (e.g., "WEAPON")
     * @param entityId   the primary key of the referenced entity
     * @return an {@link Optional} containing the matching entry, or empty if not found
     */
    Optional<SearchIndex> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * Finds all search index entries for a given entity type.
     *
     * @param entityType the entity type string (e.g., "ADVERSARY")
     * @return a list of all matching entries, possibly including soft-deleted rows
     */
    List<SearchIndex> findByEntityType(String entityType);

    // -------------------------------------------------------------------------
    // Delete helpers
    // -------------------------------------------------------------------------

    /**
     * Hard-deletes a single search index entry by entity type and entity ID.
     * <p>
     * Use this when the referenced entity is permanently removed. For soft-deletion,
     * update the {@code deletedAt} field on the entity directly.
     * </p>
     *
     * @param entityType the entity type string
     * @param entityId   the primary key of the referenced entity
     */
    void deleteByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * Hard-deletes all search index entries for the given entity type.
     * <p>
     * Intended for bulk re-indexing scenarios where all rows of a given type must be
     * cleared before re-inserting updated data.
     * </p>
     *
     * @param entityType the entity type string
     */
    @Modifying
    @Query("DELETE FROM SearchIndex si WHERE si.entityType = :entityType")
    void deleteAllByEntityType(@Param("entityType") String entityType);

    // -------------------------------------------------------------------------
    // Upsert
    // -------------------------------------------------------------------------

    /**
     * Inserts a new search index row or updates the existing row for the given
     * {@code (entityType, entityId)} pair using a PostgreSQL {@code INSERT ... ON CONFLICT}
     * statement.
     *
     * <p>The {@code search_vector} column is computed from the supplied text arguments
     * using weighted {@code to_tsvector} calls:</p>
     * <ul>
     *   <li>Weight A — {@code nameText} (highest relevance)</li>
     *   <li>Weight B — {@code descriptionText}</li>
     *   <li>Weight C — {@code featureText}</li>
     * </ul>
     *
     * <p>On conflict, all mutable columns are updated to the new values and
     * {@code deleted_at} is reset to {@code NULL} (restoring a soft-deleted row).</p>
     *
     * @param entityType         the entity type string (e.g., "WEAPON")
     * @param entityId           the primary key of the referenced entity
     * @param name               the display name stored in the index
     * @param nameText           text used for weight-A FTS vector construction (usually same as name)
     * @param descriptionText    text used for weight-B FTS vector construction; may be {@code null}
     * @param featureText        text used for weight-C FTS vector construction; may be {@code null}
     * @param tier               optional tier level; may be {@code null}
     * @param expansionId        optional expansion foreign key; may be {@code null}
     * @param isOfficial         whether the entity is official content; may be {@code null}
     * @param isPublic           whether the entity is publicly visible; may be {@code null}
     * @param createdByUserId    the owning user's ID; may be {@code null} for system content
     * @param cardType           optional card type discriminator; may be {@code null}
     * @param featureType        optional feature type; may be {@code null}
     * @param adversaryType      optional adversary type; may be {@code null}
     * @param domainCardType     optional domain card type; may be {@code null}
     * @param associatedDomainId optional associated domain foreign key; may be {@code null}
     * @param trait              optional trait string; may be {@code null}
     * @param range              optional range string; may be {@code null}
     * @param burden             optional burden string; may be {@code null}
     * @param isPrimary          optional primary flag; may be {@code null}
     * @param damageType         optional damage type string; may be {@code null}
     * @param isConsumable       optional consumable flag; may be {@code null}
     * @param isMixed            optional mixed-type flag; may be {@code null}
     * @param subclassLevel      optional subclass level string; may be {@code null}
     * @param costTagCategory    optional cost tag category string; may be {@code null}
     * @param sharedCampaignIds  PostgreSQL {@code bigint[]} literal of the campaigns the item is
     *                           shared with, e.g. {@code "{3,7}"}; pass {@code "{}"} when there
     *                           are none. Bound as text and cast rather than as a collection,
     *                           because Hibernate expands a bound collection into one placeholder
     *                           per element — see
     *                           {@link com.aboff.core.util.PostgresArrayUtil}
     */
    @Modifying
    @Query(value = """
            INSERT INTO search_index (entity_type, entity_id, name, search_vector,
                tier, expansion_id, is_official, is_public, created_by_user_id,
                card_type, feature_type, adversary_type, domain_card_type,
                associated_domain_id, trait, range, burden, is_primary,
                damage_type, is_consumable, is_mixed, subclass_level,
                cost_tag_category, shared_campaign_ids, created_at, last_modified_at)
            VALUES (:entityType, :entityId, :name,
                setweight(to_tsvector('english', :nameText), 'A') ||
                setweight(to_tsvector('english', COALESCE(:descriptionText, '')), 'B') ||
                setweight(to_tsvector('english', COALESCE(:featureText, '')), 'C'),
                :tier, :expansionId, :isOfficial, :isPublic, :createdByUserId,
                :cardType, :featureType, :adversaryType, :domainCardType,
                :associatedDomainId, :trait, :range, :burden, :isPrimary,
                :damageType, :isConsumable, :isMixed, :subclassLevel,
                :costTagCategory, CAST(:sharedCampaignIds AS bigint[]), NOW(), NOW())
            ON CONFLICT (entity_type, entity_id) DO UPDATE SET
                name = EXCLUDED.name,
                search_vector = EXCLUDED.search_vector,
                tier = EXCLUDED.tier,
                expansion_id = EXCLUDED.expansion_id,
                is_official = EXCLUDED.is_official,
                is_public = EXCLUDED.is_public,
                created_by_user_id = EXCLUDED.created_by_user_id,
                card_type = EXCLUDED.card_type,
                feature_type = EXCLUDED.feature_type,
                adversary_type = EXCLUDED.adversary_type,
                domain_card_type = EXCLUDED.domain_card_type,
                associated_domain_id = EXCLUDED.associated_domain_id,
                trait = EXCLUDED.trait,
                range = EXCLUDED.range,
                burden = EXCLUDED.burden,
                is_primary = EXCLUDED.is_primary,
                damage_type = EXCLUDED.damage_type,
                is_consumable = EXCLUDED.is_consumable,
                is_mixed = EXCLUDED.is_mixed,
                subclass_level = EXCLUDED.subclass_level,
                cost_tag_category = EXCLUDED.cost_tag_category,
                shared_campaign_ids = EXCLUDED.shared_campaign_ids,
                deleted_at = NULL,
                last_modified_at = NOW()
            """, nativeQuery = true)
    void upsertSearchIndex(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("name") String name,
            @Param("nameText") String nameText,
            @Param("descriptionText") String descriptionText,
            @Param("featureText") String featureText,
            @Param("tier") Integer tier,
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("isPublic") Boolean isPublic,
            @Param("createdByUserId") Long createdByUserId,
            @Param("cardType") String cardType,
            @Param("featureType") String featureType,
            @Param("adversaryType") String adversaryType,
            @Param("domainCardType") String domainCardType,
            @Param("associatedDomainId") Long associatedDomainId,
            @Param("trait") String trait,
            @Param("range") String range,
            @Param("burden") String burden,
            @Param("isPrimary") Boolean isPrimary,
            @Param("damageType") String damageType,
            @Param("isConsumable") Boolean isConsumable,
            @Param("isMixed") Boolean isMixed,
            @Param("subclassLevel") String subclassLevel,
            @Param("costTagCategory") String costTagCategory,
            @Param("sharedCampaignIds") String sharedCampaignIds
    );
}

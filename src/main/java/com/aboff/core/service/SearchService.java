package com.aboff.core.service;

import com.aboff.core.model.dto.response.SearchResponse;
import com.aboff.core.model.dto.response.SearchResultResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.SearchIndexRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.dh.AdversaryService;
import com.aboff.core.service.dh.AncestryCardService;
import com.aboff.core.service.dh.ArmorService;
import com.aboff.core.service.dh.BeastformService;
import com.aboff.core.service.dh.CardCostTagService;
import com.aboff.core.service.dh.ClassService;
import com.aboff.core.service.dh.CommunityCardService;
import com.aboff.core.service.dh.DomainCardService;
import com.aboff.core.service.dh.DomainService;
import com.aboff.core.service.dh.EncounterService;
import com.aboff.core.service.dh.EnvironmentService;
import com.aboff.core.service.dh.ExpansionService;
import com.aboff.core.service.dh.FeatureService;
import com.aboff.core.service.dh.LootService;
import com.aboff.core.service.dh.QuestionService;
import com.aboff.core.service.dh.SubclassCardService;
import com.aboff.core.service.dh.SubclassPathService;
import com.aboff.core.service.dh.TransformationCardService;
import com.aboff.core.service.dh.WeaponService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for executing full-text search queries across all indexed game content.
 *
 * <p>Delegates to {@link SearchIndexRepository} for PostgreSQL full-text search and
 * optionally resolves matched entity IDs to full response objects using the appropriate
 * game-content services.
 *
 * <p>Access control is enforced at the search index level: non-privileged users only
 * receive results they are permitted to view (official, public, or their own content).
 * Privileged users (MODERATOR and above) bypass these restrictions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    // -------------------------------------------------------------------------
    // Column indices in the Object[] row returned by SearchIndexRepository.search()
    // The native query selects si.* (all columns in migration order) then appends relevance_score.
    // -------------------------------------------------------------------------
    private static final int COL_ID = 0;
    private static final int COL_ENTITY_TYPE = 1;
    private static final int COL_ENTITY_ID = 2;
    private static final int COL_NAME = 3;
    // COL_SEARCH_VECTOR = 4  (not needed at application layer)
    // ... filter columns follow ...
    private static final int COL_RELEVANCE_SCORE = 27;

    private final SearchIndexRepository searchIndexRepository;
    private final RoleHierarchyService roleHierarchyService;

    // Game-content services for optional entity expansion
    private final AdversaryService adversaryService;
    private final AncestryCardService ancestryCardService;
    private final ArmorService armorService;
    private final BeastformService beastformService;
    private final CardCostTagService cardCostTagService;
    private final ClassService classService;
    private final CommunityCardService communityCardService;
    private final DomainCardService domainCardService;
    private final DomainService domainService;
    private final EncounterService encounterService;
    private final EnvironmentService environmentService;
    private final ExpansionService expansionService;
    private final FeatureService featureService;
    private final LootService lootService;
    private final QuestionService questionService;
    private final SubclassCardService subclassCardService;
    private final SubclassPathService subclassPathService;
    private final TransformationCardService transformationCardService;
    private final WeaponService weaponService;

    /**
     * Performs a paginated full-text search across all indexed game content.
     *
     * <p>The query is matched against the PostgreSQL {@code tsvector} search index using
     * {@code plainto_tsquery}. Results are ranked by relevance score (descending). All filter
     * parameters are optional; passing {@code null} disables that filter.
     *
     * <p>When {@code expand} contains {@code "entity"} (or {@code "all"}), each result row is
     * resolved to its full response DTO via the appropriate service. Expansion failures are
     * logged and silently skipped so that a single missing entity does not abort the search.
     *
     * @param query              the user-supplied search string; must not be null or blank
     * @param types              optional list of entity types to restrict results to
     * @param tier               optional tier level filter
     * @param expansionId        optional expansion FK filter
     * @param isOfficial         optional official-content filter
     * @param cardType           optional card type filter
     * @param featureType        optional feature type filter
     * @param adversaryType      optional adversary type filter
     * @param domainCardType     optional domain card type filter
     * @param associatedDomainId optional associated domain FK filter
     * @param trait              optional trait filter
     * @param range              optional range filter
     * @param burden             optional burden filter
     * @param isConsumable       optional consumable flag filter
     * @param expand             comma-separated list of expansion keys; {@code "entity"} or
     *                           {@code "all"} triggers full entity hydration
     * @param page               zero-based page index
     * @param size               page size (capped at 100)
     * @param user               the authenticated user, used for access control
     * @return a paginated {@link SearchResponse} containing matched results
     * @throws IllegalArgumentException if {@code query} is null or blank
     */
    @Transactional(readOnly = true)
    public SearchResponse search(
            String query,
            List<SearchableEntityType> types,
            Integer tier,
            Long expansionId,
            Boolean isOfficial,
            String cardType,
            String featureType,
            String adversaryType,
            String domainCardType,
            Long associatedDomainId,
            String trait,
            String range,
            String burden,
            Boolean isConsumable,
            String expand,
            int page,
            int size,
            User user) {

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }

        size = Math.min(size, 100);

        boolean isPrivileged = roleHierarchyService.isPrivilegedRole(user.getRole());
        boolean filterByEntityTypes = types != null && !types.isEmpty();
        // The native query always references :entityTypes inside an IN clause, so we must
        // pass a non-null, non-empty list even when no filter is requested. The sentinel is
        // never evaluated because :filterByEntityTypes = false short-circuits the OR.
        List<String> entityTypeStrings = filterByEntityTypes
                ? types.stream().map(SearchableEntityType::name).toList()
                : List.of("");

        log.debug("Executing search: query='{}', types={}, tier={}, page={}, size={}, privileged={}",
                query, filterByEntityTypes ? entityTypeStrings : "(none)", tier, page, size, isPrivileged);

        Page<Object[]> resultPage = searchIndexRepository.search(
                query,
                filterByEntityTypes,
                entityTypeStrings,
                tier,
                expansionId,
                isOfficial,
                cardType,
                featureType,
                adversaryType,
                domainCardType,
                associatedDomainId,
                trait,
                range,
                burden,
                isConsumable,
                user.getId(),
                isPrivileged,
                PageRequest.of(page, size));

        boolean expandEntity = expand != null && (expand.contains("entity") || expand.contains("all"));

        // Build an Authentication token once (used by services that require it)
        Authentication auth = buildAuthentication(user);

        List<SearchResultResponse> results = resultPage.getContent().stream()
                .map(row -> mapRow(row, expandEntity, expand, auth))
                .toList();

        log.debug("Search returned {} results (total {})", results.size(), resultPage.getTotalElements());

        return SearchResponse.builder()
                .results(results)
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .currentPage(resultPage.getNumber())
                .pageSize(resultPage.getSize())
                .query(query)
                .build();
    }

    /**
     * Maps a single raw {@code Object[]} row from the native search query to a
     * {@link SearchResultResponse}.
     *
     * @param row          the raw result row from the repository
     * @param expandEntity whether to resolve and attach the full entity DTO
     * @param expand       the original expand string, forwarded to individual services
     * @param auth         the authentication token for services that require it
     * @return the mapped search result
     */
    private SearchResultResponse mapRow(Object[] row, boolean expandEntity, String expand, Authentication auth) {
        String entityTypeStr = (String) row[COL_ENTITY_TYPE];
        Long entityId = toLong(row[COL_ENTITY_ID]);
        String name = (String) row[COL_NAME];
        Double relevanceScore = toDouble(row[COL_RELEVANCE_SCORE]);

        SearchableEntityType type;
        try {
            type = SearchableEntityType.valueOf(entityTypeStr);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown entity type in search index: {}", entityTypeStr);
            return SearchResultResponse.builder()
                    .type(null)
                    .id(entityId)
                    .name(name)
                    .relevanceScore(relevanceScore)
                    .build();
        }

        Object expandedEntity = null;
        if (expandEntity) {
            expandedEntity = resolveEntity(type, entityId, expand, auth);
        }

        return SearchResultResponse.builder()
                .type(type)
                .id(entityId)
                .name(name)
                .relevanceScore(relevanceScore)
                .expandedEntity(expandedEntity)
                .build();
    }

    /**
     * Resolves the full entity response DTO for the given type and ID by delegating to the
     * appropriate game-content service.
     *
     * <p>Resolution failures (entity not found, access denied) are caught and logged so that
     * a single unavailable entity does not prevent other results from being returned.
     *
     * @param type   the entity type to resolve
     * @param id     the entity primary key
     * @param expand the expand string to forward to the service
     * @param auth   the authentication token for services that require it
     * @return the resolved response DTO, or {@code null} if resolution fails
     */
    private Object resolveEntity(SearchableEntityType type, Long id, String expand, Authentication auth) {
        try {
            return switch (type) {
                case WEAPON -> weaponService.getWeaponById(id, expand);
                case ARMOR -> armorService.getArmorById(id, expand);
                case LOOT -> lootService.getLootById(id, expand);
                case DOMAIN -> domainService.getDomainById(id, expand);
                case CLASS -> classService.getClassById(id, expand);
                case FEATURE -> featureService.getFeatureById(id, expand);
                case ANCESTRY_CARD -> ancestryCardService.getAncestryCardById(id, expand);
                case COMMUNITY_CARD -> communityCardService.getCommunityCardById(id, expand);
                case SUBCLASS_CARD -> subclassCardService.getSubclassCardById(id, expand);
                case DOMAIN_CARD -> domainCardService.getDomainCardById(id, expand);
                case SUBCLASS_PATH -> subclassPathService.getSubclassPathById(id, expand);
                case QUESTION -> questionService.getQuestionById(id, expand);
                case EXPANSION -> expansionService.getExpansionById(id);
                case CARD_COST_TAG -> cardCostTagService.getCostTagById(id);
                case TRANSFORMATION_CARD -> transformationCardService.getTransformationCardById(id, expand);
                case ADVERSARY -> adversaryService.getAdversaryById(id, expand, auth);
                case ENCOUNTER -> encounterService.getEncounterById(id, expand, auth);
                case ENVIRONMENT -> environmentService.getEnvironmentById(id, expand, auth);
                case BEASTFORM -> beastformService.getBeastformById(id, expand);
            };
        } catch (EntityNotFoundException e) {
            log.warn("Entity not found during search expansion: type={}, id={}", type, id);
            return null;
        } catch (Exception e) {
            log.warn("Failed to resolve entity during search expansion: type={}, id={}, error={}",
                    type, id, e.getMessage());
            return null;
        }
    }

    /**
     * Constructs a Spring Security {@link Authentication} token backed by a
     * {@link CustomUserDetails} for the given user.
     *
     * <p>This token is used when delegating to services (e.g., {@link AdversaryService},
     * {@link EncounterService}) that require an {@link Authentication} parameter for
     * ownership and access-control validation.
     *
     * @param user the authenticated user
     * @return a pre-authenticated token containing the user's details and authorities
     */
    private Authentication buildAuthentication(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    /**
     * Safely converts a value from the result row to {@link Long}.
     *
     * @param value the raw column value
     * @return the long value, or {@code null} if the input is null
     */
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    /**
     * Safely converts a value from the result row to {@link Double}.
     *
     * @param value the raw column value
     * @return the double value, or {@code null} if the input is null
     */
    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Double d) return d;
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
    }
}

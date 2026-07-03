package com.aboff.core.controller;

import com.aboff.core.model.dto.response.SearchResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller providing the full-text search API endpoint.
 *
 * <p>Exposes {@code GET /api/search} to allow authenticated users to search across all
 * indexed Daggerheart game content using a keyword query. Optional filter parameters
 * enable callers to narrow results by entity type, tier, expansion, and many other
 * entity-specific attributes.
 *
 * <p>Results are paginated and ranked by relevance score. The optional {@code expand}
 * parameter triggers full entity hydration so that clients can retrieve complete
 * entity data in a single request.
 *
 * <p>Access control is enforced transparently: non-privileged users only receive results
 * for content they are permitted to view (official, public, or their own). Privileged
 * users (MODERATOR and above) bypass these restrictions.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;

    /**
     * Performs a full-text search across all indexed game content.
     *
     * <p>The {@code q} parameter is matched against the PostgreSQL {@code tsvector} index
     * using {@code plainto_tsquery}. All filter parameters are optional; omitting them
     * returns results across all matching entity types and attribute values.
     *
     * <p>Example requests:
     * <ul>
     *   <li>{@code GET /api/search?q=flame+sword} — keyword search across all entity types</li>
     *   <li>{@code GET /api/search?q=flame&types=WEAPON,ARMOR&tier=2} — filter by type and tier</li>
     *   <li>{@code GET /api/search?q=dragon&expand=entity} — include full entity DTOs in results</li>
     * </ul>
     *
     * @param q                  the search query string; required
     * @param types              optional list of {@link SearchableEntityType} values to restrict results
     * @param tier               optional tier level filter
     * @param expansionId        optional expansion foreign key filter
     * @param isOfficial         optional filter to include only official or non-official content
     * @param cardType           optional card type filter (e.g., "ANCESTRY", "DOMAIN")
     * @param featureType        optional feature type filter (e.g., "CLASS_FEATURE")
     * @param adversaryType      optional adversary role filter (e.g., "MINION", "LEADER")
     * @param domainCardType     optional domain card type filter (e.g., "ABILITY", "SPELL")
     * @param associatedDomainId optional filter by associated domain primary key
     * @param trait              optional trait filter (e.g., "AGILITY", "STRENGTH")
     * @param range              optional range filter (e.g., "MELEE", "RANGED")
     * @param burden             optional burden filter (e.g., "ONE_HANDED", "TWO_HANDED")
     * @param isConsumable       optional filter to restrict to consumable or non-consumable items
     * @param creatorId          optional filter restricting results to content created by this user
     * @param expand             comma-separated list of expansion keys; pass {@code "entity"} or
     *                           {@code "all"} to include full entity response DTOs in results
     * @param page               zero-based page index (default {@code 0})
     * @param size               page size (default {@code 20}; capped at {@code 100} in the service)
     * @param userDetails        the authenticated user's details, injected by Spring Security
     * @return a {@link ResponseEntity} wrapping the paginated {@link SearchResponse}
     */
    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam String q,
            @RequestParam(required = false) List<SearchableEntityType> types,
            @RequestParam(required = false) Integer tier,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) Boolean isOfficial,
            @RequestParam(required = false) String cardType,
            @RequestParam(required = false) String featureType,
            @RequestParam(required = false) String adversaryType,
            @RequestParam(required = false) String domainCardType,
            @RequestParam(required = false) Long associatedDomainId,
            @RequestParam(required = false) String trait,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) String burden,
            @RequestParam(required = false) Boolean isConsumable,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) String expand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = userDetails.getUser();
        log.debug("Search request: q='{}', types={}, tier={}, page={}", q, types, tier, page);

        SearchResponse response = searchService.search(
                q, types, tier, expansionId, isOfficial,
                cardType, featureType, adversaryType, domainCardType, associatedDomainId,
                trait, range, burden, isConsumable, creatorId, expand, page, size, user);

        return ResponseEntity.ok(response);
    }
}

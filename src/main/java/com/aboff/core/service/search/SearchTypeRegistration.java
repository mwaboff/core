package com.aboff.core.service.search;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.SearchableEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;

/**
 * Bundles everything the search infrastructure needs to know about a single
 * {@link SearchableEntityType} into one place.
 *
 * <p>Before this interface existed, adding a searchable entity required hand-editing two
 * separate switch statements that had to stay in sync with each other and with the
 * {@link SearchableEntityType} enum: {@code SearchIndexService#resolveRepository} and
 * {@code SearchService#resolveEntity}. Both switches were exhaustive over the enum (so a
 * missing arm was a compile error), but a present-and-wrong arm — most notably a stale
 * {@code case BEASTFORM -> null;} — compiled cleanly and failed only at runtime, silently.
 * For {@code resolveRepository}, that failure is destructive: {@code reindexAll(type)} deletes
 * every existing {@code search_index} row for that type before consulting the repository, so a
 * null repository means those rows are gone and can never be repopulated. See
 * {@code .research/search-registration-chokepoint.md} for the full incident history.
 *
 * <p>Each {@link SearchableEntityType} now has exactly one Spring bean implementing this
 * interface (see the {@code registration} sub-package). {@link SearchTypeRegistry} collects
 * every such bean at startup, keys them by {@link #type()}, and verifies the result covers
 * every {@link SearchableEntityType} constant — a missing or duplicated registration fails
 * {@code ApplicationContext} startup with a descriptive {@link IllegalStateException} rather
 * than compiling cleanly and misbehaving at runtime.
 *
 * <p>Registering a new searchable entity type is now a single step: add one
 * {@code @Component} implementing this interface for the new type. There is no second switch
 * to remember.
 *
 * <p>Deliberately out of scope: {@code SearchFieldMapping#buildSearchIndexData}'s per-type
 * dispatch switch is <strong>not</strong> collapsed into this registry. Unlike the two switches
 * above, that one has no legal way to produce a wrong-but-non-null result for the wrong reason
 * — every arm is a hardcoded call to a same-file sibling method, there is no repository/service
 * bean to forget wiring in, and a missing arm is a plain compile error. Folding it in here would
 * require breaking apart its extensive, entity-by-entity {@code SearchFieldMappingTest} suite for
 * no corresponding safety gain.
 */
public interface SearchTypeRegistration {

    /**
     * The {@link SearchableEntityType} this registration is responsible for.
     *
     * @return the entity type
     */
    SearchableEntityType type();

    /**
     * The repository used to re-index every row of this type from source data.
     *
     * <p>Used by {@code SearchIndexService#reindexAll(SearchableEntityType)}, which deletes all
     * existing {@code search_index} rows for {@link #type()} and repopulates them from
     * {@code repository().findAll()}. Must never return {@code null}.
     *
     * @return the JPA repository backing this entity type
     */
    JpaRepository<? extends BaseEntity, Long> repository();

    /**
     * Resolves a single entity of this type to its full response DTO, for {@code ?expand=entity}.
     *
     * @param id     the entity's primary key
     * @param expand the original expand string, forwarded to the backing service for its own
     *               nested expansion support
     * @param auth   the caller's authentication, forwarded to services that enforce
     *               ownership/visibility checks; {@code null} for services that do not need it
     * @return the resolved response DTO; implementations should let the backing service's own
     *         not-found/access-denied exceptions propagate — {@link SearchService} catches them
     */
    Object resolveEntity(Long id, String expand, Authentication auth);
}

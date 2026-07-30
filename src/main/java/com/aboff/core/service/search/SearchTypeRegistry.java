package com.aboff.core.service.search;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.SearchableEntityType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Derived registry of every {@link SearchTypeRegistration}, keyed by {@link SearchableEntityType}.
 *
 * <p>Spring supplies every {@link SearchTypeRegistration} bean in the application context via
 * constructor injection of {@code List<SearchTypeRegistration>} — there is no hand-maintained
 * list of implementations to keep in sync. The constructor then validates, once, that the
 * supplied registrations cover every {@link SearchableEntityType} constant exactly once. If a
 * type is missing or claimed by more than one bean, construction throws
 * {@link IllegalStateException}, which fails {@code ApplicationContext} startup — converting the
 * silent-null failure mode described in {@link SearchTypeRegistration}'s javadoc into a boot
 * failure that cannot reach production, and cannot be missed the way a switch's {@code -> null}
 * arm can be.
 *
 * <p>{@code SearchIndexService} and {@code SearchService} depend on this registry instead of on
 * every individual repository/service bean and instead of hand-maintained dispatch switches.
 */
@Component
@Slf4j
public class SearchTypeRegistry {

    private final Map<SearchableEntityType, SearchTypeRegistration> registrationsByType;

    /**
     * Builds the registry from every {@link SearchTypeRegistration} bean Spring can find, and
     * immediately validates completeness and uniqueness.
     *
     * @param registrations every {@link SearchTypeRegistration} bean in the application context;
     *                       order is irrelevant
     * @throws IllegalStateException if any {@link SearchableEntityType} constant has zero or more
     *                                than one registration
     */
    public SearchTypeRegistry(List<SearchTypeRegistration> registrations) {
        Map<SearchableEntityType, SearchTypeRegistration> byType = new EnumMap<>(SearchableEntityType.class);
        List<String> duplicates = new ArrayList<>();

        for (SearchTypeRegistration registration : registrations) {
            SearchTypeRegistration existing = byType.putIfAbsent(registration.type(), registration);
            if (existing != null) {
                duplicates.add(registration.type() + " is claimed by both "
                        + existing.getClass().getSimpleName() + " and "
                        + registration.getClass().getSimpleName());
            }
        }

        if (!duplicates.isEmpty()) {
            throw new IllegalStateException(
                    "Duplicate SearchTypeRegistration beans found, one type must have exactly one "
                            + "registration: " + duplicates);
        }

        Set<SearchableEntityType> missing = EnumSet.allOf(SearchableEntityType.class);
        missing.removeAll(byType.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "No SearchTypeRegistration bean found for: " + missing + ". Every "
                            + "SearchableEntityType constant must have exactly one @Component "
                            + "implementing SearchTypeRegistration (see "
                            + "com.aboff.core.service.search.registration). Without one, "
                            + "SearchIndexService#reindexAll(type) would delete every existing "
                            + "search_index row for this type and be unable to repopulate them, and "
                            + "SearchService#resolveEntity(type, ...) would silently return null for "
                            + "?expand=entity.");
        }

        this.registrationsByType = Collections.unmodifiableMap(byType);
        log.info("Initialized SearchTypeRegistry with {} entity type registrations",
                registrationsByType.size());
    }

    /**
     * Returns the repository used to re-index every row of the given type.
     *
     * @param type the searchable entity type
     * @return the repository backing {@code type}; never {@code null}
     */
    public JpaRepository<? extends BaseEntity, Long> repositoryFor(SearchableEntityType type) {
        return registrationFor(type).repository();
    }

    /**
     * Resolves a single entity of the given type to its full response DTO.
     *
     * @param type   the searchable entity type
     * @param id     the entity's primary key
     * @param expand the original expand string, forwarded to the backing service
     * @param auth   the caller's authentication, forwarded to services that require it
     * @return the resolved response DTO, or whatever the backing service returns for a miss
     */
    public Object resolveEntity(SearchableEntityType type, Long id, String expand, Authentication auth) {
        return registrationFor(type).resolveEntity(id, expand, auth);
    }

    /**
     * Looks up the registration for a type.
     *
     * <p>This should be unreachable in production: the constructor already verified every
     * {@link SearchableEntityType} constant has a registration. It remains as a defensive guard
     * rather than an unchecked {@code Map.get} so a future change that bypasses the constructor
     * (e.g. a test constructing this class some other way) fails loudly instead of NPEing.
     *
     * @param type the searchable entity type
     * @return the registration for {@code type}
     * @throws IllegalStateException if no registration exists for {@code type}
     */
    private SearchTypeRegistration registrationFor(SearchableEntityType type) {
        SearchTypeRegistration registration = registrationsByType.get(type);
        if (registration == null) {
            throw new IllegalStateException("No SearchTypeRegistration registered for type=" + type);
        }
        return registration;
    }
}

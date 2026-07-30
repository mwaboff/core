package com.aboff.core.service;

import com.aboff.core.model.enums.SearchableEntityType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architecture test for the search-registration chokepoint described in
 * {@code .research/search-registration-chokepoint.md}.
 *
 * <p>{@link SearchIndexService#resolveRepository} and {@link SearchService#resolveEntity} are
 * both arrow-syntax switch expressions over every {@link SearchableEntityType} constant with no
 * {@code default} arm, so the compiler already forces a switch arm to exist for every enum
 * value -- that part is safe. What is NOT compiler-checked is whether that arm is wired to
 * something real: {@code case BEASTFORM -> null;} type-checks and compiles cleanly, and is
 * indistinguishable from a working arm until it runs.
 *
 * <p>The consequences of a null arm are silent, and in one direction destructive:
 * <ul>
 *   <li>{@code resolveRepository()} feeds {@code reindexAll(type)}, which unconditionally
 *       deletes every existing {@code search_index} row for that type BEFORE consulting the
 *       repository. A null repository means those rows are deleted and can never be
 *       repopulated -- worse than a no-op.</li>
 *   <li>{@code resolveEntity()} feeds {@code ?expand=entity} on search results; a null arm means
 *       that expansion silently returns null for every result of that type, forever, with no
 *       error anywhere in the response.</li>
 * </ul>
 *
 * <p>This is exactly how Beastform shipped (fixed in PR #55), and on 2026-07-30 three of five
 * entity branches in flight still carried a stale {@code case BEASTFORM -> null;} left over from
 * before that fix -- caught only by a human directly grepping each branch, not by any automated
 * check. This test makes that check automatic.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li><b>Enumeration</b> iterates {@link SearchableEntityType#values()} directly rather than a
 *       hardcoded list of names, so a newly added enum constant is covered with zero edits to
 *       this file -- that property is most of this test's value. A hardcoded list would silently
 *       stop growing the moment someone adds a 19th type and forgets this file too, which is
 *       exactly the failure mode under test.</li>
 *   <li><b>Invocation</b>: both methods under test are {@code private}, and testing through their
 *       public entry points was rejected. {@code resolveEntity()} wraps its switch in a
 *       try/catch that turns EVERY exception -- including "entity 1 doesn't exist," which is
 *       guaranteed without a real database -- into the same null this test is trying to catch;
 *       going through {@code reindexAll}/the search API would make a real failure
 *       indistinguishable from a database-less test environment. Instead, each service under
 *       test is constructed directly with a Mockito mock for every constructor dependency
 *       (discovered via reflection, not a hand-maintained list, so a new dependency is
 *       automatically mocked rather than silently left null), and the private methods are
 *       invoked directly via reflection. This is a plain unit test with no Spring context and no
 *       database -- the least fragile option available, and the fastest.</li>
 *   <li><b>Distinguishing "wired but empty" from "not wired at all"</b>: every mocked
 *       service/repository method is given a default answer that returns a real, non-null
 *       instance of its declared return type (see {@link #NON_NULL_ANSWER}). A working arm
 *       therefore returns non-null THROUGH the mock, while a {@code -> null} arm returns a null
 *       literal WITHOUT ever calling the mock -- the two are only distinguishable this way,
 *       which is why a bare {@code mock(Service.class)} with Mockito's ordinary
 *       null-returning default stubbing would not work here.</li>
 *   <li><b>Exemptions</b>: {@link #REPOSITORY_EXEMPT} and {@link #ENTITY_EXEMPT} are explicit,
 *       documented allowlists for a type that genuinely has no repository or service by design.
 *       Both are empty: as of this test, all 18 {@link SearchableEntityType} constants resolve
 *       non-null from both methods (confirmed by reading both switches directly in
 *       {@code SearchIndexService.java}/{@code SearchService.java}, not assumed). If that stops
 *       being true for a legitimate reason, add the type here with a comment explaining why --
 *       do not weaken the assertions below instead.</li>
 * </ul>
 */
class SearchRegistrationArchitectureTest {

    /**
     * Types with no legitimate repository for {@link SearchIndexService#resolveRepository}.
     * Empty -- see the "Exemptions" note in the class javadoc.
     */
    private static final Set<SearchableEntityType> REPOSITORY_EXEMPT = EnumSet.noneOf(SearchableEntityType.class);

    /**
     * Types with no legitimate service for {@link SearchService#resolveEntity}.
     * Empty -- see the "Exemptions" note in the class javadoc.
     */
    private static final Set<SearchableEntityType> ENTITY_EXEMPT = EnumSet.noneOf(SearchableEntityType.class);

    @Test
    void everySearchableEntityTypeResolvesARepository() throws Exception {
        SearchIndexService service = instantiateWithMocks(SearchIndexService.class);
        Method resolveRepository =
                SearchIndexService.class.getDeclaredMethod("resolveRepository", SearchableEntityType.class);
        resolveRepository.setAccessible(true);

        List<String> failures = new ArrayList<>();
        for (SearchableEntityType type : SearchableEntityType.values()) {
            if (REPOSITORY_EXEMPT.contains(type)) {
                continue;
            }
            Object result;
            try {
                result = resolveRepository.invoke(service, type);
            } catch (InvocationTargetException e) {
                failures.add(type + ": SearchIndexService.resolveRepository() threw "
                        + e.getCause() + " instead of returning a repository.");
                continue;
            }
            if (result == null) {
                failures.add(type + ": SearchIndexService.resolveRepository() returns null. "
                        + "reindexAll(" + type + ") unconditionally deletes every existing "
                        + "search_index row for this type BEFORE checking the repository -- a null "
                        + "repository means those rows are deleted and can never be repopulated. "
                        + "Add a `case " + type + " -> <repository>;` arm.");
            }
        }

        assertThat(failures)
                .as("Every SearchableEntityType must resolve a repository in "
                        + "SearchIndexService.resolveRepository() (see class javadoc for why this matters)")
                .isEmpty();
    }

    @Test
    void everySearchableEntityTypeResolvesAnEntity() throws Exception {
        SearchService service = instantiateWithMocks(SearchService.class);
        Method resolveEntity = SearchService.class.getDeclaredMethod(
                "resolveEntity", SearchableEntityType.class, Long.class, String.class, Authentication.class);
        resolveEntity.setAccessible(true);

        List<String> failures = new ArrayList<>();
        for (SearchableEntityType type : SearchableEntityType.values()) {
            if (ENTITY_EXEMPT.contains(type)) {
                continue;
            }
            Object result;
            try {
                result = resolveEntity.invoke(service, type, 1L, null, null);
            } catch (InvocationTargetException e) {
                failures.add(type + ": SearchService.resolveEntity() threw "
                        + e.getCause() + " instead of returning an entity.");
                continue;
            }
            if (result == null) {
                failures.add(type + ": SearchService.resolveEntity() returns null even though its "
                        + "mocked backing service returned a non-null response -- the switch arm for "
                        + "this type is not wired to a service call at all. `?expand=entity` will "
                        + "silently return null for every search result of this type. Add a "
                        + "`case " + type + " -> <service>.get...;` arm.");
            }
        }

        assertThat(failures)
                .as("Every SearchableEntityType must resolve an entity in "
                        + "SearchService.resolveEntity() (see class javadoc for why this matters)")
                .isEmpty();
    }

    /**
     * Builds {@code type} via its single (Lombok {@code @RequiredArgsConstructor}-generated)
     * constructor, supplying a Mockito mock for every parameter.
     *
     * <p>Reflection-driven rather than a hand-maintained list of dependencies, so that adding a
     * new constructor dependency (e.g. a new entity type's repository or service) is
     * automatically covered with no edit to this file.
     *
     * @param type the class to instantiate; must declare exactly one constructor
     * @return an instance backed entirely by mocks
     */
    private static <T> T instantiateWithMocks(Class<T> type) throws ReflectiveOperationException {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        if (constructors.length != 1) {
            throw new IllegalStateException(type + " must have exactly one constructor for this test to "
                    + "reflectively populate it with mocks; found " + constructors.length);
        }
        Constructor<?> constructor = constructors[0];
        constructor.setAccessible(true);

        Class<?>[] paramTypes = constructor.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            args[i] = Mockito.mock(paramTypes[i], NON_NULL_ANSWER);
        }

        @SuppressWarnings("unchecked")
        T instance = (T) constructor.newInstance(args);
        return instance;
    }

    /**
     * Default answer for every mocked service/repository method call: returns a real, non-null
     * instance of the method's declared return type (via its no-arg constructor, which every
     * response DTO in this codebase has as a plain Lombok class; falling back to a Mockito mock
     * of the return type if no no-arg constructor exists), and Mockito's ordinary default for
     * void/primitive returns.
     *
     * <p>This is what makes a "wired to a working service" arm distinguishable from a
     * {@code -> null} arm: the former returns this non-null value THROUGH the mock; the latter
     * returns a null literal without ever invoking the mock at all.
     */
    private static final Answer<Object> NON_NULL_ANSWER = invocation -> {
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (returnType == void.class || returnType == Void.class || returnType.isPrimitive()) {
            return Mockito.RETURNS_DEFAULTS.answer(invocation);
        }
        try {
            Constructor<?> noArgConstructor = returnType.getDeclaredConstructor();
            noArgConstructor.setAccessible(true);
            return noArgConstructor.newInstance();
        } catch (ReflectiveOperationException e) {
            return Mockito.mock(returnType);
        }
    };
}

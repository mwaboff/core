package com.aboff.core.service;

import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.service.search.SearchTypeRegistration;
import com.aboff.core.service.search.SearchTypeRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Architecture test for the search-registration chokepoint described in
 * {@code .research/search-registration-chokepoint.md}.
 *
 * <p>{@code SearchIndexService#resolveRepository} and {@code SearchService#resolveEntity} used to
 * be hand-maintained switch expressions over every {@link SearchableEntityType} constant. Both
 * were exhaustive with no {@code default} arm, so the compiler already forced a switch arm to
 * exist for every enum value — that part was safe. What was NOT compiler-checked was whether that
 * arm was wired to something real: {@code case BEASTFORM -> null;} type-checked and compiled
 * cleanly, and was indistinguishable from a working arm until it ran.
 *
 * <p>Both switches have been replaced by {@link SearchTypeRegistry}, a derived registry built
 * from one {@link SearchTypeRegistration} Spring bean per {@link SearchableEntityType} (see the
 * {@code com.aboff.core.service.search.registration} package). This test now verifies two
 * properties, both stronger than the switch-based test it replaces:
 *
 * <ul>
 *   <li><b>Every production registration is wired to something real</b> — {@link #everyProductionRegistrationResolvesARepositoryAndAnEntity()}
 *       discovers every {@code @Component} implementing {@link SearchTypeRegistration} via
 *       classpath scanning (not a hardcoded list — a new registration is covered with zero edits
 *       to this file, exactly like the test it replaces), instantiates each with mocked
 *       dependencies, and asserts {@link SearchTypeRegistration#repository()} and
 *       {@link SearchTypeRegistration#resolveEntity} both return real, non-null values obtained
 *       <em>through</em> the mock rather than a null literal that bypasses it.</li>
 *   <li><b>A missing or duplicated registration fails registry construction, not silently</b> —
 *       {@link #missingRegistration_FailsRegistryConstruction()} and
 *       {@link #duplicateRegistration_FailsRegistryConstruction()} prove the exact guarantee the
 *       old test could only prove indirectly (by checking every arm was non-null): that omitting
 *       a type is no longer possible to do quietly. Because {@link SearchTypeRegistry}'s
 *       validation runs in its constructor, this failure mode now surfaces at Spring
 *       {@code ApplicationContext} startup, not only when {@code ./mvnw verify} happens to be
 *       run.</li>
 * </ul>
 */
class SearchRegistrationArchitectureTest {

    private static final String REGISTRATION_PACKAGE = "com.aboff.core.service.search.registration";

    @Test
    void everyProductionRegistrationResolvesARepositoryAndAnEntity() throws Exception {
        List<SearchTypeRegistration> registrations = instantiateProductionRegistrations();

        // Constructing the registry from every real, discovered registration must not throw --
        // i.e. every SearchableEntityType constant has exactly one @Component covering it.
        SearchTypeRegistry registry = new SearchTypeRegistry(registrations);

        List<String> failures = new ArrayList<>();
        for (SearchableEntityType type : SearchableEntityType.values()) {
            try {
                Object repository = registry.repositoryFor(type);
                if (repository == null) {
                    failures.add(type + ": SearchTypeRegistration#repository() returned null. "
                            + "reindexAll(" + type + ") unconditionally deletes every existing "
                            + "search_index row for this type BEFORE checking the repository -- a "
                            + "null repository means those rows are deleted and can never be "
                            + "repopulated.");
                }
            } catch (Exception e) {
                failures.add(type + ": repositoryFor() threw " + e);
            }

            try {
                Object entity = registry.resolveEntity(type, 1L, null, null);
                if (entity == null) {
                    failures.add(type + ": SearchTypeRegistration#resolveEntity() returned null "
                            + "even though its mocked backing service returns a non-null response "
                            + "-- the registration is not wired to a service call at all. "
                            + "`?expand=entity` will silently return null for every search result "
                            + "of this type.");
                }
            } catch (Exception e) {
                failures.add(type + ": resolveEntity() threw " + e);
            }
        }

        assertThat(failures)
                .as("Every SearchableEntityType must resolve a repository and an entity through "
                        + "its SearchTypeRegistration bean (see class javadoc for why this matters)")
                .isEmpty();
    }

    @Test
    void missingRegistration_FailsRegistryConstruction() throws Exception {
        // Arrange — every production registration except BEASTFORM's, reproducing the exact
        // incident this registry exists to prevent (a forgotten/stale Beastform registration).
        List<SearchTypeRegistration> registrations = instantiateProductionRegistrations();
        registrations.removeIf(r -> r.type() == SearchableEntityType.BEASTFORM);

        // Act & Assert
        assertThatThrownBy(() -> new SearchTypeRegistry(registrations))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BEASTFORM");
    }

    @Test
    void duplicateRegistration_FailsRegistryConstruction() throws Exception {
        // Arrange — two registrations both claiming WEAPON
        List<SearchTypeRegistration> registrations = instantiateProductionRegistrations();
        SearchTypeRegistration weaponRegistration = registrations.stream()
                .filter(r -> r.type() == SearchableEntityType.WEAPON)
                .findFirst()
                .orElseThrow();
        registrations.add(weaponRegistration);

        // Act & Assert
        assertThatThrownBy(() -> new SearchTypeRegistry(registrations))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WEAPON");
    }

    /**
     * Discovers every {@code @Component} implementing {@link SearchTypeRegistration} in
     * {@value #REGISTRATION_PACKAGE} via classpath scanning, and instantiates each with a
     * Mockito mock for every constructor dependency.
     *
     * <p>Classpath scanning (rather than a hardcoded list of registration class names) is what
     * gives this test the same "covered with zero edits" property the test it replaces had via
     * {@code SearchableEntityType.values()}: adding a new {@code @Component} implementing
     * {@link SearchTypeRegistration} is automatically picked up.
     *
     * @return a fresh, mutable list of real registration instances backed entirely by mocks
     */
    private static List<SearchTypeRegistration> instantiateProductionRegistrations() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(SearchTypeRegistration.class));
        MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

        List<SearchTypeRegistration> registrations = new ArrayList<>();
        for (var candidate : scanner.findCandidateComponents(REGISTRATION_PACKAGE)) {
            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(candidate.getBeanClassName());
            Class<?> candidateClass = Class.forName(metadataReader.getClassMetadata().getClassName());
            if (candidateClass.isInterface() || !SearchTypeRegistration.class.isAssignableFrom(candidateClass)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends SearchTypeRegistration> registrationClass =
                    (Class<? extends SearchTypeRegistration>) candidateClass;
            registrations.add(instantiateWithMocks(registrationClass));
        }

        assertThat(registrations)
                .as("Classpath scan of " + REGISTRATION_PACKAGE + " found no SearchTypeRegistration "
                        + "beans at all -- the scan itself is broken, not just a single registration")
                .isNotEmpty();

        return registrations;
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
     * <p>This is what makes a "wired to a working service" registration distinguishable from one
     * that returns a null literal without calling its dependency at all: the former returns this
     * non-null value THROUGH the mock.
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

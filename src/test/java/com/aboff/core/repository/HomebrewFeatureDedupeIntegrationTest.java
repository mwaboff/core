package com.aboff.core.repository;

import com.aboff.core.model.dto.dh.request.FeatureInput;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import com.aboff.core.service.dh.FeatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for find-or-create on features that carry no sourcebook.
 * <p>
 * Users authoring custom items create features inline, and those features belong to no
 * expansion. The dedupe query previously compared expansions with a bare
 * {@code f.expansion.id = :expansionId}, which evaluates to UNKNOWN rather than TRUE when the
 * parameter is null. Every homebrew lookup therefore missed, so each save minted a fresh row
 * and orphaned the previous one — unbounded growth reachable by any logged-in user, since the
 * backend has no rate limiting.
 * <p>
 * These tests exercise the real JPQL rather than a mock, because the defect lived entirely in
 * the query's null semantics: {@code FeatureServiceTest} stubs the repository and passes either
 * way.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class HomebrewFeatureDedupeIntegrationTest {

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private ExpansionRepository expansionRepository;

    private Expansion expansion;

    @BeforeEach
    void setUp() {
        expansion = expansionRepository.save(
                Expansion.builder().name("Dedupe Test Book").isPublished(true).build());
    }

    private FeatureInput homebrewInput(String name, String description) {
        return FeatureInput.builder()
                .name(name)
                .description(description)
                .featureType(FeatureType.ITEM)
                .build();
    }

    @Test
    void findOrCreateReusesExistingHomebrewFeatureInsteadOfMintingDuplicates() {
        Feature first = featureService.findOrCreate(homebrewInput("Serrated", "Deals extra damage."));
        Feature second = featureService.findOrCreate(homebrewInput("Serrated", "Deals extra damage."));

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    void createdHomebrewFeatureHasNoExpansion() {
        Feature created = featureService.findOrCreate(homebrewInput("Weightless", "Ignores burden."));

        assertThat(created.getExpansion()).isNull();
    }

    @Test
    void repeatedSavesDoNotGrowTheFeatureTable() {
        long before = featureRepository.count();

        for (int i = 0; i < 5; i++) {
            featureService.findOrCreate(homebrewInput("Balanced", "Reroll a 1 on damage."));
        }

        assertThat(featureRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void homebrewLookupDoesNotMatchAnOfficialFeatureWithTheSameNameAndText() {
        Feature official = featureService.findOrCreate(FeatureInput.builder()
                .name("Brutal")
                .description("Roll an additional damage die.")
                .featureType(FeatureType.ITEM)
                .expansionId(expansion.getId())
                .build());

        Feature homebrew = featureService.findOrCreate(
                homebrewInput("Brutal", "Roll an additional damage die."));

        assertThat(homebrew.getId()).isNotEqualTo(official.getId());
        assertThat(homebrew.getExpansion()).isNull();
        assertThat(official.getExpansion()).isNotNull();
    }

    @Test
    void officialLookupDoesNotMatchAHomebrewFeatureWithTheSameNameAndText() {
        Feature homebrew = featureService.findOrCreate(
                homebrewInput("Sturdy", "Reduce incoming damage by 1."));

        Feature official = featureService.findOrCreate(FeatureInput.builder()
                .name("Sturdy")
                .description("Reduce incoming damage by 1.")
                .featureType(FeatureType.ITEM)
                .expansionId(expansion.getId())
                .build());

        assertThat(official.getId()).isNotEqualTo(homebrew.getId());
    }

    @Test
    void homebrewFeaturesWithDifferentDescriptionsRemainDistinct() {
        Feature quick = featureService.findOrCreate(homebrewInput("Quick", "Mark a Stress to strike twice."));
        Feature quickAlt = featureService.findOrCreate(homebrewInput("Quick", "Mark a Stress to move first."));

        assertThat(quickAlt.getId()).isNotEqualTo(quick.getId());
    }

    @Test
    void nullExpansionParameterMatchesOnlyExpansionlessRows() {
        featureService.findOrCreate(FeatureInput.builder()
                .name("Gilded")
                .description("Worth double when sold.")
                .featureType(FeatureType.ITEM)
                .expansionId(expansion.getId())
                .build());

        Optional<Feature> match = featureRepository
                .findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                        "Gilded", null, FeatureType.ITEM, "Worth double when sold.");

        assertThat(match).isEmpty();
    }

    @Test
    void homebrewDedupeIsCaseInsensitiveOnName() {
        Feature lower = featureService.findOrCreate(homebrewInput("hooked", "Pull the target closer."));
        Feature upper = featureService.findOrCreate(homebrewInput("HOOKED", "Pull the target closer."));

        assertThat(upper.getId()).isEqualTo(lower.getId());
    }
}

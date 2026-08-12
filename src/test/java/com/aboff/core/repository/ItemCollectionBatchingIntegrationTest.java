package com.aboff.core.repository;

import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import com.aboff.core.repository.dh.WeaponRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the per-row loading of an item's campaign tags and features.
 * <p>
 * Every item response reads both collections — {@code campaignIds} and {@code featureIds} are
 * always populated, and the {@code isEmpty()} check alone is enough to initialise a lazy
 * collection. Browsing a page of 100 weapons therefore issued 100 separate loads of each, on
 * top of the two queries for the page itself. {@code @BatchSize} on {@code BaseItem} collapses
 * them into one {@code IN} per batch.
 * </p>
 * <p>
 * This asserts on Hibernate's own JDBC statement count rather than on response content, because
 * batching has no other observable effect. Statistics are switched on for the duration of the
 * test only, so the shared Spring context is not reconfigured and stays cached.
 * </p>
 * <p>
 * The visibility query in {@code findAccessibleWithFilters} carries a {@code LEFT JOIN} onto the
 * campaign join table as a predicate, which does not populate the collection, alongside
 * {@code DISTINCT} and pagination. A fetch join or entity graph on top of that shape makes
 * Hibernate paginate in memory (HHH000104), which is why batching is the tool used here.
 * </p>
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ItemCollectionBatchingIntegrationTest {

    /** Weapons to page through. Above one batch of 25, so batching is visibly not one-per-row. */
    private static final int WEAPON_COUNT = 60;

    @Autowired
    private WeaponRepository weaponRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionFactory sessionFactory;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void enableStatistics() {
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @AfterEach
    void disableStatistics() {
        statistics.setStatisticsEnabled(false);
    }

    private long countStatementsLoadingBothCollectionsOf(List<Weapon> weapons) {
        statistics.clear();
        weapons.forEach(weapon -> {
            weapon.getCampaigns().size();
            weapon.getFeatures().size();
        });
        return statistics.getPrepareStatementCount();
    }

    private List<Weapon> freshlyLoadedPage() {
        // Entities created above are already managed with their collections in hand, which would
        // hide every load. Detach them so the page below is read from the database.
        entityManager.flush();
        entityManager.clear();
        return weaponRepository.findAll(PageRequest.of(0, WEAPON_COUNT)).getContent();
    }

    @BeforeEach
    void seed() {
        User owner = userRepository.save(User.builder()
                .username("batch-" + System.nanoTime())
                .email("batch" + System.nanoTime() + "@test.com")
                .role(Role.USER)
                .build());
        Expansion expansion = expansionRepository.save(
                Expansion.builder().name("Batching Test Book").isPublished(true).build());
        Campaign campaign = campaignRepository.save(
                Campaign.builder().name("Batching Table").creator(owner).build());
        Feature feature = featureRepository.save(Feature.builder()
                .name("Batched Feature").featureType(FeatureType.ITEM).expansion(expansion)
                .isOfficial(false).build());

        for (int i = 0; i < WEAPON_COUNT; i++) {
            weaponRepository.save(Weapon.builder()
                    .name("Batched Blade " + i).tier(1)
                    .isOfficial(false).isPublic(true).createdBy(owner)
                    .isPrimary(true).trait(Trait.AGILITY).range(Range.MELEE).burden(Burden.ONE_HANDED)
                    .damage(DamageRoll.builder().diceType(DiceType.D8).damageType(DamageType.PHYSICAL).build())
                    .campaigns(Set.of(campaign))
                    .features(Set.of(feature))
                    .build());
        }
    }

    @Test
    void readingBothCollectionsAcrossAPageDoesNotIssueOneQueryPerRow() {
        long statements = countStatementsLoadingBothCollectionsOf(freshlyLoadedPage());

        // One query per row for each of the two collections would be 120. Batched at 25 it is
        // three per collection. The bound is deliberately loose: it must fail on per-row loading
        // and pass for any sane batch size, not pin an exact number.
        assertThat(statements).isLessThan(WEAPON_COUNT / 2);
    }

    @Test
    void batchingScalesWithBatchesRatherThanRows() {
        long statements = countStatementsLoadingBothCollectionsOf(freshlyLoadedPage());

        // 60 rows at a batch size of 25 is three batches per collection, so six statements. Any
        // count at or below twelve means the loads are batched rather than per-row.
        assertThat(statements).isLessThanOrEqualTo(12);
    }
}

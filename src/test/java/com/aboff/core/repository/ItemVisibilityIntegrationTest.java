package com.aboff.core.repository;

import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.WeaponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the custom-item visibility rule against a real persistence context.
 * <p>
 * The rule lives entirely in a {@code @Query}, so service-level tests that mock the repository
 * cannot verify it — they assert against whatever page the mock was told to return. These tests
 * persist real rows and real campaign memberships and check who can actually see what.
 * </p>
 * <p>
 * The rule: an item is visible when the caller is a moderator or above, or it is official, or
 * it is public, or it has no author, or the caller authored it, or it is explicitly tagged to a
 * campaign the caller is involved in. Notably there is no derived "we share a campaign, so you
 * see my things" rule — sharing is always a deliberate act by the author.
 * </p>
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ItemVisibilityIntegrationTest {

    /** Matches nothing; stands in for an empty campaign list, which SQL rejects. */
    private static final List<Long> NO_CAMPAIGNS = List.of(-1L);

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 50);

    @Autowired
    private WeaponRepository weaponRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private UserRepository userRepository;

    private User author;
    private User stranger;
    private User campaignMate;
    private Expansion expansion;

    @BeforeEach
    void setUp() {
        author = persistUser("author", Role.USER);
        stranger = persistUser("stranger", Role.USER);
        campaignMate = persistUser("mate", Role.USER);
        expansion = expansionRepository.save(
                Expansion.builder().name("Visibility Test Book").isPublished(true).build());
    }

    private User persistUser(String name, Role role) {
        return userRepository.save(User.builder()
                .username(name + "-" + System.nanoTime())
                .email(name + System.nanoTime() + "@test.com")
                .role(role)
                .build());
    }

    private Weapon persistWeapon(String name, boolean official, boolean isPublic, User creator,
                                 Set<Campaign> campaigns) {
        return weaponRepository.save(Weapon.builder()
                .name(name)
                .tier(1)
                .expansion(official ? expansion : null)
                .isOfficial(official)
                .isPublic(isPublic)
                .createdBy(creator)
                .campaigns(campaigns == null ? Set.of() : campaigns)
                .isPrimary(true)
                .trait(Trait.AGILITY)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(DamageRoll.builder()
                        .diceType(DiceType.D8)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build());
    }

    /** Names of the weapons the given user can see, with no filters applied. */
    private List<String> visibleTo(User user, List<Long> campaignIds, boolean privileged) {
        return weaponRepository.findAccessibleWithFilters(
                        user.getId(), campaignIds, privileged,
                        null, null, null, null, null, null, null, null, null, null, FIRST_PAGE)
                .getContent().stream().map(Weapon::getName).toList();
    }

    private Campaign persistCampaign(String name, User creator, Set<User> players) {
        return campaignRepository.save(Campaign.builder()
                .name(name)
                .creator(creator)
                .players(players == null ? Set.of() : players)
                .build());
    }

    // ==================== BASELINE VISIBILITY ====================

    @Test
    void officialItemIsVisibleToEveryone() {
        persistWeapon("Official Blade", true, false, null, null);

        assertThat(visibleTo(stranger, NO_CAMPAIGNS, false)).contains("Official Blade");
    }

    @Test
    void publicCustomItemIsVisibleToEveryone() {
        persistWeapon("Published Homebrew", false, true, author, null);

        assertThat(visibleTo(stranger, NO_CAMPAIGNS, false)).contains("Published Homebrew");
    }

    @Test
    void ownPrivateItemIsVisibleToItsAuthor() {
        persistWeapon("My Secret Blade", false, false, author, null);

        assertThat(visibleTo(author, NO_CAMPAIGNS, false)).contains("My Secret Blade");
    }

    @Test
    void privateItemIsHiddenFromEveryoneElse() {
        persistWeapon("Author Only", false, false, author, null);

        assertThat(visibleTo(stranger, NO_CAMPAIGNS, false)).doesNotContain("Author Only");
    }

    @Test
    void creatorlessCustomItemIsTreatedAsSystemContentAndStaysVisible() {
        // An official row later demoted to custom keeps a null creator. Every imported row is
        // in this shape, so losing them would empty the catalogue.
        persistWeapon("Orphaned Blade", false, false, null, null);

        assertThat(visibleTo(stranger, NO_CAMPAIGNS, false)).contains("Orphaned Blade");
    }

    // ==================== CAMPAIGN SHARING ====================

    @Test
    void itemTaggedToMyCampaignIsVisibleEvenThoughItIsPrivate() {
        Campaign campaign = persistCampaign("Shared Table", author, Set.of(campaignMate));
        persistWeapon("Shared Blade", false, false, author, Set.of(campaign));

        assertThat(visibleTo(campaignMate, List.of(campaign.getId()), false)).contains("Shared Blade");
    }

    @Test
    void itemTaggedToACampaignIsHiddenFromNonMembers() {
        Campaign campaign = persistCampaign("Private Table", author, Set.of(campaignMate));
        persistWeapon("Table Blade", false, false, author, Set.of(campaign));

        assertThat(visibleTo(stranger, NO_CAMPAIGNS, false)).doesNotContain("Table Blade");
    }

    @Test
    void untaggedItemIsHiddenFromCampaignMates() {
        // Sharing is deliberate: merely sharing a campaign with the author grants nothing.
        Campaign campaign = persistCampaign("Some Table", author, Set.of(campaignMate));
        persistWeapon("Untagged Blade", false, false, author, null);

        assertThat(visibleTo(campaignMate, List.of(campaign.getId()), false))
                .doesNotContain("Untagged Blade");
    }

    @Test
    void itemTaggedToACampaignIHaveLeftIsHidden() {
        Campaign left = persistCampaign("Old Table", author, Set.of());
        persistWeapon("Old Blade", false, false, author, Set.of(left));

        assertThat(visibleTo(campaignMate, NO_CAMPAIGNS, false)).doesNotContain("Old Blade");
    }

    @Test
    void campaignTagsDoNotConsumePageSlots() {
        // Without DISTINCT the LEFT JOIN emits one row per campaign tag, and LIMIT applies to
        // those raw rows. A weapon in three campaigns would then eat the whole page and crowd
        // out everything behind it -- Hibernate's in-memory de-duplication of root entities
        // hides this on a large page, so the page must be small enough to bite.
        Campaign one = persistCampaign("Slot One", campaignMate, Set.of());
        Campaign two = persistCampaign("Slot Two", campaignMate, Set.of());
        Campaign three = persistCampaign("Slot Three", campaignMate, Set.of());
        persistWeapon("AAA Shared Blade", false, false, author, Set.of(one, two, three));
        persistWeapon("BBB Public Blade", false, true, author, null);

        List<String> firstPage = weaponRepository.findAccessibleWithFilters(
                        campaignMate.getId(), List.of(one.getId(), two.getId(), three.getId()), false,
                        null, null, null, null, null, null, null, null, null, null,
                        PageRequest.of(0, 2, org.springframework.data.domain.Sort.by("name")))
                .getContent().stream().map(Weapon::getName).toList();

        assertThat(firstPage).containsExactly("AAA Shared Blade", "BBB Public Blade");
    }

    @Test
    void totalElementsIsNotInflatedByMultipleCampaignTags() {
        // Guards the explicit countQuery. The LEFT JOIN onto campaign tags yields one row per
        // tag, so COUNT without DISTINCT reports a weapon in three campaigns as three results.
        //
        // The page size here matters: Spring Data skips the count query entirely when page 0
        // already holds every row, so a large page would make this assertion vacuous. Asking
        // for one row at a time forces the count query to actually run.
        Campaign one = persistCampaign("Count One", campaignMate, Set.of());
        Campaign two = persistCampaign("Count Two", campaignMate, Set.of());
        Campaign three = persistCampaign("Count Three", campaignMate, Set.of());
        persistWeapon("Counted Blade", false, false, author, Set.of(one, two, three));

        List<Long> campaignIds = List.of(one.getId(), two.getId(), three.getId());
        long total = weaponRepository.findAccessibleWithFilters(
                        campaignMate.getId(), campaignIds, false,
                        null, null, null, null, null, null, null, null, null, null,
                        PageRequest.of(0, 1))
                .getTotalElements();

        assertThat(total).isEqualTo(1);
    }

    // ==================== PRIVILEGE AND EDGE CASES ====================

    @Test
    void moderatorSeesEveryonesPrivateItems() {
        persistWeapon("Hidden Blade", false, false, author, null);

        assertThat(visibleTo(stranger, NO_CAMPAIGNS, true)).contains("Hidden Blade");
    }

    @Test
    void userWithNoCampaignsDoesNotFailTheQuery() {
        // An empty IN () list is a hard SQL error; the sentinel keeps the clause valid.
        persistWeapon("Any Blade", true, false, null, null);

        assertThat(visibleTo(stranger, NO_CAMPAIGNS, false)).isNotEmpty();
    }

    @Test
    void softDeletedItemsAreExcluded() {
        Weapon weapon = persistWeapon("Deleted Blade", false, true, author, null);
        weapon.softDelete();
        weaponRepository.save(weapon);

        assertThat(visibleTo(stranger, NO_CAMPAIGNS, false)).doesNotContain("Deleted Blade");
    }

    @Test
    void createdByUserIdFilterNarrowsWithinVisibilityAndDoesNotWidenIt() {
        persistWeapon("Author Private", false, false, author, null);
        persistWeapon("Author Public", false, true, author, null);

        List<String> visible = weaponRepository.findAccessibleWithFilters(
                        stranger.getId(), NO_CAMPAIGNS, false,
                        null, author.getId(), null, null, null, null, null, null, null, null, FIRST_PAGE)
                .getContent().stream().map(Weapon::getName).toList();

        assertThat(visible).contains("Author Public").doesNotContain("Author Private");
    }


    // ==================== SEARCH AND SORT ====================

    private List<String> search(User user, String name, com.aboff.core.model.enums.ItemSort sort) {
        Pageable pageable = sort == null ? FIRST_PAGE
                : PageRequest.of(0, 50, sort.toSort());
        return weaponRepository.findAccessibleWithFilters(
                        user.getId(), NO_CAMPAIGNS, false,
                        null, null, name, null, null, null, null, null, null, null, pageable)
                .getContent().stream().map(Weapon::getName).toList();
    }

    @Test
    void nameFilterMatchesASubstring() {
        persistWeapon("Runed Longsword", false, true, author, null);
        persistWeapon("Plain Dagger", false, true, author, null);

        assertThat(search(stranger, "longs", null)).contains("Runed Longsword");
    }

    @Test
    void nameFilterExcludesNonMatches() {
        persistWeapon("Runed Longsword", false, true, author, null);
        persistWeapon("Plain Dagger", false, true, author, null);

        assertThat(search(stranger, "longs", null)).doesNotContain("Plain Dagger");
    }

    @Test
    void nameFilterIsCaseInsensitive() {
        persistWeapon("Runed Longsword", false, true, author, null);

        assertThat(search(stranger, "RUNED", null)).contains("Runed Longsword");
    }

    @Test
    void nameFilterDoesNotBypassVisibility() {
        // Searching must never surface something the caller could not otherwise see.
        persistWeapon("Secret Longsword", false, false, author, null);

        assertThat(search(stranger, "Secret", null)).isEmpty();
    }

    @Test
    void nullNameFilterReturnsEverythingVisible() {
        persistWeapon("Anything", false, true, author, null);

        assertThat(search(stranger, null, null)).contains("Anything");
    }

    @Test
    void nameSortOrdersAlphabeticallyRatherThanByInsertion() {
        // The point of the NAME ordering: user-authored content gets low-id official rows ahead
        // of it under the default ID sort, so it always lands on the last page.
        persistWeapon("Zzz Custom Blade", false, true, author, null);
        persistWeapon("Aaa Custom Blade", false, true, author, null);

        List<String> customOnly = search(stranger, "Custom Blade",
                com.aboff.core.model.enums.ItemSort.NAME);

        assertThat(customOnly).containsExactly("Aaa Custom Blade", "Zzz Custom Blade");
    }

}

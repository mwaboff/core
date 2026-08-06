package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.UpdateArmorRequest;
import com.aboff.core.model.dto.dh.request.UpdateLootRequest;
import com.aboff.core.model.dto.dh.request.UpdateWeaponRequest;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Armor;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Loot;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ArmorRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.LootRepository;
import com.aboff.core.repository.dh.WeaponRepository;
import com.aboff.core.security.CustomUserDetails;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests covering the removal of every campaign tag from a custom item.
 *
 * <p>These deliberately go through a real {@code save()} rather than a mocked repository.
 * {@link ItemAccessService#resolveCampaigns} returns the collection that the item services
 * hand straight to {@code setCampaigns}, and Hibernate calls {@code clear()} on that
 * collection while merging a managed entity whose campaign set is already populated. An
 * immutable collection throws {@link UnsupportedOperationException} at that point. Every
 * existing test of {@code resolveCampaigns} stubs the repository, so the returned collection
 * never reached a merge and the defect went unnoticed until manual QA hit a 500.
 *
 * <p>The three item types share {@code resolveCampaigns}, so one test each is enough; the
 * setup differs only in the entity being built.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ItemCampaignTagClearingIntegrationTest {

    @Autowired
    private WeaponService weaponService;
    @Autowired
    private ArmorService armorService;
    @Autowired
    private LootService lootService;

    @Autowired
    private WeaponRepository weaponRepository;
    @Autowired
    private ArmorRepository armorRepository;
    @Autowired
    private LootRepository lootRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private Campaign campaign;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .username("tagowner")
                .email("tagowner@example.com")
                .role(Role.USER)
                .build());

        campaign = campaignRepository.save(Campaign.builder()
                .name("Tagged Campaign")
                .creator(owner)
                .build());

        CustomUserDetails principal = new CustomUserDetails(owner);
        authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
    }

    /**
     * Flushes the pending inserts and detaches everything, so that the service under test
     * loads the item fresh and its campaign set is a real Hibernate collection rather than
     * the plain {@link HashSet} the test built.
     */
    private void resetPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void updateWeapon_WithEmptyCampaignIds_ClearsTagsWithoutError() {
        // Arrange — a custom weapon already shared with a campaign
        Weapon weapon = weaponRepository.save(Weapon.builder()
                .name("Tagged Blade")
                .tier(1)
                .isOfficial(false)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(DamageRoll.builder()
                        .diceCount(1)
                        .diceType(DiceType.D8)
                        .modifier(0)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .createdBy(owner)
                .campaigns(new HashSet<>(Set.of(campaign)))
                .build());
        Long weaponId = weapon.getId();
        resetPersistenceContext();

        UpdateWeaponRequest request = new UpdateWeaponRequest();
        request.setCampaignIds(List.of());

        // Act & Assert — an immutable empty set here fails inside Hibernate's merge
        assertThatCode(() -> weaponService.updateWeapon(weaponId, request, authentication))
                .doesNotThrowAnyException();

        resetPersistenceContext();
        Weapon reloaded = weaponRepository.findById(weaponId).orElseThrow();
        assertThat(reloaded.getCampaigns()).isEmpty();
    }

    @Test
    void updateArmor_WithEmptyCampaignIds_ClearsTagsWithoutError() {
        // Arrange
        Armor armor = armorRepository.save(Armor.builder()
                .name("Tagged Plate")
                .tier(1)
                .isOfficial(false)
                .baseMajorThreshold(7)
                .baseSevereThreshold(15)
                .baseScore(4)
                .createdBy(owner)
                .campaigns(new HashSet<>(Set.of(campaign)))
                .build());
        Long armorId = armor.getId();
        resetPersistenceContext();

        UpdateArmorRequest request = new UpdateArmorRequest();
        request.setCampaignIds(List.of());

        // Act & Assert
        assertThatCode(() -> armorService.updateArmor(armorId, request, authentication))
                .doesNotThrowAnyException();

        resetPersistenceContext();
        Armor reloaded = armorRepository.findById(armorId).orElseThrow();
        assertThat(reloaded.getCampaigns()).isEmpty();
    }

    @Test
    void updateLoot_WithEmptyCampaignIds_ClearsTagsWithoutError() {
        // Arrange
        Loot loot = lootRepository.save(Loot.builder()
                .name("Tagged Trinket")
                .tier(1)
                .isOfficial(false)
                .isConsumable(false)
                .createdBy(owner)
                .campaigns(new HashSet<>(Set.of(campaign)))
                .build());
        Long lootId = loot.getId();
        resetPersistenceContext();

        UpdateLootRequest request = new UpdateLootRequest();
        request.setCampaignIds(List.of());

        // Act & Assert
        assertThatCode(() -> lootService.updateLoot(lootId, request, authentication))
                .doesNotThrowAnyException();

        resetPersistenceContext();
        Loot reloaded = lootRepository.findById(lootId).orElseThrow();
        assertThat(reloaded.getCampaigns()).isEmpty();
    }

    @Test
    void updateWeapon_WithoutCampaignIds_LeavesTagsUntouched() {
        // Arrange — the counterpart intent: a null list means "not mentioned", not "clear".
        // This guards the fix from being over-applied to the omitted case.
        Weapon weapon = weaponRepository.save(Weapon.builder()
                .name("Untouched Blade")
                .tier(1)
                .isOfficial(false)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(DamageRoll.builder()
                        .diceCount(1)
                        .diceType(DiceType.D8)
                        .modifier(0)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .createdBy(owner)
                .campaigns(new HashSet<>(Set.of(campaign)))
                .build());
        Long weaponId = weapon.getId();
        resetPersistenceContext();

        UpdateWeaponRequest request = new UpdateWeaponRequest();
        request.setName("Renamed Blade");

        // Act
        weaponService.updateWeapon(weaponId, request, authentication);

        // Assert
        resetPersistenceContext();
        Weapon reloaded = weaponRepository.findById(weaponId).orElseThrow();
        assertThat(reloaded.getCampaigns()).hasSize(1);
    }
}

package com.aboff.core.config;

import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.AncestryCard;
import com.aboff.core.model.entity.dh.Armor;
import com.aboff.core.model.entity.dh.Beastform;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.CommunityCard;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.Condition;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.DomainCard;
import com.aboff.core.model.entity.dh.Encounter;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Loot;
import com.aboff.core.model.entity.dh.MartialStance;
import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.model.entity.dh.SubclassCard;
import com.aboff.core.model.entity.dh.SubclassPath;
import com.aboff.core.model.entity.dh.TransformationCard;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.AdversaryType;
import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.DomainCardType;
import com.aboff.core.model.enums.EnvironmentType;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.QuestionType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.model.enums.SubclassLevel;
import com.aboff.core.model.enums.Trait;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SearchFieldMapping}.
 *
 * <p>Verifies that each entity type produces the correct {@link SearchFieldMapping.SearchIndexData}
 * with the expected text fields and filter column values.
 */
@ExtendWith(MockitoExtension.class)
class SearchFieldMappingTest {

    @InjectMocks
    private SearchFieldMapping searchFieldMapping;

    // ==================== WEAPON TESTS ====================

    @Test
    void buildSearchIndexData_Weapon_SetsEntityTypeToWeapon() {
        // Arrange
        Weapon weapon = Weapon.builder()
                .name("Longsword")
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .build();
        weapon.setId(1L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        // Assert
        assertThat(data.getEntityType()).isEqualTo("WEAPON");
    }

    @Test
    void buildSearchIndexData_Weapon_MapsNameToNameText() {
        // Arrange
        Weapon weapon = Weapon.builder()
                .name("Longsword")
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .build();
        weapon.setId(1L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        // Assert
        assertThat(data.getNameText()).isEqualTo("Longsword");
    }

    @Test
    void buildSearchIndexData_Weapon_WithFeatures_MapsFeatureText() {
        // Arrange
        Feature feature = Feature.builder()
                .name("Flame Burst")
                .description("Deal extra fire damage")
                .featureType(FeatureType.OTHER)
                .build();
        feature.setId(10L);

        Weapon weapon = Weapon.builder()
                .name("Flaming Sword")
                .tier(2)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .features(Set.of(feature))
                .build();
        weapon.setId(2L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        // Assert
        assertThat(data.getFeatureText()).contains("Flame Burst");
    }

    @Test
    void buildSearchIndexData_Weapon_MapsTraitFilterColumn() {
        // Arrange
        Weapon weapon = Weapon.builder()
                .name("Dagger")
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.FINESSE)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .build();
        weapon.setId(3L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        // Assert
        assertThat(data.getTrait()).isEqualTo("FINESSE");
    }

    @Test
    void buildSearchIndexData_Weapon_MapsRangeFilterColumn() {
        // Arrange
        Weapon weapon = Weapon.builder()
                .name("Shortbow")
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.FINESSE)
                .range(Range.FAR)
                .burden(Burden.TWO_HANDED)
                .build();
        weapon.setId(4L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        // Assert
        assertThat(data.getRange()).isEqualTo("FAR");
    }

    @Test
    void buildSearchIndexData_Weapon_MapsBurdenFilterColumn() {
        // Arrange
        Weapon weapon = Weapon.builder()
                .name("Greatsword")
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.TWO_HANDED)
                .build();
        weapon.setId(5L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        // Assert
        assertThat(data.getBurden()).isEqualTo("TWO_HANDED");
    }

    @Test
    void buildSearchIndexData_Weapon_WithDamage_MapsDamageTypeFilterColumn() {
        // Arrange
        DamageRoll damage = DamageRoll.builder()
                .diceCount(2)
                .diceType(DiceType.D10)
                .modifier(3)
                .damageType(DamageType.PHYSICAL)
                .build();
        Weapon weapon = Weapon.builder()
                .name("Sword")
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(damage)
                .build();
        weapon.setId(6L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        // Assert
        assertThat(data.getDamageType()).isEqualTo("PHYSICAL");
    }

    @Test
    void buildSearchIndexData_Weapon_WithNullDamage_DamageTypeIsNull() {
        // Arrange
        Weapon weapon = Weapon.builder()
                .name("Unarmed Strike")
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .build();
        weapon.setId(7L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        // Assert
        assertThat(data.getDamageType()).isNull();
    }

    // ==================== CAMPAIGN SHARING TESTS ====================

    /**
     * Builds a campaign with only the ID set, which is all the mapping reads.
     */
    private Campaign campaignWithId(Long id) {
        Campaign campaign = Campaign.builder().name("Campaign " + id).build();
        campaign.setId(id);
        return campaign;
    }

    @Test
    void buildSearchIndexData_Weapon_WithCampaigns_SetsSortedSharedCampaignIds() {
        // Arrange — insertion order deliberately unsorted; the mapping sorts so that
        // re-indexing an unchanged item produces an identical array.
        Weapon weapon = Weapon.builder()
                .name("Shared Blade")
                .tier(1)
                .isOfficial(false)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .campaigns(new LinkedHashSet<>(List.of(
                        campaignWithId(7L), campaignWithId(3L))))
                .build();
        weapon.setId(30L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        // Assert
        assertThat(data.getSharedCampaignIds()).containsExactly(3L, 7L);
    }

    @Test
    void buildSearchIndexData_Weapon_WithNoCampaigns_SetsEmptySharedCampaignIds() {
        // Arrange
        Weapon weapon = Weapon.builder()
                .name("Private Blade")
                .tier(1)
                .isOfficial(false)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .build();
        weapon.setId(31L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        // Assert — empty rather than null: an untagged item must overlap nobody's campaigns
        assertThat(data.getSharedCampaignIds()).isEmpty();
    }

    @Test
    void buildSearchIndexData_Armor_WithCampaigns_SetsSharedCampaignIds() {
        // Arrange
        Armor armor = Armor.builder()
                .name("Shared Plate")
                .tier(2)
                .isOfficial(false)
                .baseMajorThreshold(7)
                .baseSevereThreshold(15)
                .baseScore(4)
                .campaigns(new LinkedHashSet<>(List.of(campaignWithId(11L))))
                .build();
        armor.setId(32L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(armor, SearchableEntityType.ARMOR);

        // Assert
        assertThat(data.getSharedCampaignIds()).containsExactly(11L);
    }

    @Test
    void buildSearchIndexData_Loot_WithCampaigns_SetsSharedCampaignIds() {
        // Arrange
        Loot loot = Loot.builder()
                .name("Shared Trinket")
                .tier(1)
                .isOfficial(false)
                .campaigns(new LinkedHashSet<>(List.of(campaignWithId(5L))))
                .build();
        loot.setId(33L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(loot, SearchableEntityType.LOOT);

        // Assert
        assertThat(data.getSharedCampaignIds()).containsExactly(5L);
    }

    @Test
    void buildSearchIndexData_MartialStance_WithCampaigns_SetsSharedCampaignIds() {
        // Arrange — martial stances extend BaseItem and are indexed, so they carry tags too
        MartialStance stance = MartialStance.builder()
                .name("Shared Stance")
                .tier(1)
                .isOfficial(false)
                .campaigns(new LinkedHashSet<>(List.of(campaignWithId(9L))))
                .build();
        stance.setId(34L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(stance, SearchableEntityType.MARTIAL_STANCE);

        // Assert
        assertThat(data.getSharedCampaignIds()).containsExactly(9L);
    }

    @Test
    void buildSearchIndexData_MartialStance_SetsIsPublic() {
        // Arrange — the flag was never mapped, so is_public stayed NULL and the
        // `is_public = true` branch of the access clause never fired for stances
        MartialStance stance = MartialStance.builder()
                .name("Published Stance")
                .tier(1)
                .isOfficial(false)
                .isPublic(true)
                .build();
        stance.setId(35L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(stance, SearchableEntityType.MARTIAL_STANCE);

        // Assert
        assertThat(data.getIsPublic()).isTrue();
    }

    @Test
    void buildSearchIndexData_Domain_HasNoSharedCampaignIds() {
        // Arrange — only BaseItem subclasses carry campaign tags
        Domain domain = Domain.builder().name("Blade").description("The domain of blades").build();
        domain.setId(36L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(domain, SearchableEntityType.DOMAIN);

        // Assert
        assertThat(data.getSharedCampaignIds()).isNull();
    }

    // ==================== BEASTFORM TESTS ====================

    @Test
    void buildSearchIndexData_Beastform_MapsTierFilterColumn() {
        // Arrange — regression test: tier must not be null (it was silently missing from
        // buildForBeastform, breaking the tier search facet, since it was added after
        // Beastform's own search registration went in)
        Beastform beastform = Beastform.builder()
                .name("Agile Scout")
                .evasion(2)
                .tier(1)
                .build();
        beastform.setId(70L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(beastform, SearchableEntityType.BEASTFORM);

        // Assert
        assertThat(data.getTier()).isEqualTo(1);
    }

    @Test
    void buildSearchIndexData_Beastform_MapsNameAndDescriptionText() {
        // Arrange
        Beastform beastform = Beastform.builder()
                .name("Agile Scout")
                .example("Fox, Mouse, Weasel, etc.")
                .advantages("Gain advantage on: deceive, locate, sneak")
                .evasion(2)
                .tier(1)
                .build();
        beastform.setId(71L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(beastform, SearchableEntityType.BEASTFORM);

        // Assert
        assertThat(data.getNameText()).isEqualTo("Agile Scout");
        assertThat(data.getDescriptionText())
                .isEqualTo("Fox, Mouse, Weasel, etc. Gain advantage on: deceive, locate, sneak");
    }

    // ==================== DOMAIN TESTS ====================

    @Test
    void buildSearchIndexData_Domain_SetsEntityTypeToDomain() {
        // Arrange
        Domain domain = Domain.builder()
                .name("Blade")
                .description("The domain of swords and combat.")
                .build();
        domain.setId(10L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(domain, SearchableEntityType.DOMAIN);

        // Assert
        assertThat(data.getEntityType()).isEqualTo("DOMAIN");
    }

    @Test
    void buildSearchIndexData_Domain_MapsNameToNameText() {
        // Arrange
        Domain domain = Domain.builder()
                .name("Codex")
                .description("The domain of knowledge.")
                .build();
        domain.setId(11L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(domain, SearchableEntityType.DOMAIN);

        // Assert
        assertThat(data.getNameText()).isEqualTo("Codex");
    }

    @Test
    void buildSearchIndexData_Domain_MapsDescriptionToDescriptionText() {
        // Arrange
        Domain domain = Domain.builder()
                .name("Grace")
                .description("The domain of healing and protection.")
                .build();
        domain.setId(12L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(domain, SearchableEntityType.DOMAIN);

        // Assert
        assertThat(data.getDescriptionText()).isEqualTo("The domain of healing and protection.");
    }

    @Test
    void buildSearchIndexData_Domain_MapsIsOfficialFilterColumn() {
        // Arrange — regression test: domains had no isOfficial at all, leaving
        // search_index.is_official NULL, which made every domain vanish from any
        // search filtered to official content
        Domain domain = Domain.builder()
                .name("Dread")
                .isOfficial(true)
                .build();
        domain.setId(13L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(domain, SearchableEntityType.DOMAIN);

        // Assert
        assertThat(data.getIsOfficial()).isTrue();
    }

    @Test
    void buildSearchIndexData_Domain_MapsNonOfficialIsOfficialFilterColumn() {
        // Arrange
        Domain domain = Domain.builder()
                .name("Homebrew Domain")
                .isOfficial(false)
                .build();
        domain.setId(14L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(domain, SearchableEntityType.DOMAIN);

        // Assert
        assertThat(data.getIsOfficial()).isFalse();
    }

    // ==================== CLASS TESTS ====================

    @Test
    void buildSearchIndexData_Class_MapsIsOfficialFilterColumn() {
        // Arrange — regression test: same gap as domains, see above
        Class clazz = Class.builder()
                .name("Warlock")
                .description("A wielder of borrowed power.")
                .isOfficial(true)
                .startingEvasion(10)
                .startingHitPoints(6)
                .build();
        clazz.setId(10L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(clazz, SearchableEntityType.CLASS);

        // Assert
        assertThat(data.getIsOfficial()).isTrue();
    }

    @Test
    void buildSearchIndexData_Class_MapsNonOfficialIsOfficialFilterColumn() {
        // Arrange
        Class clazz = Class.builder()
                .name("Homebrew Class")
                .isOfficial(false)
                .startingEvasion(9)
                .startingHitPoints(5)
                .build();
        clazz.setId(11L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(clazz, SearchableEntityType.CLASS);

        // Assert
        assertThat(data.getIsOfficial()).isFalse();
    }

    // ==================== ANCESTRY CARD TESTS ====================

    @Test
    void buildSearchIndexData_AncestryCard_SetsEntityTypeToAncestryCard() {
        // Arrange
        AncestryCard card = AncestryCard.builder()
                .name("Elf")
                .description("A lithe and graceful people.")
                .isOfficial(true)
                .isMixed(false)
                .build();
        card.setId(20L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.ANCESTRY_CARD);

        // Assert
        assertThat(data.getEntityType()).isEqualTo("ANCESTRY_CARD");
    }

    @Test
    void buildSearchIndexData_AncestryCard_MapsNameToNameText() {
        // Arrange
        AncestryCard card = AncestryCard.builder()
                .name("Dwarf")
                .description("A stout and hardy folk.")
                .isOfficial(true)
                .isMixed(false)
                .build();
        card.setId(21L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.ANCESTRY_CARD);

        // Assert
        assertThat(data.getNameText()).isEqualTo("Dwarf");
    }

    @Test
    void buildSearchIndexData_AncestryCard_MapsDescriptionToDescriptionText() {
        // Arrange
        AncestryCard card = AncestryCard.builder()
                .name("Halfling")
                .description("Small but mighty.")
                .isOfficial(true)
                .isMixed(false)
                .build();
        card.setId(22L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.ANCESTRY_CARD);

        // Assert
        assertThat(data.getDescriptionText()).isEqualTo("Small but mighty.");
    }

    @Test
    void buildSearchIndexData_AncestryCard_WithFeatures_MapsFeatureText() {
        // Arrange
        Feature feature = Feature.builder()
                .name("Night Vision")
                .description("See in the dark.")
                .featureType(FeatureType.OTHER)
                .build();
        feature.setId(30L);

        AncestryCard card = AncestryCard.builder()
                .name("Elf")
                .description("Graceful.")
                .isOfficial(true)
                .isMixed(false)
                .features(Set.of(feature))
                .build();
        card.setId(23L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.ANCESTRY_CARD);

        // Assert
        assertThat(data.getFeatureText()).contains("Night Vision");
    }

    @Test
    void buildSearchIndexData_AncestryCard_MapsIsOfficialFilterColumn() {
        // Arrange
        AncestryCard card = AncestryCard.builder()
                .name("Custom Mix")
                .isOfficial(false)
                .isMixed(true)
                .build();
        card.setId(24L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.ANCESTRY_CARD);

        // Assert
        assertThat(data.getIsOfficial()).isFalse();
    }

    @Test
    void buildSearchIndexData_AncestryCard_MapsIsMixedFilterColumn() {
        // Arrange
        AncestryCard card = AncestryCard.builder()
                .name("Half-Elf")
                .isOfficial(false)
                .isMixed(true)
                .build();
        card.setId(25L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.ANCESTRY_CARD);

        // Assert
        assertThat(data.getIsMixed()).isTrue();
    }

    @Test
    void buildSearchIndexData_AncestryCard_MapsCardTypeFilterColumn() {
        // Arrange — regression test: cardType must not be null (it was never populated
        // by buildSearchIndexData, silently breaking the cardType search filter)
        AncestryCard card = AncestryCard.builder()
                .name("Elf")
                .isOfficial(true)
                .isMixed(false)
                .build();
        card.setId(26L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.ANCESTRY_CARD);

        // Assert
        assertThat(data.getCardType()).isEqualTo("ANCESTRY");
    }

    // ==================== COMMUNITY CARD TESTS ====================

    @Test
    void buildSearchIndexData_CommunityCard_SetsEntityTypeToCommunityCard() {
        // Arrange
        CommunityCard card = CommunityCard.builder()
                .name("Highborne")
                .description("Raised among wealth and privilege.")
                .isOfficial(true)
                .build();
        card.setId(40L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.COMMUNITY_CARD);

        // Assert
        assertThat(data.getEntityType()).isEqualTo("COMMUNITY_CARD");
    }

    @Test
    void buildSearchIndexData_CommunityCard_MapsCardTypeFilterColumn() {
        // Arrange — regression test: cardType must not be null
        CommunityCard card = CommunityCard.builder()
                .name("Wanderborne")
                .isOfficial(true)
                .build();
        card.setId(41L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.COMMUNITY_CARD);

        // Assert
        assertThat(data.getCardType()).isEqualTo("COMMUNITY");
    }

    @Test
    void buildSearchIndexData_CommunityCard_MapsIsOfficialFilterColumn() {
        // Arrange — regression test: isOfficial was never populated for community cards,
        // leaving search_index.is_official NULL and hiding every official community card
        // from searches that filter on isOfficial
        CommunityCard card = CommunityCard.builder()
                .name("Loreborne")
                .isOfficial(true)
                .build();
        card.setId(42L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.COMMUNITY_CARD);

        // Assert
        assertThat(data.getIsOfficial()).isTrue();
    }

    @Test
    void buildSearchIndexData_CommunityCard_MapsNonOfficialIsOfficialFilterColumn() {
        // Arrange
        CommunityCard card = CommunityCard.builder()
                .name("Homebrew Community")
                .isOfficial(false)
                .build();
        card.setId(43L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.COMMUNITY_CARD);

        // Assert
        assertThat(data.getIsOfficial()).isFalse();
    }

    // ==================== SUBCLASS CARD TESTS ====================

    @Test
    void buildSearchIndexData_SubclassCard_SetsEntityTypeToSubclassCard() {
        // Arrange
        SubclassCard card = SubclassCard.builder()
                .name("Stalwart")
                .description("A defensive fighting style.")
                .isOfficial(true)
                .level(SubclassLevel.FOUNDATION)
                .build();
        card.setId(50L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.SUBCLASS_CARD);

        // Assert
        assertThat(data.getEntityType()).isEqualTo("SUBCLASS_CARD");
    }

    @Test
    void buildSearchIndexData_SubclassCard_MapsCardTypeFilterColumn() {
        // Arrange — regression test: cardType must not be null
        SubclassCard card = SubclassCard.builder()
                .name("Vengeance")
                .isOfficial(true)
                .level(SubclassLevel.SPECIALIZATION)
                .build();
        card.setId(51L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.SUBCLASS_CARD);

        // Assert
        assertThat(data.getCardType()).isEqualTo("SUBCLASS");
    }

    // ==================== DOMAIN CARD TESTS ====================

    @Test
    void buildSearchIndexData_DomainCard_SetsEntityTypeToDomainCard() {
        // Arrange
        DomainCard card = DomainCard.builder()
                .name("Whirlwind")
                .description("A flurry of blows.")
                .isOfficial(true)
                .level(1)
                .recallCost(0)
                .type(DomainCardType.ABILITY)
                .build();
        card.setId(60L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.DOMAIN_CARD);

        // Assert
        assertThat(data.getEntityType()).isEqualTo("DOMAIN_CARD");
    }

    @Test
    void buildSearchIndexData_DomainCard_MapsCardTypeFilterColumn() {
        // Arrange — regression test: cardType must not be null
        DomainCard card = DomainCard.builder()
                .name("Rune Ward")
                .isOfficial(true)
                .level(2)
                .recallCost(1)
                .type(DomainCardType.SPELL)
                .build();
        card.setId(61L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.DOMAIN_CARD);

        // Assert
        assertThat(data.getCardType()).isEqualTo("DOMAIN");
    }

    @Test
    void buildSearchIndexData_DomainCard_MapsDomainCardTypeFilterColumnIndependentlyOfCardType() {
        // Arrange — domainCardType (the card's own SPELL/ABILITY/etc. value) and cardType
        // (the DOMAIN discriminator) are distinct filter columns and must both be populated
        DomainCard card = DomainCard.builder()
                .name("Rune Ward")
                .isOfficial(true)
                .level(2)
                .recallCost(1)
                .type(DomainCardType.SPELL)
                .build();
        card.setId(62L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.DOMAIN_CARD);

        // Assert
        assertThat(data.getDomainCardType()).isEqualTo("SPELL");
    }

    // ==================== NULL NAME GUARD TESTS ====================

    @Test
    void buildSearchIndexData_Feature_WithNullName_ReturnsNull() {
        // Arrange — Feature.name is intentionally nullable; no name means the entity cannot be indexed
        Feature feature = Feature.builder()
                .name(null)
                .description("A feature with no name yet.")
                .featureType(FeatureType.OTHER)
                .build();
        feature.setId(50L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(feature, SearchableEntityType.FEATURE);

        // Assert
        assertThat(data).isNull();
    }

    @Test
    void buildSearchIndexData_Feature_WithName_ReturnsData() {
        // Arrange
        Feature feature = Feature.builder()
                .name("Named Feature")
                .description("Has a name.")
                .featureType(FeatureType.OTHER)
                .build();
        feature.setId(51L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(feature, SearchableEntityType.FEATURE);

        // Assert
        assertThat(data).isNotNull();
        assertThat(data.getName()).isEqualTo("Named Feature");
    }

    // ==================== NULL FEATURES TEST ====================

    @Test
    void buildSearchIndexData_Weapon_WithNoFeatures_FeatureTextIsNull() {
        // Arrange
        Weapon weapon = Weapon.builder()
                .name("Basic Sword")
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .build();
        weapon.setId(99L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        // Assert
        assertThat(data.getFeatureText()).isNull();
    }

    // ==================== TRANSFORMATION_CARD TESTS ====================

    @Test
    void buildSearchIndexData_TransformationCard_SetsEntityTypeToTransformationCard() {
        // Arrange
        Expansion expansion = Expansion.builder().name("Hope & Fear").isPublished(true).build();
        expansion.setId(5L);

        TransformationCard card = TransformationCard.builder()
                .name("Feral Transformation")
                .description("Becomes a beast")
                .expansion(expansion)
                .build();
        card.setId(1L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.TRANSFORMATION_CARD);

        // Assert
        assertThat(data.getEntityType()).isEqualTo("TRANSFORMATION_CARD");
    }

    @Test
    void buildSearchIndexData_TransformationCard_MapsNameAndDescriptionText() {
        // Arrange
        Expansion expansion = Expansion.builder().name("Hope & Fear").isPublished(true).build();
        expansion.setId(5L);

        TransformationCard card = TransformationCard.builder()
                .name("Feral Transformation")
                .description("Becomes a beast")
                .expansion(expansion)
                .build();
        card.setId(1L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.TRANSFORMATION_CARD);

        // Assert
        assertThat(data.getNameText()).isEqualTo("Feral Transformation");
        assertThat(data.getDescriptionText()).isEqualTo("Becomes a beast");
    }

    @Test
    void buildSearchIndexData_TransformationCard_MapsExpansionIdFilterColumn() {
        // Arrange
        Expansion expansion = Expansion.builder().name("Hope & Fear").isPublished(true).build();
        expansion.setId(5L);

        TransformationCard card = TransformationCard.builder()
                .name("Feral Transformation")
                .expansion(expansion)
                .build();
        card.setId(1L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.TRANSFORMATION_CARD);

        // Assert
        assertThat(data.getExpansionId()).isEqualTo(5L);
    }

    @Test
    void buildSearchIndexData_TransformationCard_WithFeatures_MapsFeatureText() {
        // Arrange
        Feature feature = Feature.builder()
                .name("Bestial Fury")
                .description("Gain a bonus while transformed")
                .featureType(FeatureType.OTHER)
                .build();
        feature.setId(40L);

        Expansion expansion = Expansion.builder().name("Hope & Fear").isPublished(true).build();
        expansion.setId(5L);

        TransformationCard card = TransformationCard.builder()
                .name("Feral Transformation")
                .expansion(expansion)
                .features(Set.of(feature))
                .build();
        card.setId(1L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.TRANSFORMATION_CARD);

        // Assert
        assertThat(data.getFeatureText()).contains("Bestial Fury");
    }

    @Test
    void buildSearchIndexData_TransformationCard_WithNoFeatures_FeatureTextIsNull() {
        // Arrange
        Expansion expansion = Expansion.builder().name("Hope & Fear").isPublished(true).build();
        expansion.setId(5L);

        TransformationCard card = TransformationCard.builder()
                .name("Feral Transformation")
                .expansion(expansion)
                .build();
        card.setId(1L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.TRANSFORMATION_CARD);

        // Assert
        assertThat(data.getFeatureText()).isNull();
    }

    @Test
    void buildSearchIndexData_TransformationCard_CardTypeIsNull() {
        // Arrange — TransformationCard is not a Card subtype (no CardType applies to it),
        // so unlike the four Card-derived entity types, its cardType filter column must stay null
        Expansion expansion = Expansion.builder().name("Hope & Fear").isPublished(true).build();
        expansion.setId(5L);

        TransformationCard card = TransformationCard.builder()
                .name("Feral Transformation")
                .expansion(expansion)
                .build();
        card.setId(1L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.TRANSFORMATION_CARD);

        // Assert
        assertThat(data.getCardType()).isNull();
    }

    // ==================== SRD FILTER COLUMN TESTS ====================
    //
    // Workstream H: search_index.srd gates paid-expansion content out of search for callers
    // who may not view it. Every buildForX method that has a source `srd` field to read from
    // must map it through unchanged (true stays true, and an unset/null source stays null
    // rather than being coerced to false) so the ON CONFLICT upsert and the search predicate
    // downstream have a trustworthy value. EXPANSION is the one type with no srd concept at
    // all -- see buildForExpansion's Javadoc -- and is covered separately below.

    @Test
    void buildSearchIndexData_Domain_MapsSrdFilterColumn() {
        Domain domain = Domain.builder().name("Blade").isOfficial(true).srd(true).build();
        domain.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(domain, SearchableEntityType.DOMAIN);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_Domain_WithUnsetSrd_SrdDefaultsFalse() {
        // Arrange — Domain.srd is @Builder.Default = false (matching the NOT NULL DB column), so
        // a builder call that omits .srd(...) produces false, not null; the mapping must pass
        // that default through unchanged rather than coercing it to something else. This is
        // distinct from search_index.srd, which is genuinely nullable during a backfill window —
        // this asserts the source entity's own default, not that column's null-until-backfilled
        // state.
        Domain domain = Domain.builder().name("Blade").isOfficial(true).build();
        domain.setId(201L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(domain, SearchableEntityType.DOMAIN);

        assertThat(data.getSrd()).isFalse();
    }

    @Test
    void buildSearchIndexData_Class_MapsSrdFilterColumn() {
        Class clazz = Class.builder()
                .name("Warlock")
                .isOfficial(true)
                .startingEvasion(10)
                .startingHitPoints(6)
                .srd(true)
                .build();
        clazz.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(clazz, SearchableEntityType.CLASS);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_Feature_MapsSrdFilterColumn() {
        Feature feature = Feature.builder()
                .name("Night Vision")
                .featureType(FeatureType.OTHER)
                .srd(false)
                .build();
        feature.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(feature, SearchableEntityType.FEATURE);

        assertThat(data.getSrd()).isFalse();
    }

    @Test
    void buildSearchIndexData_AncestryCard_MapsSrdFilterColumn() {
        AncestryCard card = AncestryCard.builder()
                .name("Elf")
                .isOfficial(true)
                .isMixed(false)
                .srd(true)
                .build();
        card.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.ANCESTRY_CARD);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_CommunityCard_MapsSrdFilterColumn() {
        CommunityCard card = CommunityCard.builder()
                .name("Highborne")
                .isOfficial(true)
                .srd(true)
                .build();
        card.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.COMMUNITY_CARD);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_SubclassCard_MapsSrdFilterColumn() {
        SubclassCard card = SubclassCard.builder()
                .name("Stalwart")
                .isOfficial(true)
                .level(SubclassLevel.FOUNDATION)
                .srd(true)
                .build();
        card.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.SUBCLASS_CARD);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_DomainCard_MapsSrdFilterColumn() {
        DomainCard card = DomainCard.builder()
                .name("Whirlwind")
                .isOfficial(true)
                .level(1)
                .recallCost(0)
                .type(DomainCardType.ABILITY)
                .srd(true)
                .build();
        card.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.DOMAIN_CARD);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_Weapon_MapsSrdFilterColumn() {
        Weapon weapon = Weapon.builder()
                .name("Longsword")
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .srd(true)
                .build();
        weapon.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_Weapon_WithUnsetSrd_SrdDefaultsFalse() {
        // BaseItem.srd is @Builder.Default = false (matching the NOT NULL DB column), so a
        // builder call that omits .srd(...) produces false, not null; see the equivalent Domain
        // test above for why this isn't the same thing as search_index.srd's nullability.
        Weapon weapon = Weapon.builder()
                .name("Longsword")
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .build();
        weapon.setId(201L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        assertThat(data.getSrd()).isFalse();
    }

    @Test
    void buildSearchIndexData_Armor_MapsSrdFilterColumn() {
        Armor armor = Armor.builder()
                .name("Plate")
                .tier(2)
                .isOfficial(true)
                .baseMajorThreshold(7)
                .baseSevereThreshold(15)
                .baseScore(4)
                .srd(true)
                .build();
        armor.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(armor, SearchableEntityType.ARMOR);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_Loot_MapsSrdFilterColumn() {
        Loot loot = Loot.builder()
                .name("Trinket")
                .tier(1)
                .isOfficial(true)
                .srd(true)
                .build();
        loot.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(loot, SearchableEntityType.LOOT);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_MartialStance_MapsSrdFilterColumn() {
        MartialStance stance = MartialStance.builder()
                .name("Aggressive Stance")
                .tier(1)
                .isOfficial(true)
                .srd(false)
                .build();
        stance.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(stance, SearchableEntityType.MARTIAL_STANCE);

        assertThat(data.getSrd()).isFalse();
    }

    @Test
    void buildSearchIndexData_Adversary_MapsSrdFilterColumn() {
        Adversary adversary = Adversary.builder()
                .name("Bandit")
                .tier(1)
                .adversaryType(AdversaryType.STANDARD)
                .isOfficial(true)
                .isPublic(true)
                .srd(true)
                .build();
        adversary.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(adversary, SearchableEntityType.ADVERSARY);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_Adversary_WithUnsetSrd_SrdDefaultsFalse() {
        // Adversary.srd is @Builder.Default = false (matching the NOT NULL DB column), so a
        // builder call that omits .srd(...) produces false, not null; see the equivalent Domain
        // test above for why this isn't the same thing as search_index.srd's nullability.
        Adversary adversary = Adversary.builder()
                .name("Bandit")
                .tier(1)
                .adversaryType(AdversaryType.STANDARD)
                .isOfficial(true)
                .isPublic(true)
                .build();
        adversary.setId(201L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(adversary, SearchableEntityType.ADVERSARY);

        assertThat(data.getSrd()).isFalse();
    }

    @Test
    void buildSearchIndexData_Beastform_MapsSrdFilterColumn() {
        Beastform beastform = Beastform.builder()
                .name("Agile Scout")
                .evasion(2)
                .tier(1)
                .isOfficial(true)
                .srd(true)
                .build();
        beastform.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(beastform, SearchableEntityType.BEASTFORM);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_Encounter_MapsSrdFilterColumn() {
        Encounter encounter = Encounter.builder()
                .name("Bridge Ambush")
                .isOfficial(true)
                .isPublic(false)
                .srd(true)
                .build();
        encounter.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(encounter, SearchableEntityType.ENCOUNTER);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_SubclassPath_MapsSrdFilterColumn() {
        SubclassPath path = SubclassPath.builder()
                .name("Stalwart")
                .srd(true)
                .build();
        path.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(path, SearchableEntityType.SUBCLASS_PATH);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_Question_MapsSrdFilterColumn() {
        Question question = Question.builder()
                .questionText("What drives you to adventure?")
                .questionType(QuestionType.BACKGROUND)
                .srd(false)
                .build();
        question.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(question, SearchableEntityType.QUESTION);

        assertThat(data.getSrd()).isFalse();
    }

    @Test
    void buildSearchIndexData_CardCostTag_MapsSrdFilterColumn() {
        CardCostTag tag = CardCostTag.builder()
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .srd(true)
                .build();
        tag.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(tag, SearchableEntityType.CARD_COST_TAG);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_TransformationCard_MapsSrdFilterColumn() {
        Expansion expansion = Expansion.builder().name("Hope & Fear").isPublished(true).build();
        expansion.setId(6L);

        TransformationCard card = TransformationCard.builder()
                .name("Feral Transformation")
                .expansion(expansion)
                .srd(true)
                .build();
        card.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(card, SearchableEntityType.TRANSFORMATION_CARD);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_Environment_MapsSrdFilterColumn() {
        Environment environment = Environment.builder()
                .name("Ruined Keep")
                .tier(1)
                .environmentType(EnvironmentType.EXPLORATION)
                .isOfficial(true)
                .srd(true)
                .build();
        environment.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(environment, SearchableEntityType.ENVIRONMENT);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_Condition_MapsSrdFilterColumn() {
        Condition condition = Condition.builder()
                .name("Restrained")
                .isOfficial(true)
                .srd(true)
                .build();
        condition.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(condition, SearchableEntityType.CONDITION);

        assertThat(data.getSrd()).isTrue();
    }

    @Test
    void buildSearchIndexData_Expansion_SrdIsAlwaysNull() {
        // Arrange — regression guard: `expansions` carries no srd column at all (the book
        // itself is not gated content, only the cards within it are), so this must stay null
        // unconditionally rather than defaulting to true/false.
        Expansion expansion = Expansion.builder().name("Hope & Fear").isPublished(true).build();
        expansion.setId(200L);

        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(expansion, SearchableEntityType.EXPANSION);

        assertThat(data.getSrd()).isNull();
    }
}

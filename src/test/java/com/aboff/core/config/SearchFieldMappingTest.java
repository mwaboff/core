package com.aboff.core.config;

import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.dh.AncestryCard;
import com.aboff.core.model.entity.dh.Armor;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Loot;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.model.enums.Trait;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void buildSearchIndexData_Weapon_IsAlwaysPublic() {
        // Arrange — custom (non-official) weapons must remain globally visible in search,
        // since items have no per-item privacy concept.
        Weapon weapon = Weapon.builder()
                .name("Custom Dagger")
                .tier(1)
                .isOfficial(false)
                .isPrimary(true)
                .trait(Trait.FINESSE)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .build();
        weapon.setId(8L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(weapon, SearchableEntityType.WEAPON);

        // Assert
        assertThat(data.getIsPublic()).isTrue();
    }

    // ==================== ARMOR TESTS ====================

    @Test
    void buildSearchIndexData_Armor_IsAlwaysPublic() {
        // Arrange — custom (non-official) armor must remain globally visible in search,
        // since items have no per-item privacy concept.
        Armor armor = Armor.builder()
                .name("Custom Chainmail")
                .tier(1)
                .isOfficial(false)
                .baseMajorThreshold(6)
                .baseSevereThreshold(12)
                .baseScore(2)
                .build();
        armor.setId(1L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(armor, SearchableEntityType.ARMOR);

        // Assert
        assertThat(data.getIsPublic()).isTrue();
    }

    // ==================== LOOT TESTS ====================

    @Test
    void buildSearchIndexData_Loot_IsAlwaysPublic() {
        // Arrange — custom (non-official) loot must remain globally visible in search,
        // since items have no per-item privacy concept.
        Loot loot = Loot.builder()
                .name("Custom Potion")
                .tier(1)
                .isOfficial(false)
                .isConsumable(true)
                .build();
        loot.setId(1L);

        // Act
        SearchFieldMapping.SearchIndexData data =
                searchFieldMapping.buildSearchIndexData(loot, SearchableEntityType.LOOT);

        // Assert
        assertThat(data.getIsPublic()).isTrue();
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
}

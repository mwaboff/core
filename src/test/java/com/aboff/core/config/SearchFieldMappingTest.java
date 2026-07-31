package com.aboff.core.config;

import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.dh.AncestryCard;
import com.aboff.core.model.entity.dh.Beastform;
import com.aboff.core.model.entity.dh.CommunityCard;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.DomainCard;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.SubclassCard;
import com.aboff.core.model.entity.dh.SubclassPath;
import com.aboff.core.model.entity.dh.TransformationCard;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.DomainCardType;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.model.enums.SubclassLevel;
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
}

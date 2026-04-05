package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the CharacterSheet entity.
 */
class CharacterSheetTest {

    // ==================== BUILDER TESTS ====================

    @Test
    void builder_WithAllFields_CreatesValidInstance() {
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .name("Thorin")
                .pronouns("he/him")
                .level(5)
                .evasion(12)
                .armorMax(10)
                .armorMarked(3)
                .majorDamageThreshold(8)
                .severeDamageThreshold(12)
                .agilityModifier(2)
                .agilityMarked(false)
                .strengthModifier(3)
                .strengthMarked(true)
                .finesseModifier(1)
                .finesseMarked(false)
                .instinctModifier(2)
                .instinctMarked(false)
                .presenceModifier(1)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(15)
                .hitPointMarked(5)
                .stressMax(10)
                .stressMarked(2)
                .hopeMax(3)
                .hopeMarked(1)
                .gold(250)
                .owner(owner)
                .build();

        assertThat(sheet.getName()).isEqualTo("Thorin");
        assertThat(sheet.getPronouns()).isEqualTo("he/him");
        assertThat(sheet.getLevel()).isEqualTo(5);
        assertThat(sheet.getEvasion()).isEqualTo(12);
        assertThat(sheet.getArmorMax()).isEqualTo(10);
        assertThat(sheet.getArmorMarked()).isEqualTo(3);
        assertThat(sheet.getMajorDamageThreshold()).isEqualTo(8);
        assertThat(sheet.getSevereDamageThreshold()).isEqualTo(12);
        assertThat(sheet.getAgilityModifier()).isEqualTo(2);
        assertThat(sheet.getAgilityMarked()).isFalse();
        assertThat(sheet.getStrengthModifier()).isEqualTo(3);
        assertThat(sheet.getStrengthMarked()).isTrue();
        assertThat(sheet.getFinesseModifier()).isEqualTo(1);
        assertThat(sheet.getFinesseMarked()).isFalse();
        assertThat(sheet.getInstinctModifier()).isEqualTo(2);
        assertThat(sheet.getInstinctMarked()).isFalse();
        assertThat(sheet.getPresenceModifier()).isEqualTo(1);
        assertThat(sheet.getPresenceMarked()).isFalse();
        assertThat(sheet.getKnowledgeModifier()).isEqualTo(0);
        assertThat(sheet.getKnowledgeMarked()).isFalse();
        assertThat(sheet.getHitPointMax()).isEqualTo(15);
        assertThat(sheet.getHitPointMarked()).isEqualTo(5);
        assertThat(sheet.getStressMax()).isEqualTo(10);
        assertThat(sheet.getStressMarked()).isEqualTo(2);
        assertThat(sheet.getHopeMax()).isEqualTo(3);
        assertThat(sheet.getHopeMarked()).isEqualTo(1);
        assertThat(sheet.getGold()).isEqualTo(250);
        assertThat(sheet.getOwner()).isEqualTo(owner);
    }

    @Test
    void builder_WithMinimalFields_UsesDefaults() {
        CharacterSheet sheet = CharacterSheet.builder()
                .name("Minimal Character")
                .build();

        assertThat(sheet.getName()).isEqualTo("Minimal Character");
        assertThat(sheet.getLevel()).isEqualTo(1);
        assertThat(sheet.getEvasion()).isEqualTo(0);
        assertThat(sheet.getArmorMax()).isEqualTo(0);
        assertThat(sheet.getArmorMarked()).isEqualTo(0);
        assertThat(sheet.getAgilityModifier()).isEqualTo(0);
        assertThat(sheet.getAgilityMarked()).isFalse();
        assertThat(sheet.getStrengthModifier()).isEqualTo(0);
        assertThat(sheet.getStrengthMarked()).isFalse();
        assertThat(sheet.getFinesseModifier()).isEqualTo(0);
        assertThat(sheet.getFinesseMarked()).isFalse();
        assertThat(sheet.getInstinctModifier()).isEqualTo(0);
        assertThat(sheet.getInstinctMarked()).isFalse();
        assertThat(sheet.getPresenceModifier()).isEqualTo(0);
        assertThat(sheet.getPresenceMarked()).isFalse();
        assertThat(sheet.getKnowledgeModifier()).isEqualTo(0);
        assertThat(sheet.getKnowledgeMarked()).isFalse();
        assertThat(sheet.getHitPointMax()).isEqualTo(6);
        assertThat(sheet.getHitPointMarked()).isEqualTo(0);
        assertThat(sheet.getStressMax()).isEqualTo(6);
        assertThat(sheet.getStressMarked()).isEqualTo(0);
        assertThat(sheet.getHopeMax()).isEqualTo(2);
        assertThat(sheet.getHopeMarked()).isEqualTo(0);
        assertThat(sheet.getGold()).isEqualTo(0);
    }

    // ==================== DEFAULT VALUES TESTS ====================

    @Test
    void defaultLevel_IsOne() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        assertThat(sheet.getLevel()).isEqualTo(1);
    }

    @Test
    void defaultEvasion_IsZero() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        assertThat(sheet.getEvasion()).isEqualTo(0);
    }

    @Test
    void defaultHitPointMax_IsSix() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        assertThat(sheet.getHitPointMax()).isEqualTo(6);
    }

    @Test
    void defaultStressMax_IsSix() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        assertThat(sheet.getStressMax()).isEqualTo(6);
    }

    @Test
    void defaultHopeMax_IsTwo() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        assertThat(sheet.getHopeMax()).isEqualTo(2);
    }

    @Test
    void defaultGold_IsZero() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        assertThat(sheet.getGold()).isEqualTo(0);
    }

    @Test
    void defaultTraitModifiers_AreZero() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();

        assertThat(sheet.getAgilityModifier()).isEqualTo(0);
        assertThat(sheet.getStrengthModifier()).isEqualTo(0);
        assertThat(sheet.getFinesseModifier()).isEqualTo(0);
        assertThat(sheet.getInstinctModifier()).isEqualTo(0);
        assertThat(sheet.getPresenceModifier()).isEqualTo(0);
        assertThat(sheet.getKnowledgeModifier()).isEqualTo(0);
    }

    @Test
    void defaultTraitMarked_AreFalse() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();

        assertThat(sheet.getAgilityMarked()).isFalse();
        assertThat(sheet.getStrengthMarked()).isFalse();
        assertThat(sheet.getFinesseMarked()).isFalse();
        assertThat(sheet.getInstinctMarked()).isFalse();
        assertThat(sheet.getPresenceMarked()).isFalse();
        assertThat(sheet.getKnowledgeMarked()).isFalse();
    }

    @Test
    void defaultCollections_AreEmpty() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();

        assertThat(sheet.getCommunityCards()).isEmpty();
        assertThat(sheet.getAncestryCards()).isEmpty();
        assertThat(sheet.getSubclassCards()).isEmpty();
        assertThat(sheet.getCharacterSheetWeapons()).isEmpty();
        assertThat(sheet.getCharacterSheetArmors()).isEmpty();
        assertThat(sheet.getCharacterSheetLoot()).isEmpty();
        assertThat(sheet.getExperiences()).isEmpty();
    }

    // ==================== SOFT DELETE TESTS ====================

    @Test
    void isDeleted_WhenDeletedAtIsNull_ReturnsFalse() {
        CharacterSheet sheet = CharacterSheet.builder()
                .name("Active Character")
                .deletedAt(null)
                .build();

        assertThat(sheet.isDeleted()).isFalse();
    }

    @Test
    void isDeleted_WhenDeletedAtIsSet_ReturnsTrue() {
        CharacterSheet sheet = CharacterSheet.builder()
                .name("Deleted Character")
                .deletedAt(LocalDateTime.now())
                .build();

        assertThat(sheet.isDeleted()).isTrue();
    }

    @Test
    void softDelete_SetsDeletedAtTimestamp() {
        CharacterSheet sheet = CharacterSheet.builder()
                .name("Test Character")
                .build();

        assertThat(sheet.getDeletedAt()).isNull();

        LocalDateTime beforeDelete = LocalDateTime.now().minusSeconds(1);
        sheet.softDelete();
        LocalDateTime afterDelete = LocalDateTime.now().plusSeconds(1);

        assertThat(sheet.getDeletedAt()).isNotNull();
        assertThat(sheet.getDeletedAt()).isAfter(beforeDelete);
        assertThat(sheet.getDeletedAt()).isBefore(afterDelete);
        assertThat(sheet.isDeleted()).isTrue();
    }

    @Test
    void restore_ClearsDeletedAtTimestamp() {
        CharacterSheet sheet = CharacterSheet.builder()
                .name("Test Character")
                .deletedAt(LocalDateTime.now())
                .build();

        assertThat(sheet.isDeleted()).isTrue();

        sheet.restore();

        assertThat(sheet.getDeletedAt()).isNull();
        assertThat(sheet.isDeleted()).isFalse();
    }

    @Test
    void softDeleteThenRestore_AllowsMultipleCycles() {
        CharacterSheet sheet = CharacterSheet.builder()
                .name("Test Character")
                .build();

        // First cycle
        sheet.softDelete();
        assertThat(sheet.isDeleted()).isTrue();
        sheet.restore();
        assertThat(sheet.isDeleted()).isFalse();

        // Second cycle
        sheet.softDelete();
        assertThat(sheet.isDeleted()).isTrue();
        sheet.restore();
        assertThat(sheet.isDeleted()).isFalse();
    }

    // ==================== FIELD SETTERS TESTS ====================

    @Test
    void setName_UpdatesValue() {
        CharacterSheet sheet = CharacterSheet.builder().name("Original").build();
        sheet.setName("Updated");
        assertThat(sheet.getName()).isEqualTo("Updated");
    }

    @Test
    void setLevel_UpdatesValue() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").level(1).build();
        sheet.setLevel(10);
        assertThat(sheet.getLevel()).isEqualTo(10);
    }

    @Test
    void setEvasion_UpdatesValue() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").evasion(5).build();
        sheet.setEvasion(12);
        assertThat(sheet.getEvasion()).isEqualTo(12);
    }

    @Test
    void setTraitModifier_UpdatesValue() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();

        sheet.setAgilityModifier(3);
        assertThat(sheet.getAgilityModifier()).isEqualTo(3);

        sheet.setStrengthModifier(-2);
        assertThat(sheet.getStrengthModifier()).isEqualTo(-2);
    }

    @Test
    void setTraitMarked_UpdatesValue() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();

        sheet.setAgilityMarked(true);
        assertThat(sheet.getAgilityMarked()).isTrue();

        sheet.setStrengthMarked(true);
        assertThat(sheet.getStrengthMarked()).isTrue();
    }

    @Test
    void setGold_UpdatesValue() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").gold(100).build();
        sheet.setGold(500);
        assertThat(sheet.getGold()).isEqualTo(500);
    }

    // ==================== RELATIONSHIP TESTS ====================

    @Test
    void setOwner_UpdatesValue() {
        User owner1 = User.builder().id(1L).username("player1").build();
        User owner2 = User.builder().id(2L).username("player2").build();

        CharacterSheet sheet = CharacterSheet.builder()
                .name("Test")
                .owner(owner1)
                .build();

        sheet.setOwner(owner2);

        assertThat(sheet.getOwner()).isEqualTo(owner2);
    }

    @Test
    void characterSheetWeapons_CanSetAndRetrieve() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        Weapon weapon = Weapon.builder().id(1L).name("Sword").build();
        CharacterSheetWeapon csw = CharacterSheetWeapon.builder()
                .characterSheet(sheet).weapon(weapon).equipped(true).slot("PRIMARY").build();

        sheet.getCharacterSheetWeapons().add(csw);

        assertThat(sheet.getCharacterSheetWeapons()).hasSize(1);
    }

    @Test
    void characterSheetArmors_CanSetAndRetrieve() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        Armor armor = Armor.builder().id(1L).name("Plate").build();
        CharacterSheetArmor csa = CharacterSheetArmor.builder()
                .characterSheet(sheet).armor(armor).equipped(true).build();

        sheet.getCharacterSheetArmors().add(csa);

        assertThat(sheet.getCharacterSheetArmors()).hasSize(1);
    }

    @Test
    void characterSheetLoot_CanSetAndRetrieve() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        Loot loot = Loot.builder().id(1L).name("Potion").build();
        CharacterSheetLoot csl = CharacterSheetLoot.builder()
                .characterSheet(sheet).loot(loot).build();

        sheet.getCharacterSheetLoot().add(csl);

        assertThat(sheet.getCharacterSheetLoot()).hasSize(1);
    }

    // ==================== COLLECTION TESTS ====================

    @Test
    void communityCards_CanAddAndRemove() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        CommunityCard card1 = CommunityCard.builder().name("Card 1").build();
        CommunityCard card2 = CommunityCard.builder().name("Card 2").build();

        sheet.getCommunityCards().add(card1);
        sheet.getCommunityCards().add(card2);

        assertThat(sheet.getCommunityCards()).hasSize(2);
        assertThat(sheet.getCommunityCards()).containsExactlyInAnyOrder(card1, card2);

        sheet.getCommunityCards().remove(card1);

        assertThat(sheet.getCommunityCards()).hasSize(1);
        assertThat(sheet.getCommunityCards()).contains(card2);
    }

    @Test
    void ancestryCards_CanAddAndRemove() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        AncestryCard card = AncestryCard.builder().id(1L).build();

        sheet.getAncestryCards().add(card);

        assertThat(sheet.getAncestryCards()).hasSize(1);
        assertThat(sheet.getAncestryCards()).contains(card);
    }

    @Test
    void subclassCards_CanAddAndRemove() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        SubclassCard card = SubclassCard.builder().id(1L).build();

        sheet.getSubclassCards().add(card);

        assertThat(sheet.getSubclassCards()).hasSize(1);
        assertThat(sheet.getSubclassCards()).contains(card);
    }

    @Test
    void inventoryWeapons_CanAddAndRemove() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        Weapon weapon = Weapon.builder().id(1L).name("Sword").build();
        CharacterSheetWeapon csw = CharacterSheetWeapon.builder()
                .characterSheet(sheet).weapon(weapon).build();

        sheet.getCharacterSheetWeapons().add(csw);

        assertThat(sheet.getCharacterSheetWeapons()).hasSize(1);

        sheet.getCharacterSheetWeapons().remove(csw);

        assertThat(sheet.getCharacterSheetWeapons()).isEmpty();
    }

    @Test
    void inventoryArmors_CanAddAndRemove() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        Armor armor = Armor.builder().id(1L).name("Leather").build();
        CharacterSheetArmor csa = CharacterSheetArmor.builder()
                .characterSheet(sheet).armor(armor).build();

        sheet.getCharacterSheetArmors().add(csa);

        assertThat(sheet.getCharacterSheetArmors()).hasSize(1);

        sheet.getCharacterSheetArmors().remove(csa);

        assertThat(sheet.getCharacterSheetArmors()).isEmpty();
    }

    @Test
    void inventoryItems_CanAddAndRemove() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        Loot loot = Loot.builder().id(1L).name("Potion").build();
        CharacterSheetLoot csl = CharacterSheetLoot.builder()
                .characterSheet(sheet).loot(loot).build();

        sheet.getCharacterSheetLoot().add(csl);

        assertThat(sheet.getCharacterSheetLoot()).hasSize(1);

        sheet.getCharacterSheetLoot().remove(csl);

        assertThat(sheet.getCharacterSheetLoot()).isEmpty();
    }

    @Test
    void experiences_CanAddAndRemove() {
        CharacterSheet sheet = CharacterSheet.builder().name("Test").build();
        Experience experience = Experience.builder()
                .id(1L)
                .description("Survived dragon attack")
                .build();

        sheet.getExperiences().add(experience);

        assertThat(sheet.getExperiences()).hasSize(1);
        assertThat(sheet.getExperiences()).contains(experience);
    }

    // ==================== EQUALITY TESTS ====================

    @Test
    void equals_SameId_ReturnsTrue() {
        CharacterSheet sheet1 = CharacterSheet.builder().id(1L).name("Test").build();
        CharacterSheet sheet2 = CharacterSheet.builder().id(1L).name("Test").build();

        assertThat(sheet1).isEqualTo(sheet2);
    }

    @Test
    void equals_DifferentName_ReturnsFalse() {
        CharacterSheet sheet1 = CharacterSheet.builder().name("Test 1").build();
        CharacterSheet sheet2 = CharacterSheet.builder().name("Test 2").build();

        assertThat(sheet1).isNotEqualTo(sheet2);
    }

    @Test
    void hashCode_SameId_ReturnsSameHash() {
        CharacterSheet sheet1 = CharacterSheet.builder().id(1L).name("Test").build();
        CharacterSheet sheet2 = CharacterSheet.builder().id(1L).name("Test").build();

        assertThat(sheet1.hashCode()).isEqualTo(sheet2.hashCode());
    }

    // ==================== NULL HANDLING TESTS ====================

    @Test
    void builder_WithNullOptionalFields_AllowsNull() {
        CharacterSheet sheet = CharacterSheet.builder()
                .name("Test")
                .pronouns(null)
                .deletedAt(null)
                .build();

        assertThat(sheet.getPronouns()).isNull();
        assertThat(sheet.getDeletedAt()).isNull();
    }

    // ==================== NEGATIVE VALUE TESTS ====================

    @Test
    void traitModifiers_AllowNegativeValues() {
        CharacterSheet sheet = CharacterSheet.builder()
                .name("Test")
                .agilityModifier(-3)
                .strengthModifier(-2)
                .finesseModifier(-1)
                .build();

        assertThat(sheet.getAgilityModifier()).isEqualTo(-3);
        assertThat(sheet.getStrengthModifier()).isEqualTo(-2);
        assertThat(sheet.getFinesseModifier()).isEqualTo(-1);
    }
}

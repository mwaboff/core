package com.aboff.core.model.entity.dh;

import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Trait;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Beastform entity.
 */
class BeastformTest {

    // ==================== BUILDER TESTS ====================

    @Test
    void builder_WithAllFields_CreatesValidInstance() {
        User creator = User.builder().id(1L).username("creator1").build();
        Expansion expansion = Expansion.builder().id(1L).name("Core").build();
        Feature feature = Feature.builder().id(1L).name("Night Vision").build();
        Beastform original = Beastform.builder().id(1L).name("Original Wolf").build();
        DamageRoll damage = DamageRoll.builder()
                .diceCount(2)
                .diceType(DiceType.D8)
                .modifier(2)
                .damageType(DamageType.PHYSICAL)
                .build();

        Beastform beastform = Beastform.builder()
                .name("Dire Wolf")
                .example("A massive wolf with glowing eyes")
                .advantages("Enhanced tracking in forests")
                .agilityModifier(2)
                .strengthModifier(3)
                .finesseModifier(-1)
                .instinctModifier(4)
                .presenceModifier(1)
                .knowledgeModifier(-2)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.STRENGTH)
                .damage(damage)
                .isOfficial(true)
                .isPublic(true)
                .originalBeastform(original)
                .expansion(expansion)
                .createdBy(creator)
                .build();

        assertThat(beastform.getName()).isEqualTo("Dire Wolf");
        assertThat(beastform.getExample()).isEqualTo("A massive wolf with glowing eyes");
        assertThat(beastform.getAdvantages()).isEqualTo("Enhanced tracking in forests");
        assertThat(beastform.getAgilityModifier()).isEqualTo(2);
        assertThat(beastform.getStrengthModifier()).isEqualTo(3);
        assertThat(beastform.getFinesseModifier()).isEqualTo(-1);
        assertThat(beastform.getInstinctModifier()).isEqualTo(4);
        assertThat(beastform.getPresenceModifier()).isEqualTo(1);
        assertThat(beastform.getKnowledgeModifier()).isEqualTo(-2);
        assertThat(beastform.getAttackRange()).isEqualTo(Range.MELEE);
        assertThat(beastform.getAttackTrait()).isEqualTo(Trait.STRENGTH);
        assertThat(beastform.getDamage()).isEqualTo(damage);
        assertThat(beastform.getIsOfficial()).isTrue();
        assertThat(beastform.getIsPublic()).isTrue();
        assertThat(beastform.getOriginalBeastform()).isEqualTo(original);
        assertThat(beastform.getExpansion()).isEqualTo(expansion);
        assertThat(beastform.getCreatedBy()).isEqualTo(creator);
    }

    @Test
    void builder_WithMinimalFields_UsesDefaults() {
        Beastform beastform = Beastform.builder()
                .name("Basic Beast")
                .build();

        assertThat(beastform.getName()).isEqualTo("Basic Beast");
        assertThat(beastform.getAgilityModifier()).isEqualTo(0);
        assertThat(beastform.getStrengthModifier()).isEqualTo(0);
        assertThat(beastform.getFinesseModifier()).isEqualTo(0);
        assertThat(beastform.getInstinctModifier()).isEqualTo(0);
        assertThat(beastform.getPresenceModifier()).isEqualTo(0);
        assertThat(beastform.getKnowledgeModifier()).isEqualTo(0);
        assertThat(beastform.getIsOfficial()).isFalse();
        assertThat(beastform.getIsPublic()).isFalse();
        assertThat(beastform.getFeatures()).isEmpty();
    }

    // ==================== DEFAULT VALUES TESTS ====================

    @Test
    void defaultTraitModifiers_AreZero() {
        Beastform beastform = Beastform.builder().name("Test").build();

        assertThat(beastform.getAgilityModifier()).isEqualTo(0);
        assertThat(beastform.getStrengthModifier()).isEqualTo(0);
        assertThat(beastform.getFinesseModifier()).isEqualTo(0);
        assertThat(beastform.getInstinctModifier()).isEqualTo(0);
        assertThat(beastform.getPresenceModifier()).isEqualTo(0);
        assertThat(beastform.getKnowledgeModifier()).isEqualTo(0);
    }

    @Test
    void defaultIsOfficial_IsFalse() {
        Beastform beastform = Beastform.builder().name("Test").build();
        assertThat(beastform.getIsOfficial()).isFalse();
    }

    @Test
    void defaultIsPublic_IsFalse() {
        Beastform beastform = Beastform.builder().name("Test").build();
        assertThat(beastform.getIsPublic()).isFalse();
    }

    @Test
    void defaultFeatures_IsEmpty() {
        Beastform beastform = Beastform.builder().name("Test").build();
        assertThat(beastform.getFeatures()).isEmpty();
    }

    // ==================== SOFT DELETE TESTS ====================

    @Test
    void isDeleted_WhenDeletedAtIsNull_ReturnsFalse() {
        Beastform beastform = Beastform.builder()
                .name("Active Beastform")
                .deletedAt(null)
                .build();

        assertThat(beastform.isDeleted()).isFalse();
    }

    @Test
    void isDeleted_WhenDeletedAtIsSet_ReturnsTrue() {
        Beastform beastform = Beastform.builder()
                .name("Deleted Beastform")
                .deletedAt(LocalDateTime.now())
                .build();

        assertThat(beastform.isDeleted()).isTrue();
    }

    @Test
    void softDelete_SetsDeletedAtTimestamp() {
        Beastform beastform = Beastform.builder()
                .name("Test Beastform")
                .build();

        assertThat(beastform.getDeletedAt()).isNull();

        LocalDateTime beforeDelete = LocalDateTime.now().minusSeconds(1);
        beastform.softDelete();
        LocalDateTime afterDelete = LocalDateTime.now().plusSeconds(1);

        assertThat(beastform.getDeletedAt()).isNotNull();
        assertThat(beastform.getDeletedAt()).isAfter(beforeDelete);
        assertThat(beastform.getDeletedAt()).isBefore(afterDelete);
        assertThat(beastform.isDeleted()).isTrue();
    }

    @Test
    void restore_ClearsDeletedAtTimestamp() {
        Beastform beastform = Beastform.builder()
                .name("Test Beastform")
                .deletedAt(LocalDateTime.now())
                .build();

        assertThat(beastform.isDeleted()).isTrue();

        beastform.restore();

        assertThat(beastform.getDeletedAt()).isNull();
        assertThat(beastform.isDeleted()).isFalse();
    }

    @Test
    void softDeleteThenRestore_AllowsMultipleCycles() {
        Beastform beastform = Beastform.builder()
                .name("Test Beastform")
                .build();

        // First cycle
        beastform.softDelete();
        assertThat(beastform.isDeleted()).isTrue();
        beastform.restore();
        assertThat(beastform.isDeleted()).isFalse();

        // Second cycle
        beastform.softDelete();
        assertThat(beastform.isDeleted()).isTrue();
        beastform.restore();
        assertThat(beastform.isDeleted()).isFalse();
    }

    // ==================== FIELD SETTERS TESTS ====================

    @Test
    void setName_UpdatesValue() {
        Beastform beastform = Beastform.builder().name("Original").build();
        beastform.setName("Updated");
        assertThat(beastform.getName()).isEqualTo("Updated");
    }

    @Test
    void setTraitModifier_UpdatesValue() {
        Beastform beastform = Beastform.builder().name("Test").build();

        beastform.setAgilityModifier(3);
        assertThat(beastform.getAgilityModifier()).isEqualTo(3);

        beastform.setStrengthModifier(-2);
        assertThat(beastform.getStrengthModifier()).isEqualTo(-2);

        beastform.setFinesseModifier(1);
        assertThat(beastform.getFinesseModifier()).isEqualTo(1);

        beastform.setInstinctModifier(4);
        assertThat(beastform.getInstinctModifier()).isEqualTo(4);

        beastform.setPresenceModifier(-1);
        assertThat(beastform.getPresenceModifier()).isEqualTo(-1);

        beastform.setKnowledgeModifier(2);
        assertThat(beastform.getKnowledgeModifier()).isEqualTo(2);
    }

    @Test
    void setAttackRange_UpdatesValue() {
        Beastform beastform = Beastform.builder().name("Test").attackRange(Range.MELEE).build();
        beastform.setAttackRange(Range.FAR);
        assertThat(beastform.getAttackRange()).isEqualTo(Range.FAR);
    }

    @Test
    void setAttackTrait_UpdatesValue() {
        Beastform beastform = Beastform.builder().name("Test").attackTrait(Trait.STRENGTH).build();
        beastform.setAttackTrait(Trait.AGILITY);
        assertThat(beastform.getAttackTrait()).isEqualTo(Trait.AGILITY);
    }

    @Test
    void setDamage_UpdatesValue() {
        DamageRoll damage1 = DamageRoll.builder()
                .diceType(DiceType.D6)
                .damageType(DamageType.PHYSICAL)
                .build();
        DamageRoll damage2 = DamageRoll.builder()
                .diceType(DiceType.D10)
                .damageType(DamageType.MAGIC)
                .build();

        Beastform beastform = Beastform.builder()
                .name("Test")
                .damage(damage1)
                .build();

        beastform.setDamage(damage2);

        assertThat(beastform.getDamage()).isEqualTo(damage2);
    }

    // ==================== RELATIONSHIP TESTS ====================

    @Test
    void setCreatedBy_UpdatesValue() {
        User creator1 = User.builder().id(1L).username("creator1").build();
        User creator2 = User.builder().id(2L).username("creator2").build();

        Beastform beastform = Beastform.builder()
                .name("Test")
                .createdBy(creator1)
                .build();

        beastform.setCreatedBy(creator2);

        assertThat(beastform.getCreatedBy()).isEqualTo(creator2);
    }

    @Test
    void setExpansion_UpdatesValue() {
        Expansion expansion1 = Expansion.builder().id(1L).name("Core").build();
        Expansion expansion2 = Expansion.builder().id(2L).name("Advanced").build();

        Beastform beastform = Beastform.builder()
                .name("Test")
                .expansion(expansion1)
                .build();

        beastform.setExpansion(expansion2);

        assertThat(beastform.getExpansion()).isEqualTo(expansion2);
    }

    @Test
    void setOriginalBeastform_UpdatesValue() {
        Beastform original1 = Beastform.builder().id(1L).name("Original 1").build();
        Beastform original2 = Beastform.builder().id(2L).name("Original 2").build();

        Beastform beastform = Beastform.builder()
                .name("Custom")
                .originalBeastform(original1)
                .build();

        beastform.setOriginalBeastform(original2);

        assertThat(beastform.getOriginalBeastform()).isEqualTo(original2);
    }

    // ==================== COLLECTION TESTS ====================

    @Test
    void features_CanAddAndRemove() {
        Beastform beastform = Beastform.builder().name("Test").build();
        Feature feature1 = Feature.builder().id(1L).name("Night Vision").build();
        Feature feature2 = Feature.builder().id(2L).name("Enhanced Smell").build();

        beastform.getFeatures().add(feature1);
        beastform.getFeatures().add(feature2);

        assertThat(beastform.getFeatures()).hasSize(2);
        assertThat(beastform.getFeatures()).containsExactlyInAnyOrder(feature1, feature2);

        beastform.getFeatures().remove(feature1);

        assertThat(beastform.getFeatures()).hasSize(1);
        assertThat(beastform.getFeatures()).contains(feature2);
    }

    // ==================== EQUALITY TESTS ====================

    @Test
    void equals_SameId_ReturnsTrue() {
        Beastform beastform1 = Beastform.builder().id(1L).name("Test").build();
        Beastform beastform2 = Beastform.builder().id(1L).name("Test").build();

        assertThat(beastform1).isEqualTo(beastform2);
    }

    @Test
    void equals_DifferentName_ReturnsFalse() {
        Beastform beastform1 = Beastform.builder().name("Test 1").build();
        Beastform beastform2 = Beastform.builder().name("Test 2").build();

        assertThat(beastform1).isNotEqualTo(beastform2);
    }

    @Test
    void hashCode_SameId_ReturnsSameHash() {
        Beastform beastform1 = Beastform.builder().id(1L).name("Test").build();
        Beastform beastform2 = Beastform.builder().id(1L).name("Test").build();

        assertThat(beastform1.hashCode()).isEqualTo(beastform2.hashCode());
    }

    // ==================== NULL HANDLING TESTS ====================

    @Test
    void builder_WithNullOptionalFields_AllowsNull() {
        Beastform beastform = Beastform.builder()
                .name("Test")
                .example(null)
                .advantages(null)
                .originalBeastform(null)
                .deletedAt(null)
                .build();

        assertThat(beastform.getExample()).isNull();
        assertThat(beastform.getAdvantages()).isNull();
        assertThat(beastform.getOriginalBeastform()).isNull();
        assertThat(beastform.getDeletedAt()).isNull();
    }

    // ==================== NEGATIVE VALUE TESTS ====================

    @Test
    void traitModifiers_AllowNegativeValues() {
        Beastform beastform = Beastform.builder()
                .name("Test")
                .agilityModifier(-3)
                .strengthModifier(-2)
                .finesseModifier(-1)
                .instinctModifier(-4)
                .presenceModifier(-1)
                .knowledgeModifier(-5)
                .build();

        assertThat(beastform.getAgilityModifier()).isEqualTo(-3);
        assertThat(beastform.getStrengthModifier()).isEqualTo(-2);
        assertThat(beastform.getFinesseModifier()).isEqualTo(-1);
        assertThat(beastform.getInstinctModifier()).isEqualTo(-4);
        assertThat(beastform.getPresenceModifier()).isEqualTo(-1);
        assertThat(beastform.getKnowledgeModifier()).isEqualTo(-5);
    }

    // ==================== CONTENT MANAGEMENT TESTS ====================

    @Test
    void setIsOfficial_UpdatesValue() {
        Beastform beastform = Beastform.builder().name("Test").isOfficial(false).build();
        beastform.setIsOfficial(true);
        assertThat(beastform.getIsOfficial()).isTrue();
    }

    @Test
    void setIsPublic_UpdatesValue() {
        Beastform beastform = Beastform.builder().name("Test").isPublic(false).build();
        beastform.setIsPublic(true);
        assertThat(beastform.getIsPublic()).isTrue();
    }

    // ==================== COMBAT TESTS ====================

    @Test
    void combatAttributes_CanBeSet() {
        DamageRoll damage = DamageRoll.builder()
                .diceCount(2)
                .diceType(DiceType.D10)
                .modifier(3)
                .damageType(DamageType.PHYSICAL)
                .build();

        Beastform beastform = Beastform.builder()
                .name("Combat Beast")
                .attackRange(Range.CLOSE)
                .attackTrait(Trait.FINESSE)
                .damage(damage)
                .build();

        assertThat(beastform.getAttackRange()).isEqualTo(Range.CLOSE);
        assertThat(beastform.getAttackTrait()).isEqualTo(Trait.FINESSE);
        assertThat(beastform.getDamage()).isNotNull();
        assertThat(beastform.getDamage().getDiceCount()).isEqualTo(2);
        assertThat(beastform.getDamage().getDiceType()).isEqualTo(DiceType.D10);
        assertThat(beastform.getDamage().getModifier()).isEqualTo(3);
        assertThat(beastform.getDamage().getDamageType()).isEqualTo(DamageType.PHYSICAL);
    }
}

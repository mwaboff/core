package com.aboff.core.model.embeddable;

import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DamageRollTest {

    // ==================== PARSE BASIC TESTS ====================

    @Test
    void parse_ProficiencyBasedWithPositiveModifierMagic_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("d10+3 mag");

        assertThat(result.getDiceCount()).isNull();
        assertThat(result.getDiceType()).isEqualTo(DiceType.D10);
        assertThat(result.getModifier()).isEqualTo(3);
        assertThat(result.getDamageType()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void parse_ExplicitDiceCountPhysical_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("2d12 phy");

        assertThat(result.getDiceCount()).isEqualTo(2);
        assertThat(result.getDiceType()).isEqualTo(DiceType.D12);
        assertThat(result.getModifier()).isNull();
        assertThat(result.getDamageType()).isEqualTo(DamageType.PHYSICAL);
    }

    @Test
    void parse_ProficiencyBasedWithNegativeModifier_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("d6-1 phy");

        assertThat(result.getDiceCount()).isNull();
        assertThat(result.getDiceType()).isEqualTo(DiceType.D6);
        assertThat(result.getModifier()).isEqualTo(-1);
        assertThat(result.getDamageType()).isEqualTo(DamageType.PHYSICAL);
    }

    @Test
    void parse_ExplicitDiceCountWithModifier_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("3d8+5 mag");

        assertThat(result.getDiceCount()).isEqualTo(3);
        assertThat(result.getDiceType()).isEqualTo(DiceType.D8);
        assertThat(result.getModifier()).isEqualTo(5);
        assertThat(result.getDamageType()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void parse_SingleDiceExplicit_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("1d20 phy");

        assertThat(result.getDiceCount()).isEqualTo(1);
        assertThat(result.getDiceType()).isEqualTo(DiceType.D20);
        assertThat(result.getModifier()).isNull();
        assertThat(result.getDamageType()).isEqualTo(DamageType.PHYSICAL);
    }

    // ==================== PARSE ALL DICE TYPES ====================

    @Test
    void parse_D4_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("d4 phy");

        assertThat(result.getDiceType()).isEqualTo(DiceType.D4);
    }

    @Test
    void parse_D6_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("d6 mag");

        assertThat(result.getDiceType()).isEqualTo(DiceType.D6);
    }

    @Test
    void parse_D8_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("d8 phy");

        assertThat(result.getDiceType()).isEqualTo(DiceType.D8);
    }

    @Test
    void parse_D10_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("d10 mag");

        assertThat(result.getDiceType()).isEqualTo(DiceType.D10);
    }

    @Test
    void parse_D12_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("d12 phy");

        assertThat(result.getDiceType()).isEqualTo(DiceType.D12);
    }

    @Test
    void parse_D20_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("d20 mag");

        assertThat(result.getDiceType()).isEqualTo(DiceType.D20);
    }

    // ==================== PARSE CASE INSENSITIVITY ====================

    @Test
    void parse_UppercaseDiceCode_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("D10+3 MAG");

        assertThat(result.getDiceType()).isEqualTo(DiceType.D10);
        assertThat(result.getDamageType()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void parse_MixedCase_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("2D12 PHY");

        assertThat(result.getDiceCount()).isEqualTo(2);
        assertThat(result.getDiceType()).isEqualTo(DiceType.D12);
        assertThat(result.getDamageType()).isEqualTo(DamageType.PHYSICAL);
    }

    // ==================== PARSE WHITESPACE HANDLING ====================

    @Test
    void parse_LeadingWhitespace_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("  d10+3 mag");

        assertThat(result.getDiceType()).isEqualTo(DiceType.D10);
        assertThat(result.getModifier()).isEqualTo(3);
    }

    @Test
    void parse_TrailingWhitespace_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("d10+3 mag  ");

        assertThat(result.getDiceType()).isEqualTo(DiceType.D10);
        assertThat(result.getDamageType()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void parse_MultipleSpacesBetweenModifierAndDamageType_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("d10+3  mag");

        assertThat(result.getDiceType()).isEqualTo(DiceType.D10);
        assertThat(result.getModifier()).isEqualTo(3);
        assertThat(result.getDamageType()).isEqualTo(DamageType.MAGIC);
    }

    // ==================== PARSE LARGE VALUES ====================

    @Test
    void parse_LargeDiceCount_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("10d6+2 phy");

        assertThat(result.getDiceCount()).isEqualTo(10);
    }

    @Test
    void parse_LargeModifier_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("d12+99 mag");

        assertThat(result.getModifier()).isEqualTo(99);
    }

    @Test
    void parse_LargeNegativeModifier_ParsesCorrectly() {
        DamageRoll result = DamageRoll.parse("d8-50 phy");

        assertThat(result.getModifier()).isEqualTo(-50);
    }

    // ==================== PARSE ERROR CASES ====================

    @Test
    void parse_NullNotation_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageRoll.parse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Damage notation cannot be null or blank");
    }

    @Test
    void parse_EmptyString_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageRoll.parse(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Damage notation cannot be null or blank");
    }

    @Test
    void parse_BlankString_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageRoll.parse("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Damage notation cannot be null or blank");
    }

    @Test
    void parse_MissingDamageType_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageRoll.parse("d10+3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid damage notation");
    }

    @Test
    void parse_MissingDiceType_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageRoll.parse("+3 mag"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid damage notation");
    }

    @Test
    void parse_InvalidDiceType_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageRoll.parse("d7 phy"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown dice type");
    }

    @Test
    void parse_InvalidDamageType_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageRoll.parse("d10 fire"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid damage notation");
    }

    @Test
    void parse_RandomGarbage_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageRoll.parse("attack the enemy"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid damage notation");
    }

    @Test
    void parse_JustNumber_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DamageRoll.parse("10"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid damage notation");
    }

    // ==================== TO NOTATION TESTS ====================

    @Test
    void toNotation_ProficiencyBasedWithPositiveModifierMagic_FormatsCorrectly() {
        DamageRoll roll = DamageRoll.builder()
                .diceType(DiceType.D10)
                .modifier(3)
                .damageType(DamageType.MAGIC)
                .build();

        assertThat(roll.toNotation()).isEqualTo("d10+3 mag");
    }

    @Test
    void toNotation_ExplicitDiceCountNoModifier_FormatsCorrectly() {
        DamageRoll roll = DamageRoll.builder()
                .diceCount(2)
                .diceType(DiceType.D12)
                .damageType(DamageType.PHYSICAL)
                .build();

        assertThat(roll.toNotation()).isEqualTo("2d12 phy");
    }

    @Test
    void toNotation_NegativeModifier_FormatsCorrectly() {
        DamageRoll roll = DamageRoll.builder()
                .diceType(DiceType.D6)
                .modifier(-1)
                .damageType(DamageType.PHYSICAL)
                .build();

        assertThat(roll.toNotation()).isEqualTo("d6-1 phy");
    }

    @Test
    void toNotation_ZeroModifier_OmitsModifier() {
        DamageRoll roll = DamageRoll.builder()
                .diceType(DiceType.D8)
                .modifier(0)
                .damageType(DamageType.MAGIC)
                .build();

        assertThat(roll.toNotation()).isEqualTo("d8 mag");
    }

    @Test
    void toNotation_NullModifier_OmitsModifier() {
        DamageRoll roll = DamageRoll.builder()
                .diceType(DiceType.D20)
                .damageType(DamageType.PHYSICAL)
                .build();

        assertThat(roll.toNotation()).isEqualTo("d20 phy");
    }

    @Test
    void toNotation_ExplicitSingleDice_IncludesDiceCount() {
        DamageRoll roll = DamageRoll.builder()
                .diceCount(1)
                .diceType(DiceType.D4)
                .damageType(DamageType.MAGIC)
                .build();

        assertThat(roll.toNotation()).isEqualTo("1d4 mag");
    }

    // ==================== TOSTRING TESTS ====================

    @Test
    void toString_ReturnsNotation() {
        DamageRoll roll = DamageRoll.builder()
                .diceCount(2)
                .diceType(DiceType.D10)
                .modifier(5)
                .damageType(DamageType.MAGIC)
                .build();

        assertThat(roll.toString()).isEqualTo("2d10+5 mag");
    }

    // ==================== ROUND TRIP TESTS ====================

    @Test
    void roundTrip_ProficiencyBasedWithModifier_PreservesValues() {
        String original = "d10+3 mag";
        DamageRoll parsed = DamageRoll.parse(original);

        assertThat(parsed.toNotation()).isEqualTo(original);
    }

    @Test
    void roundTrip_ExplicitDiceCount_PreservesValues() {
        String original = "2d12 phy";
        DamageRoll parsed = DamageRoll.parse(original);

        assertThat(parsed.toNotation()).isEqualTo(original);
    }

    @Test
    void roundTrip_NegativeModifier_PreservesValues() {
        String original = "d6-1 phy";
        DamageRoll parsed = DamageRoll.parse(original);

        assertThat(parsed.toNotation()).isEqualTo(original);
    }

    @Test
    void roundTrip_AllComponents_PreservesValues() {
        String original = "3d8+5 mag";
        DamageRoll parsed = DamageRoll.parse(original);

        assertThat(parsed.toNotation()).isEqualTo(original);
    }

    // ==================== USES PROFICIENCY TESTS ====================

    @Test
    void usesProficiency_NullDiceCount_ReturnsTrue() {
        DamageRoll roll = DamageRoll.builder()
                .diceType(DiceType.D10)
                .damageType(DamageType.MAGIC)
                .build();

        assertThat(roll.usesProficiency()).isTrue();
    }

    @Test
    void usesProficiency_ExplicitDiceCount_ReturnsFalse() {
        DamageRoll roll = DamageRoll.builder()
                .diceCount(2)
                .diceType(DiceType.D10)
                .damageType(DamageType.MAGIC)
                .build();

        assertThat(roll.usesProficiency()).isFalse();
    }

    @Test
    void usesProficiency_ParsedProficiencyBased_ReturnsTrue() {
        DamageRoll roll = DamageRoll.parse("d10+3 mag");

        assertThat(roll.usesProficiency()).isTrue();
    }

    @Test
    void usesProficiency_ParsedExplicitCount_ReturnsFalse() {
        DamageRoll roll = DamageRoll.parse("2d10+3 mag");

        assertThat(roll.usesProficiency()).isFalse();
    }

    // ==================== BUILDER TESTS ====================

    @Test
    void builder_AllFields_CreatesValidInstance() {
        DamageRoll roll = DamageRoll.builder()
                .diceCount(2)
                .diceType(DiceType.D10)
                .modifier(3)
                .damageType(DamageType.MAGIC)
                .build();

        assertThat(roll.getDiceCount()).isEqualTo(2);
        assertThat(roll.getDiceType()).isEqualTo(DiceType.D10);
        assertThat(roll.getModifier()).isEqualTo(3);
        assertThat(roll.getDamageType()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void builder_MinimalFields_CreatesValidInstance() {
        DamageRoll roll = DamageRoll.builder()
                .diceType(DiceType.D6)
                .damageType(DamageType.PHYSICAL)
                .build();

        assertThat(roll.getDiceCount()).isNull();
        assertThat(roll.getDiceType()).isEqualTo(DiceType.D6);
        assertThat(roll.getModifier()).isNull();
        assertThat(roll.getDamageType()).isEqualTo(DamageType.PHYSICAL);
    }

    // ==================== EQUALITY TESTS ====================

    @Test
    void equals_SameValues_ReturnsTrue() {
        DamageRoll roll1 = DamageRoll.parse("2d10+3 mag");
        DamageRoll roll2 = DamageRoll.parse("2d10+3 mag");

        assertThat(roll1).isEqualTo(roll2);
    }

    @Test
    void equals_DifferentDiceCount_ReturnsFalse() {
        DamageRoll roll1 = DamageRoll.parse("2d10+3 mag");
        DamageRoll roll2 = DamageRoll.parse("3d10+3 mag");

        assertThat(roll1).isNotEqualTo(roll2);
    }

    @Test
    void equals_DifferentDiceType_ReturnsFalse() {
        DamageRoll roll1 = DamageRoll.parse("2d10+3 mag");
        DamageRoll roll2 = DamageRoll.parse("2d12+3 mag");

        assertThat(roll1).isNotEqualTo(roll2);
    }

    @Test
    void equals_DifferentModifier_ReturnsFalse() {
        DamageRoll roll1 = DamageRoll.parse("2d10+3 mag");
        DamageRoll roll2 = DamageRoll.parse("2d10+5 mag");

        assertThat(roll1).isNotEqualTo(roll2);
    }

    @Test
    void equals_DifferentDamageType_ReturnsFalse() {
        DamageRoll roll1 = DamageRoll.parse("2d10+3 mag");
        DamageRoll roll2 = DamageRoll.parse("2d10+3 phy");

        assertThat(roll1).isNotEqualTo(roll2);
    }

    @Test
    void hashCode_SameValues_ReturnsSameHash() {
        DamageRoll roll1 = DamageRoll.parse("2d10+3 mag");
        DamageRoll roll2 = DamageRoll.parse("2d10+3 mag");

        assertThat(roll1.hashCode()).isEqualTo(roll2.hashCode());
    }
}

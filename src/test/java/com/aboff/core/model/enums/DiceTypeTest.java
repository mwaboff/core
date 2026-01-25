package com.aboff.core.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiceTypeTest {

    // ==================== SIDES TESTS ====================

    @Test
    void getSides_D4_Returns4() {
        assertThat(DiceType.D4.getSides()).isEqualTo(4);
    }

    @Test
    void getSides_D6_Returns6() {
        assertThat(DiceType.D6.getSides()).isEqualTo(6);
    }

    @Test
    void getSides_D8_Returns8() {
        assertThat(DiceType.D8.getSides()).isEqualTo(8);
    }

    @Test
    void getSides_D10_Returns10() {
        assertThat(DiceType.D10.getSides()).isEqualTo(10);
    }

    @Test
    void getSides_D12_Returns12() {
        assertThat(DiceType.D12.getSides()).isEqualTo(12);
    }

    @Test
    void getSides_D20_Returns20() {
        assertThat(DiceType.D20.getSides()).isEqualTo(20);
    }

    // ==================== CODE TESTS ====================

    @Test
    void getCode_D4_ReturnsLowercaseCode() {
        assertThat(DiceType.D4.getCode()).isEqualTo("d4");
    }

    @Test
    void getCode_D20_ReturnsLowercaseCode() {
        assertThat(DiceType.D20.getCode()).isEqualTo("d20");
    }

    // ==================== FROM CODE TESTS ====================

    @Test
    void fromCode_LowercaseD4_ReturnsD4() {
        assertThat(DiceType.fromCode("d4")).isEqualTo(DiceType.D4);
    }

    @Test
    void fromCode_UppercaseD10_ReturnsD10() {
        assertThat(DiceType.fromCode("D10")).isEqualTo(DiceType.D10);
    }

    @Test
    void fromCode_MixedCaseD12_ReturnsD12() {
        assertThat(DiceType.fromCode("D12")).isEqualTo(DiceType.D12);
    }

    @Test
    void fromCode_WithWhitespace_TrimsAndReturnsCorrectType() {
        assertThat(DiceType.fromCode("  d6  ")).isEqualTo(DiceType.D6);
    }

    @Test
    void fromCode_AllValidDiceTypes_ReturnCorrectType() {
        assertThat(DiceType.fromCode("d4")).isEqualTo(DiceType.D4);
        assertThat(DiceType.fromCode("d6")).isEqualTo(DiceType.D6);
        assertThat(DiceType.fromCode("d8")).isEqualTo(DiceType.D8);
        assertThat(DiceType.fromCode("d10")).isEqualTo(DiceType.D10);
        assertThat(DiceType.fromCode("d12")).isEqualTo(DiceType.D12);
        assertThat(DiceType.fromCode("d20")).isEqualTo(DiceType.D20);
    }

    // ==================== ERROR CASES ====================

    @Test
    void fromCode_NullCode_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DiceType.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dice code cannot be null or blank");
    }

    @Test
    void fromCode_EmptyString_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DiceType.fromCode(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dice code cannot be null or blank");
    }

    @Test
    void fromCode_BlankString_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DiceType.fromCode("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dice code cannot be null or blank");
    }

    @Test
    void fromCode_InvalidDiceType_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DiceType.fromCode("d7"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown dice type: d7");
    }

    @Test
    void fromCode_InvalidFormat_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DiceType.fromCode("10"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown dice type: 10");
    }

    @Test
    void fromCode_D100NotSupported_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> DiceType.fromCode("d100"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown dice type: d100");
    }
}

package com.aboff.core.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Range enum.
 * Tests verify that all range categories exist and can be properly used.
 */
class RangeTest {

    @Test
    void allRangeValues_ExistAndAreAccessible() {
        assertThat(Range.MELEE).isNotNull();
        assertThat(Range.VERY_CLOSE).isNotNull();
        assertThat(Range.CLOSE).isNotNull();
        assertThat(Range.FAR).isNotNull();
        assertThat(Range.VERY_FAR).isNotNull();
        assertThat(Range.OUT_OF_RANGE).isNotNull();
    }

    @Test
    void valueOf_ValidRangeName_ReturnsCorrectValue() {
        assertThat(Range.valueOf("MELEE")).isEqualTo(Range.MELEE);
        assertThat(Range.valueOf("VERY_CLOSE")).isEqualTo(Range.VERY_CLOSE);
        assertThat(Range.valueOf("CLOSE")).isEqualTo(Range.CLOSE);
        assertThat(Range.valueOf("FAR")).isEqualTo(Range.FAR);
        assertThat(Range.valueOf("VERY_FAR")).isEqualTo(Range.VERY_FAR);
        assertThat(Range.valueOf("OUT_OF_RANGE")).isEqualTo(Range.OUT_OF_RANGE);
    }

    @Test
    void values_ReturnsAllRangeCategories() {
        Range[] values = Range.values();

        assertThat(values).hasSize(6);
        assertThat(values).containsExactlyInAnyOrder(
            Range.MELEE,
            Range.VERY_CLOSE,
            Range.CLOSE,
            Range.FAR,
            Range.VERY_FAR,
            Range.OUT_OF_RANGE
        );
    }

    @Test
    void name_ReturnsCorrectEnumName() {
        assertThat(Range.MELEE.name()).isEqualTo("MELEE");
        assertThat(Range.VERY_CLOSE.name()).isEqualTo("VERY_CLOSE");
        assertThat(Range.CLOSE.name()).isEqualTo("CLOSE");
        assertThat(Range.FAR.name()).isEqualTo("FAR");
        assertThat(Range.VERY_FAR.name()).isEqualTo("VERY_FAR");
        assertThat(Range.OUT_OF_RANGE.name()).isEqualTo("OUT_OF_RANGE");
    }

    @Test
    void toString_ReturnsEnumName() {
        assertThat(Range.MELEE.toString()).isEqualTo("MELEE");
        assertThat(Range.FAR.toString()).isEqualTo("FAR");
    }

    @Test
    void ordinal_ReturnsCorrectPosition() {
        assertThat(Range.MELEE.ordinal()).isEqualTo(0);
        assertThat(Range.VERY_CLOSE.ordinal()).isEqualTo(1);
        assertThat(Range.CLOSE.ordinal()).isEqualTo(2);
        assertThat(Range.FAR.ordinal()).isEqualTo(3);
        assertThat(Range.VERY_FAR.ordinal()).isEqualTo(4);
        assertThat(Range.OUT_OF_RANGE.ordinal()).isEqualTo(5);
    }
}

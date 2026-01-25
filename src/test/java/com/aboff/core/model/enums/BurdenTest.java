package com.aboff.core.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Burden enum.
 * Tests verify that all burden types exist and can be properly used.
 */
class BurdenTest {

    @Test
    void allBurdenValues_ExistAndAreAccessible() {
        assertThat(Burden.ONE_HANDED).isNotNull();
        assertThat(Burden.TWO_HANDED).isNotNull();
    }

    @Test
    void valueOf_ValidBurdenName_ReturnsCorrectValue() {
        assertThat(Burden.valueOf("ONE_HANDED")).isEqualTo(Burden.ONE_HANDED);
        assertThat(Burden.valueOf("TWO_HANDED")).isEqualTo(Burden.TWO_HANDED);
    }

    @Test
    void values_ReturnsAllBurdenTypes() {
        Burden[] values = Burden.values();

        assertThat(values).hasSize(2);
        assertThat(values).containsExactlyInAnyOrder(
            Burden.ONE_HANDED,
            Burden.TWO_HANDED
        );
    }

    @Test
    void name_ReturnsCorrectEnumName() {
        assertThat(Burden.ONE_HANDED.name()).isEqualTo("ONE_HANDED");
        assertThat(Burden.TWO_HANDED.name()).isEqualTo("TWO_HANDED");
    }

    @Test
    void toString_ReturnsEnumName() {
        assertThat(Burden.ONE_HANDED.toString()).isEqualTo("ONE_HANDED");
        assertThat(Burden.TWO_HANDED.toString()).isEqualTo("TWO_HANDED");
    }

    @Test
    void ordinal_ReturnsCorrectPosition() {
        assertThat(Burden.ONE_HANDED.ordinal()).isEqualTo(0);
        assertThat(Burden.TWO_HANDED.ordinal()).isEqualTo(1);
    }
}

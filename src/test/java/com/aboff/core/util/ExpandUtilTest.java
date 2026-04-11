package com.aboff.core.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ExpandUtilTest {

    @Test
    void parseExpand_NullInput_ReturnsEmptySet() {
        // Act
        Set<String> result = ExpandUtil.parseExpand(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void parseExpand_EmptyString_ReturnsEmptySet() {
        // Act
        Set<String> result = ExpandUtil.parseExpand("");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void parseExpand_WhitespaceOnly_ReturnsEmptySet() {
        // Act
        Set<String> result = ExpandUtil.parseExpand("   ");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void parseExpand_SingleValue_ReturnsSetWithOneElement() {
        // Act
        Set<String> result = ExpandUtil.parseExpand("owner");

        // Assert
        assertThat(result).containsExactly("owner");
    }

    @Test
    void parseExpand_MultipleValues_ReturnsSetWithAllElements() {
        // Act
        Set<String> result = ExpandUtil.parseExpand("owner,experiences,inventoryWeapons");

        // Assert
        assertThat(result).containsExactlyInAnyOrder("owner", "experiences", "inventoryWeapons");
    }

    @Test
    void parseExpand_DuplicateValues_ReturnsSetWithUniqueElements() {
        // Act
        Set<String> result = ExpandUtil.parseExpand("owner,owner,experiences");

        // Assert
        assertThat(result).containsExactlyInAnyOrder("owner", "experiences");
    }

    @Test
    void shouldExpand_FieldInSet_ReturnsTrue() {
        // Arrange
        Set<String> expandSet = Set.of("owner", "experiences");

        // Act
        boolean result = ExpandUtil.shouldExpand(expandSet, "owner");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void shouldExpand_FieldNotInSet_ReturnsFalse() {
        // Arrange
        Set<String> expandSet = Set.of("owner", "experiences");

        // Act
        boolean result = ExpandUtil.shouldExpand(expandSet, "inventoryWeapons");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void shouldExpand_AllInSet_ReturnsTrueForAnyField() {
        // Arrange
        Set<String> expandSet = Set.of("all");

        // Act & Assert
        assertThat(ExpandUtil.shouldExpand(expandSet, "owner")).isTrue();
        assertThat(ExpandUtil.shouldExpand(expandSet, "experiences")).isTrue();
        assertThat(ExpandUtil.shouldExpand(expandSet, "inventoryWeapons")).isTrue();
        assertThat(ExpandUtil.shouldExpand(expandSet, "anyArbitraryField")).isTrue();
    }

    @Test
    void shouldExpand_EmptySet_ReturnsFalse() {
        // Arrange
        Set<String> expandSet = Set.of();

        // Act
        boolean result = ExpandUtil.shouldExpand(expandSet, "owner");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void shouldExpand_ParseExpandAllWithShouldExpand_EndToEndExpansion() {
        // Arrange - simulate parseExpand("all") feeding into shouldExpand
        Set<String> expandSet = ExpandUtil.parseExpand("all");

        // Act & Assert - any field should be expanded when "all" is requested
        assertThat(ExpandUtil.shouldExpand(expandSet, "owner")).isTrue();
        assertThat(ExpandUtil.shouldExpand(expandSet, "experiences")).isTrue();
        assertThat(ExpandUtil.shouldExpand(expandSet, "inventoryWeapons")).isTrue();
    }
}

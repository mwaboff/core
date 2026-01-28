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
}

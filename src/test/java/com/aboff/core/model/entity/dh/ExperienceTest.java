package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Experience entity.
 */
class ExperienceTest {

    // ==================== BUILDER TESTS ====================

    @Test
    void builder_WithAllFields_CreatesValidInstance() {
        User user = User.builder().id(1L).username("testuser").build();
        CharacterSheet sheet = CharacterSheet.builder().id(1L).name("Test Character").build();

        Experience experience = Experience.builder()
                .characterSheet(sheet)
                .createdBy(user)
                .description("Survived a dragon attack")
                .modifier(3)
                .build();

        assertThat(experience.getCharacterSheet()).isEqualTo(sheet);
        assertThat(experience.getCreatedBy()).isEqualTo(user);
        assertThat(experience.getDescription()).isEqualTo("Survived a dragon attack");
        assertThat(experience.getModifier()).isEqualTo(3);
    }

    @Test
    void builder_WithMinimalFields_UsesDefaults() {
        User user = User.builder().id(1L).username("testuser").build();
        CharacterSheet sheet = CharacterSheet.builder().id(1L).name("Test Character").build();

        Experience experience = Experience.builder()
                .characterSheet(sheet)
                .createdBy(user)
                .description("Apprenticed with a blacksmith")
                .build();

        assertThat(experience.getCharacterSheet()).isEqualTo(sheet);
        assertThat(experience.getCreatedBy()).isEqualTo(user);
        assertThat(experience.getDescription()).isEqualTo("Apprenticed with a blacksmith");
        assertThat(experience.getModifier()).isEqualTo(2); // Default value
    }

    // ==================== DEFAULT VALUES TESTS ====================

    @Test
    void defaultModifier_IsTwo() {
        Experience experience = Experience.builder()
                .description("Test experience")
                .build();

        assertThat(experience.getModifier()).isEqualTo(2);
    }

    // ==================== FIELD SETTERS TESTS ====================

    @Test
    void setDescription_UpdatesValue() {
        Experience experience = Experience.builder()
                .description("Original description")
                .build();

        experience.setDescription("Updated description");

        assertThat(experience.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void setModifier_UpdatesValue() {
        Experience experience = Experience.builder()
                .modifier(2)
                .build();

        experience.setModifier(5);

        assertThat(experience.getModifier()).isEqualTo(5);
    }

    @Test
    void setModifier_AllowsNegativeValues() {
        Experience experience = Experience.builder()
                .modifier(2)
                .build();

        experience.setModifier(-1);

        assertThat(experience.getModifier()).isEqualTo(-1);
    }

    @Test
    void setCharacterSheet_UpdatesValue() {
        CharacterSheet sheet1 = CharacterSheet.builder().id(1L).name("Character 1").build();
        CharacterSheet sheet2 = CharacterSheet.builder().id(2L).name("Character 2").build();

        Experience experience = Experience.builder()
                .characterSheet(sheet1)
                .build();

        experience.setCharacterSheet(sheet2);

        assertThat(experience.getCharacterSheet()).isEqualTo(sheet2);
    }

    @Test
    void setCreatedBy_UpdatesValue() {
        User user1 = User.builder().id(1L).username("user1").build();
        User user2 = User.builder().id(2L).username("user2").build();

        Experience experience = Experience.builder()
                .createdBy(user1)
                .build();

        experience.setCreatedBy(user2);

        assertThat(experience.getCreatedBy()).isEqualTo(user2);
    }

    // ==================== EQUALITY TESTS ====================

    @Test
    void equals_SameValues_ReturnsTrue() {
        User user = User.builder().id(1L).username("testuser").build();
        CharacterSheet sheet = CharacterSheet.builder().id(1L).name("Test Character").build();

        Experience exp1 = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(user)
                .description("Test description")
                .modifier(2)
                .build();

        Experience exp2 = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(user)
                .description("Test description")
                .modifier(2)
                .build();

        assertThat(exp1).isEqualTo(exp2);
    }

    @Test
    void equals_DifferentDescription_ReturnsFalse() {
        Experience exp1 = Experience.builder().description("Test 1").build();
        Experience exp2 = Experience.builder().description("Test 2").build();

        assertThat(exp1).isNotEqualTo(exp2);
    }

    @Test
    void hashCode_SameValues_ReturnsSameHash() {
        User user = User.builder().id(1L).username("testuser").build();
        CharacterSheet sheet = CharacterSheet.builder().id(1L).name("Test Character").build();

        Experience exp1 = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(user)
                .description("Test description")
                .modifier(2)
                .build();

        Experience exp2 = Experience.builder()
                .id(1L)
                .characterSheet(sheet)
                .createdBy(user)
                .description("Test description")
                .modifier(2)
                .build();

        assertThat(exp1.hashCode()).isEqualTo(exp2.hashCode());
    }

    // ==================== LONG DESCRIPTION TESTS ====================

    @Test
    void description_SupportsLongText() {
        String longDescription = "A".repeat(1000);

        Experience experience = Experience.builder()
                .description(longDescription)
                .build();

        assertThat(experience.getDescription()).hasSize(1000);
        assertThat(experience.getDescription()).isEqualTo(longDescription);
    }

    // ==================== NULL HANDLING TESTS ====================

    @Test
    void builder_WithNullDescription_AllowsNull() {
        Experience experience = Experience.builder()
                .description(null)
                .build();

        assertThat(experience.getDescription()).isNull();
    }

    @Test
    void builder_WithNullCharacterSheet_AllowsNull() {
        Experience experience = Experience.builder()
                .characterSheet(null)
                .build();

        assertThat(experience.getCharacterSheet()).isNull();
    }

    @Test
    void builder_WithNullCreatedBy_AllowsNull() {
        Experience experience = Experience.builder()
                .createdBy(null)
                .build();

        assertThat(experience.getCreatedBy()).isNull();
    }
}

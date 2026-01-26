package com.aboff.core.repository;

import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ExperienceRepository.
 * Tests database operations and custom query methods.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ExperienceRepositoryIntegrationTest {

    @Autowired
    private ExperienceRepository experienceRepository;

    @Autowired
    private CharacterSheetRepository characterSheetRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private CharacterSheet sheet1;
    private CharacterSheet sheet2;

    @BeforeEach
    void setUp() {
        experienceRepository.deleteAll();
        characterSheetRepository.deleteAll();
        userRepository.deleteAll();

        user1 = createUser("player1", "player1@example.com");
        user2 = createUser("player2", "player2@example.com");

        sheet1 = createCharacterSheet("Character 1", user1);
        sheet2 = createCharacterSheet("Character 2", user2);
    }

    // ==================== CREATE TESTS ====================

    @Test
    void save_WithValidData_PersistsExperience() {
        Experience experience = Experience.builder()
                .characterSheet(sheet1)
                .createdBy(user1)
                .description("Survived a dragon attack on Redstone Village")
                .modifier(2)
                .build();

        Experience saved = experienceRepository.save(experience);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCharacterSheet()).isEqualTo(sheet1);
        assertThat(saved.getCreatedBy()).isEqualTo(user1);
        assertThat(saved.getDescription()).isEqualTo("Survived a dragon attack on Redstone Village");
        assertThat(saved.getModifier()).isEqualTo(2);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getLastModifiedAt()).isNotNull();
    }

    @Test
    void save_WithDefaultModifier_UsesExpectedDefault() {
        Experience experience = Experience.builder()
                .characterSheet(sheet1)
                .createdBy(user1)
                .description("Apprenticed with a blacksmith")
                .build();

        Experience saved = experienceRepository.save(experience);

        assertThat(saved.getModifier()).isEqualTo(2);
    }

    @Test
    void save_WithCustomModifier_PersistsCustomValue() {
        Experience experience = Experience.builder()
                .characterSheet(sheet1)
                .createdBy(user1)
                .description("Epic dragon slaying")
                .modifier(5)
                .build();

        Experience saved = experienceRepository.save(experience);

        assertThat(saved.getModifier()).isEqualTo(5);
    }

    @Test
    void save_WithNegativeModifier_PersistsNegativeValue() {
        Experience experience = Experience.builder()
                .characterSheet(sheet1)
                .createdBy(user1)
                .description("Traumatic event")
                .modifier(-1)
                .build();

        Experience saved = experienceRepository.save(experience);

        assertThat(saved.getModifier()).isEqualTo(-1);
    }

    // ==================== READ TESTS ====================

    @Test
    void findById_ExistingExperience_ReturnsExperience() {
        Experience experience = createExperience(sheet1, user1, "Test experience");

        Optional<Experience> found = experienceRepository.findById(experience.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Test experience");
    }

    @Test
    void findById_NonexistentExperience_ReturnsEmpty() {
        Optional<Experience> found = experienceRepository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findByCharacterSheetId_MultipleExperiences_ReturnsAllOrderedByCreatedAtDesc() throws InterruptedException {
        Experience exp1 = createExperience(sheet1, user1, "First experience");
        Thread.sleep(10);
        Experience exp2 = createExperience(sheet1, user1, "Second experience");
        Thread.sleep(10);
        Experience exp3 = createExperience(sheet1, user1, "Third experience");
        createExperience(sheet2, user2, "Different character");

        List<Experience> experiences = experienceRepository.findByCharacterSheetId(sheet1.getId());

        assertThat(experiences).hasSize(3);
        // Should be ordered by creation date descending (newest first)
        assertThat(experiences.get(0).getDescription()).isEqualTo("Third experience");
        assertThat(experiences.get(1).getDescription()).isEqualTo("Second experience");
        assertThat(experiences.get(2).getDescription()).isEqualTo("First experience");
    }

    @Test
    void findByCharacterSheetId_NoExperiences_ReturnsEmptyList() {
        List<Experience> experiences = experienceRepository.findByCharacterSheetId(sheet1.getId());

        assertThat(experiences).isEmpty();
    }

    @Test
    void findByCreatedById_MultipleUsers_ReturnsOnlyForSpecifiedUser() {
        createExperience(sheet1, user1, "User1 Experience 1");
        createExperience(sheet1, user1, "User1 Experience 2");
        createExperience(sheet2, user2, "User2 Experience");
        createExperience(sheet1, user2, "User2 added to sheet1");

        List<Experience> user1Experiences = experienceRepository.findByCreatedById(user1.getId());
        List<Experience> user2Experiences = experienceRepository.findByCreatedById(user2.getId());

        assertThat(user1Experiences).hasSize(2);
        assertThat(user2Experiences).hasSize(2);
    }

    @Test
    void findByCreatedById_NoExperiences_ReturnsEmptyList() {
        List<Experience> experiences = experienceRepository.findByCreatedById(user1.getId());

        assertThat(experiences).isEmpty();
    }

    @Test
    void countByCharacterSheetId_MultipleExperiences_ReturnsCorrectCount() {
        createExperience(sheet1, user1, "Experience 1");
        createExperience(sheet1, user1, "Experience 2");
        createExperience(sheet1, user2, "Experience 3");
        createExperience(sheet2, user2, "Different character");

        Long count = experienceRepository.countByCharacterSheetId(sheet1.getId());

        assertThat(count).isEqualTo(3);
    }

    @Test
    void countByCharacterSheetId_NoExperiences_ReturnsZero() {
        Long count = experienceRepository.countByCharacterSheetId(sheet1.getId());

        assertThat(count).isEqualTo(0);
    }

    @Test
    void findByCharacterSheetIdAndCreatedById_SpecificCombination_ReturnsMatching() {
        createExperience(sheet1, user1, "User1 on Sheet1 - Exp1");
        createExperience(sheet1, user1, "User1 on Sheet1 - Exp2");
        createExperience(sheet1, user2, "User2 on Sheet1");
        createExperience(sheet2, user1, "User1 on Sheet2");

        List<Experience> found = experienceRepository.findByCharacterSheetIdAndCreatedById(
                sheet1.getId(), user1.getId());

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Experience::getDescription)
                .containsExactlyInAnyOrder("User1 on Sheet1 - Exp1", "User1 on Sheet1 - Exp2");
    }

    @Test
    void findByCharacterSheetIdAndCreatedById_NoMatches_ReturnsEmptyList() {
        createExperience(sheet1, user1, "User1 on Sheet1");

        List<Experience> found = experienceRepository.findByCharacterSheetIdAndCreatedById(
                sheet2.getId(), user2.getId());

        assertThat(found).isEmpty();
    }

    // ==================== UPDATE TESTS ====================

    @Test
    void update_ModifyingDescription_PersistsChanges() {
        Experience experience = createExperience(sheet1, user1, "Original description");
        Long id = experience.getId();

        experience.setDescription("Updated description");
        experienceRepository.save(experience);

        Experience updated = experienceRepository.findById(id).orElseThrow();
        assertThat(updated.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void update_ModifyingModifier_PersistsChanges() {
        Experience experience = createExperience(sheet1, user1, "Test experience");
        Long id = experience.getId();

        experience.setModifier(5);
        experienceRepository.save(experience);

        Experience updated = experienceRepository.findById(id).orElseThrow();
        assertThat(updated.getModifier()).isEqualTo(5);
    }

    // ==================== DELETE TESTS ====================

    @Test
    void delete_RemovesFromDatabase() {
        Experience experience = createExperience(sheet1, user1, "To delete");
        Long id = experience.getId();

        experienceRepository.delete(experience);

        Optional<Experience> found = experienceRepository.findById(id);
        assertThat(found).isEmpty();
    }

    // Note: Cascade deletion is tested at the database level through the ON DELETE CASCADE
    // constraint in the migration. The JPA @OneToMany relationship with cascade = CascadeType.ALL
    // ensures proper behavior, which is verified through the database schema itself.

    // ==================== LONG TEXT TESTS ====================

    @Test
    void save_WithLongDescription_PersistsSuccessfully() {
        String longDescription = "A".repeat(5000);

        Experience experience = Experience.builder()
                .characterSheet(sheet1)
                .createdBy(user1)
                .description(longDescription)
                .build();

        Experience saved = experienceRepository.save(experience);

        assertThat(saved.getDescription()).hasSize(5000);
        assertThat(saved.getDescription()).isEqualTo(longDescription);
    }

    // ==================== COMPLEX QUERIES TESTS ====================

    @Test
    void findAll_WithMultipleCharacters_ReturnsAllExperiences() {
        createExperience(sheet1, user1, "Exp1");
        createExperience(sheet1, user2, "Exp2");
        createExperience(sheet2, user2, "Exp3");

        List<Experience> all = experienceRepository.findAll();

        assertThat(all).hasSize(3);
    }

    @Test
    void count_WithMultipleExperiences_ReturnsCorrectTotal() {
        createExperience(sheet1, user1, "Exp1");
        createExperience(sheet1, user2, "Exp2");
        createExperience(sheet2, user2, "Exp3");

        long count = experienceRepository.count();

        assertThat(count).isEqualTo(3);
    }

    // ==================== HELPER METHODS ====================

    private User createUser(String username, String email) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();
        return userRepository.save(user);
    }

    private CharacterSheet createCharacterSheet(String name, User owner) {
        CharacterSheet sheet = CharacterSheet.builder()
                .name(name)
                .majorDamageThreshold(8)
                .severeDamageThreshold(12)
                .owner(owner)
                .build();
        return characterSheetRepository.save(sheet);
    }

    private Experience createExperience(CharacterSheet sheet, User creator, String description) {
        Experience experience = Experience.builder()
                .characterSheet(sheet)
                .createdBy(creator)
                .description(description)
                .modifier(2)
                .build();
        return experienceRepository.save(experience);
    }
}

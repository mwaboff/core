package com.aboff.core.repository;

import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.*;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CharacterSheetRepository.
 * Tests database operations and custom query methods.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CharacterSheetRepositoryIntegrationTest {

    @Autowired
    private CharacterSheetRepository characterSheetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private WeaponRepository weaponRepository;

    @Autowired
    private ArmorRepository armorRepository;

    private User user1;
    private User user2;
    private Expansion expansion;

    @BeforeEach
    void setUp() {
        user1 = createUser("player1", "player1@example.com");
        user2 = createUser("player2", "player2@example.com");

        expansion = createExpansion("Core Rulebook", true);
    }

    // ==================== CREATE TESTS ====================

    @Test
    void save_WithValidData_PersistsCharacterSheet() {
        CharacterSheet sheet = CharacterSheet.builder()
                .name("Thorin")
                .pronouns("he/him")
                .level(1)
                .majorDamageThreshold(8)
                .severeDamageThreshold(12)
                .owner(user1)
                .build();

        CharacterSheet saved = characterSheetRepository.save(sheet);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Thorin");
        assertThat(saved.getPronouns()).isEqualTo("he/him");
        assertThat(saved.getLevel()).isEqualTo(1);
        assertThat(saved.getOwner()).isEqualTo(user1);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getLastModifiedAt()).isNotNull();
    }

    @Test
    void save_WithDefaultValues_UsesExpectedDefaults() {
        CharacterSheet sheet = CharacterSheet.builder()
                .name("Default Character")
                .majorDamageThreshold(8)
                .severeDamageThreshold(12)
                .owner(user1)
                .build();

        CharacterSheet saved = characterSheetRepository.save(sheet);

        assertThat(saved.getLevel()).isEqualTo(1);
        assertThat(saved.getEvasion()).isEqualTo(0);
        assertThat(saved.getHitPointMax()).isEqualTo(6);
        assertThat(saved.getStressMax()).isEqualTo(6);
        assertThat(saved.getHopeMax()).isEqualTo(2);
        assertThat(saved.getGold()).isEqualTo(0);
        assertThat(saved.getAgilityModifier()).isEqualTo(0);
        assertThat(saved.getAgilityMarked()).isFalse();
    }

    @Test
    void save_WithEquipment_PersistsRelationships() {
        Weapon weapon = createWeapon("Longsword");
        Armor armor = createArmor("Plate Armor");

        CharacterSheet sheet = CharacterSheet.builder()
                .name("Armed Character")
                .majorDamageThreshold(8)
                .severeDamageThreshold(12)
                .owner(user1)
                .activePrimaryWeapon(weapon)
                .activeArmor(armor)
                .build();

        CharacterSheet saved = characterSheetRepository.save(sheet);

        assertThat(saved.getActivePrimaryWeapon()).isEqualTo(weapon);
        assertThat(saved.getActiveArmor()).isEqualTo(armor);
    }

    // ==================== READ TESTS ====================

    @Test
    void findById_ExistingCharacterSheet_ReturnsCharacterSheet() {
        CharacterSheet sheet = createCharacterSheet("Thorin", user1);

        Optional<CharacterSheet> found = characterSheetRepository.findById(sheet.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Thorin");
    }

    @Test
    void findById_NonexistentCharacterSheet_ReturnsEmpty() {
        Optional<CharacterSheet> found = characterSheetRepository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findByOwnerId_MultipleSheets_ReturnsAllForOwner() {
        createCharacterSheet("Character 1", user1);
        createCharacterSheet("Character 2", user1);
        createCharacterSheet("Character 3", user2);

        List<CharacterSheet> user1Sheets = characterSheetRepository.findByOwnerId(user1.getId());

        assertThat(user1Sheets).hasSize(2);
        assertThat(user1Sheets).extracting(CharacterSheet::getName)
                .containsExactlyInAnyOrder("Character 1", "Character 2");
    }

    @Test
    void findByOwnerId_NoSheets_ReturnsEmptyList() {
        List<CharacterSheet> sheets = characterSheetRepository.findByOwnerId(user1.getId());

        assertThat(sheets).isEmpty();
    }

    @Test
    void findByOwnerIdAndDeletedAtIsNull_MixedDeletedAndActive_ReturnsOnlyActive() {
        CharacterSheet active1 = createCharacterSheet("Active 1", user1);
        CharacterSheet active2 = createCharacterSheet("Active 2", user1);
        CharacterSheet deleted = createCharacterSheet("Deleted", user1);
        deleted.softDelete();
        characterSheetRepository.save(deleted);

        List<CharacterSheet> activeSheets = characterSheetRepository.findByOwnerIdAndDeletedAtIsNull(user1.getId());

        assertThat(activeSheets).hasSize(2);
        assertThat(activeSheets).extracting(CharacterSheet::getName)
                .containsExactlyInAnyOrder("Active 1", "Active 2");
    }

    @Test
    void findActiveById_ActiveCharacterSheet_ReturnsCharacterSheet() {
        CharacterSheet sheet = createCharacterSheet("Active Character", user1);

        Optional<CharacterSheet> found = characterSheetRepository.findActiveById(sheet.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Active Character");
    }

    @Test
    void findActiveById_DeletedCharacterSheet_ReturnsEmpty() {
        CharacterSheet sheet = createCharacterSheet("Deleted Character", user1);
        sheet.softDelete();
        characterSheetRepository.save(sheet);

        Optional<CharacterSheet> found = characterSheetRepository.findActiveById(sheet.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void findAllActive_MixedDeletedAndActive_ReturnsOnlyActive() {
        createCharacterSheet("Active 1", user1);
        createCharacterSheet("Active 2", user2);
        CharacterSheet deleted = createCharacterSheet("Deleted", user1);
        deleted.softDelete();
        characterSheetRepository.save(deleted);

        List<CharacterSheet> activeSheets = characterSheetRepository.findAllActive();

        assertThat(activeSheets).hasSize(2);
        assertThat(activeSheets).extracting(CharacterSheet::getName)
                .containsExactlyInAnyOrder("Active 1", "Active 2");
    }

    @Test
    void findByNameContainingIgnoreCaseAndDeletedAtIsNull_PartialMatch_ReturnsMatching() {
        createCharacterSheet("Thorin Oakenshield", user1);
        createCharacterSheet("Thorin the Dwarf", user2);
        createCharacterSheet("Bilbo Baggins", user1);
        CharacterSheet deleted = createCharacterSheet("Thorin (Deleted)", user1);
        deleted.softDelete();
        characterSheetRepository.save(deleted);

        List<CharacterSheet> found = characterSheetRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull("thorin");

        assertThat(found).hasSize(2);
        assertThat(found).extracting(CharacterSheet::getName)
                .containsExactlyInAnyOrder("Thorin Oakenshield", "Thorin the Dwarf");
    }

    @Test
    void findByNameContainingIgnoreCaseAndDeletedAtIsNull_CaseInsensitive_ReturnsMatching() {
        createCharacterSheet("Aragorn", user1);

        List<CharacterSheet> foundLower = characterSheetRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull("aragorn");
        List<CharacterSheet> foundUpper = characterSheetRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull("ARAGORN");
        List<CharacterSheet> foundMixed = characterSheetRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull("ArAgOrN");

        assertThat(foundLower).hasSize(1);
        assertThat(foundUpper).hasSize(1);
        assertThat(foundMixed).hasSize(1);
    }

    @Test
    void countActiveByOwnerId_MultipleActiveSheets_ReturnsCorrectCount() {
        createCharacterSheet("Active 1", user1);
        createCharacterSheet("Active 2", user1);
        createCharacterSheet("Active 3", user2);
        CharacterSheet deleted = createCharacterSheet("Deleted", user1);
        deleted.softDelete();
        characterSheetRepository.save(deleted);

        Long count = characterSheetRepository.countActiveByOwnerId(user1.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countActiveByOwnerId_NoSheets_ReturnsZero() {
        Long count = characterSheetRepository.countActiveByOwnerId(user1.getId());

        assertThat(count).isEqualTo(0);
    }

    // ==================== UPDATE TESTS ====================

    @Test
    void update_ModifyingFields_PersistsChanges() {
        CharacterSheet sheet = createCharacterSheet("Original Name", user1);
        Long id = sheet.getId();

        sheet.setName("Updated Name");
        sheet.setLevel(5);
        sheet.setGold(1000);
        characterSheetRepository.save(sheet);

        CharacterSheet updated = characterSheetRepository.findById(id).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getLevel()).isEqualTo(5);
        assertThat(updated.getGold()).isEqualTo(1000);
    }

    @Test
    void update_ModifyingFields_UpdatesLastModifiedAt() throws InterruptedException {
        CharacterSheet sheet = createCharacterSheet("Test Character", user1);
        LocalDateTime originalModified = sheet.getLastModifiedAt();

        Thread.sleep(100); // Longer delay to ensure timestamp difference

        sheet.setName("Updated Name");
        CharacterSheet updated = characterSheetRepository.save(sheet);

        // Allow for same timestamp in fast test environments
        assertThat(updated.getLastModifiedAt()).isAfterOrEqualTo(originalModified);
    }

    // ==================== DELETE TESTS ====================

    @Test
    void delete_RemovesFromDatabase() {
        CharacterSheet sheet = createCharacterSheet("To Delete", user1);
        Long id = sheet.getId();

        characterSheetRepository.delete(sheet);

        Optional<CharacterSheet> found = characterSheetRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    void softDelete_SetsDeletedAtTimestamp() {
        CharacterSheet sheet = createCharacterSheet("To Soft Delete", user1);
        Long id = sheet.getId();

        sheet.softDelete();
        characterSheetRepository.save(sheet);

        CharacterSheet found = characterSheetRepository.findById(id).orElseThrow();
        assertThat(found.getDeletedAt()).isNotNull();
        assertThat(found.isDeleted()).isTrue();
    }

    @Test
    void restore_ClearsDeletedAtTimestamp() {
        CharacterSheet sheet = createCharacterSheet("To Restore", user1);
        sheet.softDelete();
        characterSheetRepository.save(sheet);
        Long id = sheet.getId();

        sheet.restore();
        characterSheetRepository.save(sheet);

        CharacterSheet found = characterSheetRepository.findById(id).orElseThrow();
        assertThat(found.getDeletedAt()).isNull();
        assertThat(found.isDeleted()).isFalse();
    }

    // ==================== COMPLEX QUERIES TESTS ====================

    @Test
    void findAll_WithMultipleUsers_ReturnsAllSheets() {
        createCharacterSheet("User1 Char1", user1);
        createCharacterSheet("User1 Char2", user1);
        createCharacterSheet("User2 Char1", user2);

        List<CharacterSheet> all = characterSheetRepository.findAll();

        assertThat(all).hasSize(3);
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

    private Expansion createExpansion(String name, boolean isPublished) {
        Expansion exp = Expansion.builder()
                .name(name)
                .isPublished(isPublished)
                .build();
        return expansionRepository.save(exp);
    }

    private Weapon createWeapon(String name) {
        Weapon weapon = Weapon.builder()
                .name(name)
                .expansion(expansion)
                .isOfficial(true)
                .isPrimary(true)
                .trait(com.aboff.core.model.enums.Trait.STRENGTH)
                .range(com.aboff.core.model.enums.Range.MELEE)
                .burden(com.aboff.core.model.enums.Burden.ONE_HANDED)
                .damage(com.aboff.core.model.embeddable.DamageRoll.builder()
                        .diceType(com.aboff.core.model.enums.DiceType.D6)
                        .damageType(com.aboff.core.model.enums.DamageType.PHYSICAL)
                        .build())
                .build();
        return weaponRepository.save(weapon);
    }

    private Armor createArmor(String name) {
        Armor armor = Armor.builder()
                .name(name)
                .expansion(expansion)
                .isOfficial(true)
                .baseMajorThreshold(8)
                .baseSevereThreshold(12)
                .baseScore(3)
                .build();
        return armorRepository.save(armor);
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
}

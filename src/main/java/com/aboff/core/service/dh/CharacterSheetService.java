package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.model.dto.dh.request.CreateCharacterSheetRequest;
import com.aboff.core.model.dto.dh.request.InventoryArmorRequest;
import com.aboff.core.model.dto.dh.request.InventoryLootRequest;
import com.aboff.core.model.dto.dh.request.InventoryWeaponRequest;
import com.aboff.core.model.dto.dh.request.UpdateCharacterSheetRequest;
import com.aboff.core.model.dto.dh.response.*;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.*;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.ExperienceRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.*;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import com.aboff.core.util.ExpandUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing CharacterSheet entities.
 * <p>
 * Handles business logic for CRUD operations on character sheets, including
 * access control validation, pagination, filtering, and relationship expansion.
 * </p>
 * <p>
 * Access control:
 * - Create: Any authenticated user
 * - Read: Any authenticated user
 * - Update/Delete: Character sheet owner OR users with MODERATOR/ADMIN/OWNER role
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CharacterSheetService {

    private final AuditLogger auditLogger;
    private final CharacterSheetRepository characterSheetRepository;
    private final CharacterSheetDomainCardRepository characterSheetDomainCardRepository;
    private final CharacterSheetWeaponRepository characterSheetWeaponRepository;
    private final CharacterSheetArmorRepository characterSheetArmorRepository;
    private final CharacterSheetLootRepository characterSheetLootRepository;
    private final UserRepository userRepository;
    private final ExperienceRepository experienceRepository;
    private final WeaponRepository weaponRepository;
    private final ArmorRepository armorRepository;
    private final CommunityCardRepository communityCardRepository;
    private final AncestryCardRepository ancestryCardRepository;
    private final SubclassCardRepository subclassCardRepository;
    private final DomainCardRepository domainCardRepository;
    private final LootRepository lootRepository;
    private final CampaignRepository campaignRepository;
    private final RoleHierarchyService roleHierarchyService;
    private final WeaponService weaponService;
    private final ArmorService armorService;
    private final CommunityCardService communityCardService;
    private final AncestryCardService ancestryCardService;
    private final SubclassCardService subclassCardService;
    private final DomainCardService domainCardService;
    private final LootService lootService;
    private final ClassService classService;

    /**
     * Retrieves a paginated list of character sheets.
     * <p>
     * Supports optional filtering by owner ID, name, and level range.
     * Regular users are automatically scoped to only see their own character sheets.
     * Privileged users (MODERATOR+) can see all character sheets and filter by any owner.
     * </p>
     *
     * @param page Zero-based page number
     * @param size Number of items per page (max 100)
     * @param ownerId Optional filter for owner ID (ignored for regular users, forced to own ID)
     * @param name Optional filter for name (case-insensitive partial match)
     * @param minLevel Optional filter for minimum level
     * @param maxLevel Optional filter for maximum level
     * @param expand Comma-separated list of relationships to expand (owner, experiences)
     * @param auth Authentication context
     * @return Paginated response containing character sheets
     */
    @Transactional(readOnly = true)
    public PagedResponse<CharacterSheetResponse> getAllCharacterSheets(
            int page,
            int size,
            Long ownerId,
            String name,
            Integer minLevel,
            Integer maxLevel,
            String expand,
            Authentication auth) {

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();
        if (!roleHierarchyService.isPrivilegedRole(user.getRole())) {
            ownerId = user.getId();
        }

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<CharacterSheet> characterSheetPage = characterSheetRepository.findActiveWithFilters(
                ownerId, name, minLevel, maxLevel, pageable);

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<CharacterSheetResponse>builder()
                .content(characterSheetPage.getContent().stream()
                        .map(sheet -> toResponse(sheet, expandSet))
                        .toList())
                .totalElements(characterSheetPage.getTotalElements())
                .totalPages(characterSheetPage.getTotalPages())
                .currentPage(characterSheetPage.getNumber())
                .pageSize(characterSheetPage.getSize())
                .build();
    }

    /**
     * Retrieves a single character sheet by ID.
     * <p>
     * When authentication is provided, the response may include campaign info
     * if the viewer has access to the campaign containing this character.
     * </p>
     *
     * @param id The character sheet ID
     * @param expand Comma-separated list of relationships to expand (owner, experiences)
     * @param auth The authentication object containing the current user (optional)
     * @return CharacterSheetResponse containing the character sheet details
     * @throws EntityNotFoundException if the character sheet is not found or is deleted
     */
    @Transactional(readOnly = true)
    public CharacterSheetResponse getCharacterSheetById(Long id, String expand, Authentication auth) {
        CharacterSheet characterSheet = characterSheetRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        CharacterSheetResponse response = toResponse(characterSheet, expandSet);

        // Populate campaign info if viewer has access
        if (auth != null) {
            populateCampaignInfo(response, characterSheet.getId(), auth);
        }

        return response;
    }

    /**
     * Retrieves a single character sheet by ID without authentication context.
     * <p>
     * Campaign info will not be populated in the response.
     * </p>
     *
     * @param id The character sheet ID
     * @param expand Comma-separated list of relationships to expand
     * @return CharacterSheetResponse containing the character sheet details
     * @throws EntityNotFoundException if the character sheet is not found or is deleted
     */
    @Transactional(readOnly = true)
    public CharacterSheetResponse getCharacterSheetById(Long id, String expand) {
        return getCharacterSheetById(id, expand, null);
    }

    /**
     * Creates a new character sheet.
     * <p>
     * Any authenticated user can create a character sheet. The creating user
     * becomes the owner of the character sheet. Supports setting equipment,
     * cards, and inventory on creation.
     * </p>
     *
     * @param request The creation request containing character sheet details
     * @param auth The authentication object containing the current user
     * @return CharacterSheetResponse containing the created character sheet
     */
    @Transactional
    public CharacterSheetResponse createCharacterSheet(CreateCharacterSheetRequest request, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        // Get the current user who will be the owner
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Build the character sheet
        CharacterSheet characterSheet = CharacterSheet.builder()
                .name(request.getName())
                .pronouns(request.getPronouns())
                .level(request.getLevel())
                .evasion(request.getEvasion())
                .armorMax(request.getArmorMax())
                .armorMarked(request.getArmorMarked())
                .majorDamageThreshold(request.getMajorDamageThreshold())
                .severeDamageThreshold(request.getSevereDamageThreshold())
                .agilityModifier(request.getAgilityModifier())
                .agilityMarked(request.getAgilityMarked())
                .strengthModifier(request.getStrengthModifier())
                .strengthMarked(request.getStrengthMarked())
                .finesseModifier(request.getFinesseModifier())
                .finesseMarked(request.getFinesseMarked())
                .instinctModifier(request.getInstinctModifier())
                .instinctMarked(request.getInstinctMarked())
                .presenceModifier(request.getPresenceModifier())
                .presenceMarked(request.getPresenceMarked())
                .knowledgeModifier(request.getKnowledgeModifier())
                .knowledgeMarked(request.getKnowledgeMarked())
                .hitPointMax(request.getHitPointMax())
                .hitPointMarked(request.getHitPointMarked())
                .stressMax(request.getStressMax())
                .stressMarked(request.getStressMarked())
                .hopeMax(request.getHopeMax())
                .hopeMarked(request.getHopeMarked())
                .gold(request.getGold())
                .proficiency(request.getProficiency() != null ? request.getProficiency() : 1)
                .owner(owner)
                .build();

        // Set card collections if provided
        if (request.getCommunityCardIds() != null) {
            Set<CommunityCard> communityCards = new HashSet<>();
            for (Long cardId : request.getCommunityCardIds()) {
                CommunityCard card = communityCardRepository.findById(cardId)
                        .orElseThrow(() -> new EntityNotFoundException("CommunityCard not found with id: " + cardId));
                communityCards.add(card);
            }
            characterSheet.setCommunityCards(communityCards);
        }
        if (request.getAncestryCardIds() != null) {
            Set<AncestryCard> ancestryCards = new HashSet<>();
            for (Long cardId : request.getAncestryCardIds()) {
                AncestryCard card = ancestryCardRepository.findById(cardId)
                        .orElseThrow(() -> new EntityNotFoundException("AncestryCard not found with id: " + cardId));
                ancestryCards.add(card);
            }
            characterSheet.setAncestryCards(ancestryCards);
        }
        if (request.getSubclassCardIds() != null) {
            Set<SubclassCard> subclassCards = new HashSet<>();
            for (Long cardId : request.getSubclassCardIds()) {
                SubclassCard card = subclassCardRepository.findById(cardId)
                        .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + cardId));
                subclassCards.add(card);
            }
            characterSheet.setSubclassCards(subclassCards);
        }
        if (request.getEquippedDomainCardIds() != null || request.getVaultDomainCardIds() != null) {
            if (request.getEquippedDomainCardIds() == null || request.getVaultDomainCardIds() == null) {
                throw new IllegalArgumentException("Both equippedDomainCardIds and vaultDomainCardIds must be provided together");
            }

            // Validate no duplicate IDs within or across both lists
            List<Long> allDomainIds = new ArrayList<>(request.getEquippedDomainCardIds());
            allDomainIds.addAll(request.getVaultDomainCardIds());
            if (allDomainIds.size() != new HashSet<>(allDomainIds).size()) {
                throw new IllegalArgumentException("Duplicate domain card IDs are not allowed; each card can only be assigned once to a character sheet");
            }

            Set<CharacterSheetDomainCard> domainCardEntities = new HashSet<>();
            for (Long cardId : request.getEquippedDomainCardIds()) {
                DomainCard card = domainCardRepository.findById(cardId)
                        .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + cardId));
                domainCardEntities.add(CharacterSheetDomainCard.builder()
                        .characterSheet(characterSheet).domainCard(card).equipped(true).build());
            }
            for (Long cardId : request.getVaultDomainCardIds()) {
                DomainCard card = domainCardRepository.findById(cardId)
                        .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + cardId));
                domainCardEntities.add(CharacterSheetDomainCard.builder()
                        .characterSheet(characterSheet).domainCard(card).equipped(false).build());
            }
            characterSheet.getCharacterSheetDomainCards().addAll(domainCardEntities);
        }

        // Set inventory collections if provided
        if (request.getInventoryWeapons() != null) {
            Set<CharacterSheetWeapon> weapons = new HashSet<>();
            for (InventoryWeaponRequest req : request.getInventoryWeapons()) {
                Weapon weapon = weaponRepository.findById(req.getWeaponId())
                        .orElseThrow(() -> new EntityNotFoundException("Weapon not found with id: " + req.getWeaponId()));
                weapons.add(CharacterSheetWeapon.builder()
                        .characterSheet(characterSheet)
                        .weapon(weapon)
                        .equipped(req.getEquipped() != null ? req.getEquipped() : false)
                        .slot(req.getSlot())
                        .build());
            }
            characterSheet.getCharacterSheetWeapons().addAll(weapons);
            validateWeaponSlots(characterSheet.getCharacterSheetWeapons());
        }
        if (request.getInventoryArmors() != null) {
            Set<CharacterSheetArmor> armors = new HashSet<>();
            for (InventoryArmorRequest req : request.getInventoryArmors()) {
                Armor armor = armorRepository.findById(req.getArmorId())
                        .orElseThrow(() -> new EntityNotFoundException("Armor not found with id: " + req.getArmorId()));
                armors.add(CharacterSheetArmor.builder()
                        .characterSheet(characterSheet)
                        .armor(armor)
                        .equipped(req.getEquipped() != null ? req.getEquipped() : false)
                        .build());
            }
            characterSheet.getCharacterSheetArmors().addAll(armors);
        }
        if (request.getInventoryItems() != null) {
            Set<CharacterSheetLoot> items = new HashSet<>();
            for (InventoryLootRequest req : request.getInventoryItems()) {
                Loot loot = lootRepository.findById(req.getLootId())
                        .orElseThrow(() -> new EntityNotFoundException("Loot not found with id: " + req.getLootId()));
                items.add(CharacterSheetLoot.builder()
                        .characterSheet(characterSheet)
                        .loot(loot)
                        .build());
            }
            characterSheet.getCharacterSheetLoot().addAll(items);
        }

        // Validate constraints
        validateConstraints(characterSheet);

        CharacterSheet savedSheet = characterSheetRepository.save(characterSheet);
        auditLogger.log(AuditAction.CHARACTER_CREATED, AuditContext.forUser(auth).build(),
                "\"" + savedSheet.getName() + "\" (character_sheet_id: " + savedSheet.getId() + ")");

        return toResponse(savedSheet, Set.of());
    }

    /**
     * Updates an existing character sheet.
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can update a character sheet. Supports partial updates - only non-null
     * fields are updated.
     * </p>
     *
     * @param id The character sheet ID to update
     * @param request The update request containing new character sheet details
     * @param auth The authentication object containing the current user
     * @return CharacterSheetResponse containing the updated character sheet
     * @throws EntityNotFoundException if the character sheet is not found
     * @throws InsufficientPermissionsException if the user lacks permission to update
     */
    @Transactional
    public CharacterSheetResponse updateCharacterSheet(Long id, UpdateCharacterSheetRequest request, Authentication auth) {
        CharacterSheet characterSheet = characterSheetRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + id));

        // Validate access - must be owner or moderator+
        validateAccess(characterSheet, auth, "update");

        // Update basic information
        if (request.getName() != null) {
            characterSheet.setName(request.getName());
        }
        if (request.getPronouns() != null) {
            characterSheet.setPronouns(request.getPronouns());
        }
        if (request.getLevel() != null) {
            characterSheet.setLevel(request.getLevel());
        }
        if (request.getProficiency() != null) {
            characterSheet.setProficiency(request.getProficiency());
        }

        // Update combat attributes
        if (request.getEvasion() != null) {
            characterSheet.setEvasion(request.getEvasion());
        }
        if (request.getArmorMax() != null) {
            characterSheet.setArmorMax(request.getArmorMax());
            // Clamp marked when the user explicitly lowers the base max below marked.
            if (characterSheet.getArmorMarked() > request.getArmorMax()) {
                characterSheet.setArmorMarked(request.getArmorMax());
            }
        }
        if (request.getArmorMarked() != null) {
            characterSheet.setArmorMarked(request.getArmorMarked());
        }
        if (request.getMajorDamageThreshold() != null) {
            characterSheet.setMajorDamageThreshold(request.getMajorDamageThreshold());
        }
        if (request.getSevereDamageThreshold() != null) {
            characterSheet.setSevereDamageThreshold(request.getSevereDamageThreshold());
        }

        // Update trait modifiers and marked status
        if (request.getAgilityModifier() != null) {
            characterSheet.setAgilityModifier(request.getAgilityModifier());
        }
        if (request.getAgilityMarked() != null) {
            characterSheet.setAgilityMarked(request.getAgilityMarked());
        }
        if (request.getStrengthModifier() != null) {
            characterSheet.setStrengthModifier(request.getStrengthModifier());
        }
        if (request.getStrengthMarked() != null) {
            characterSheet.setStrengthMarked(request.getStrengthMarked());
        }
        if (request.getFinesseModifier() != null) {
            characterSheet.setFinesseModifier(request.getFinesseModifier());
        }
        if (request.getFinesseMarked() != null) {
            characterSheet.setFinesseMarked(request.getFinesseMarked());
        }
        if (request.getInstinctModifier() != null) {
            characterSheet.setInstinctModifier(request.getInstinctModifier());
        }
        if (request.getInstinctMarked() != null) {
            characterSheet.setInstinctMarked(request.getInstinctMarked());
        }
        if (request.getPresenceModifier() != null) {
            characterSheet.setPresenceModifier(request.getPresenceModifier());
        }
        if (request.getPresenceMarked() != null) {
            characterSheet.setPresenceMarked(request.getPresenceMarked());
        }
        if (request.getKnowledgeModifier() != null) {
            characterSheet.setKnowledgeModifier(request.getKnowledgeModifier());
        }
        if (request.getKnowledgeMarked() != null) {
            characterSheet.setKnowledgeMarked(request.getKnowledgeMarked());
        }

        // Update resources
        if (request.getHitPointMax() != null) {
            characterSheet.setHitPointMax(request.getHitPointMax());
            if (characterSheet.getHitPointMarked() > request.getHitPointMax()) {
                characterSheet.setHitPointMarked(request.getHitPointMax());
            }
        }
        if (request.getHitPointMarked() != null) {
            characterSheet.setHitPointMarked(request.getHitPointMarked());
        }
        if (request.getStressMax() != null) {
            characterSheet.setStressMax(request.getStressMax());
            if (characterSheet.getStressMarked() > request.getStressMax()) {
                characterSheet.setStressMarked(request.getStressMax());
            }
        }
        if (request.getStressMarked() != null) {
            characterSheet.setStressMarked(request.getStressMarked());
        }
        if (request.getHopeMax() != null) {
            characterSheet.setHopeMax(request.getHopeMax());
            if (characterSheet.getHopeMarked() > request.getHopeMax()) {
                characterSheet.setHopeMarked(request.getHopeMax());
            }
        }
        if (request.getHopeMarked() != null) {
            characterSheet.setHopeMarked(request.getHopeMarked());
        }

        // Update economy
        if (request.getGold() != null) {
            characterSheet.setGold(request.getGold());
        }

        // Update card collections (replace entire collection if provided)
        if (request.getCommunityCardIds() != null) {
            Set<CommunityCard> communityCards = new HashSet<>();
            for (Long cardId : request.getCommunityCardIds()) {
                CommunityCard card = communityCardRepository.findById(cardId)
                        .orElseThrow(() -> new EntityNotFoundException("CommunityCard not found with id: " + cardId));
                communityCards.add(card);
            }
            characterSheet.setCommunityCards(communityCards);
        }
        if (request.getAncestryCardIds() != null) {
            Set<AncestryCard> ancestryCards = new HashSet<>();
            for (Long cardId : request.getAncestryCardIds()) {
                AncestryCard card = ancestryCardRepository.findById(cardId)
                        .orElseThrow(() -> new EntityNotFoundException("AncestryCard not found with id: " + cardId));
                ancestryCards.add(card);
            }
            characterSheet.setAncestryCards(ancestryCards);
        }
        if (request.getSubclassCardIds() != null) {
            Set<SubclassCard> subclassCards = new HashSet<>();
            for (Long cardId : request.getSubclassCardIds()) {
                SubclassCard card = subclassCardRepository.findById(cardId)
                        .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + cardId));
                subclassCards.add(card);
            }
            characterSheet.setSubclassCards(subclassCards);
        }
        if (request.getEquippedDomainCardIds() != null || request.getVaultDomainCardIds() != null) {
            if (request.getEquippedDomainCardIds() == null || request.getVaultDomainCardIds() == null) {
                throw new IllegalArgumentException("Both equippedDomainCardIds and vaultDomainCardIds must be provided together");
            }

            // Validate no duplicate IDs within or across both lists
            List<Long> allIds = new ArrayList<>(request.getEquippedDomainCardIds());
            allIds.addAll(request.getVaultDomainCardIds());
            if (allIds.size() != new HashSet<>(allIds).size()) {
                throw new IllegalArgumentException("Duplicate domain card IDs are not allowed; each card can only be assigned once to a character sheet");
            }

            characterSheet.getCharacterSheetDomainCards().clear();
            // Flush to execute DELETEs before INSERTs, avoiding unique constraint violation
            characterSheetRepository.flush();
            for (Long cardId : request.getEquippedDomainCardIds()) {
                DomainCard card = domainCardRepository.findById(cardId)
                        .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + cardId));
                CharacterSheetDomainCard csdc = CharacterSheetDomainCard.builder()
                        .characterSheet(characterSheet)
                        .domainCard(card)
                        .equipped(true)
                        .build();
                characterSheet.getCharacterSheetDomainCards().add(csdc);
            }
            for (Long cardId : request.getVaultDomainCardIds()) {
                DomainCard card = domainCardRepository.findById(cardId)
                        .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + cardId));
                CharacterSheetDomainCard csdc = CharacterSheetDomainCard.builder()
                        .characterSheet(characterSheet)
                        .domainCard(card)
                        .equipped(false)
                        .build();
                characterSheet.getCharacterSheetDomainCards().add(csdc);
            }
        }

        // Update inventory collections (clear-flush-rebuild pattern, same as domain cards)
        if (request.getInventoryWeapons() != null) {
            characterSheet.getCharacterSheetWeapons().clear();
            characterSheetRepository.flush();
            Set<CharacterSheetWeapon> weapons = new HashSet<>();
            for (InventoryWeaponRequest req : request.getInventoryWeapons()) {
                Weapon weapon = weaponRepository.findById(req.getWeaponId())
                        .orElseThrow(() -> new EntityNotFoundException("Weapon not found with id: " + req.getWeaponId()));
                weapons.add(CharacterSheetWeapon.builder()
                        .characterSheet(characterSheet)
                        .weapon(weapon)
                        .equipped(req.getEquipped() != null ? req.getEquipped() : false)
                        .slot(req.getSlot())
                        .build());
            }
            characterSheet.getCharacterSheetWeapons().addAll(weapons);
            validateWeaponSlots(characterSheet.getCharacterSheetWeapons());
        }
        if (request.getInventoryArmors() != null) {
            characterSheet.getCharacterSheetArmors().clear();
            characterSheetRepository.flush();
            Set<CharacterSheetArmor> armors = new HashSet<>();
            for (InventoryArmorRequest req : request.getInventoryArmors()) {
                Armor armor = armorRepository.findById(req.getArmorId())
                        .orElseThrow(() -> new EntityNotFoundException("Armor not found with id: " + req.getArmorId()));
                armors.add(CharacterSheetArmor.builder()
                        .characterSheet(characterSheet)
                        .armor(armor)
                        .equipped(req.getEquipped() != null ? req.getEquipped() : false)
                        .build());
            }
            characterSheet.getCharacterSheetArmors().addAll(armors);
        }
        if (request.getInventoryItems() != null) {
            characterSheet.getCharacterSheetLoot().clear();
            characterSheetRepository.flush();
            Set<CharacterSheetLoot> items = new HashSet<>();
            for (InventoryLootRequest req : request.getInventoryItems()) {
                Loot loot = lootRepository.findById(req.getLootId())
                        .orElseThrow(() -> new EntityNotFoundException("Loot not found with id: " + req.getLootId()));
                items.add(CharacterSheetLoot.builder()
                        .characterSheet(characterSheet)
                        .loot(loot)
                        .build());
            }
            characterSheet.getCharacterSheetLoot().addAll(items);
        }

        // Validate constraints after all updates
        validateConstraints(characterSheet);

        CharacterSheet updatedSheet = characterSheetRepository.save(characterSheet);
        auditLogger.log(AuditAction.CHARACTER_UPDATED, AuditContext.forUser(auth).build(),
                "\"" + updatedSheet.getName() + "\" (character_sheet_id: " + updatedSheet.getId() + ")");

        return toResponse(updatedSheet, Set.of());
    }

    /**
     * Deletes a character sheet (soft delete).
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can delete a character sheet. This is a soft deletion that preserves
     * the data but marks it as deleted.
     * </p>
     *
     * @param id The character sheet ID to delete
     * @param auth The authentication object containing the current user
     * @throws EntityNotFoundException if the character sheet is not found
     * @throws InsufficientPermissionsException if the user lacks permission to delete
     */
    @Transactional
    public void deleteCharacterSheet(Long id, Authentication auth) {
        CharacterSheet characterSheet = characterSheetRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + id));

        // Validate access - must be owner or moderator+
        validateAccess(characterSheet, auth, "delete");

        // Remove from any campaigns before soft deleting
        List<Campaign> campaigns = campaignRepository.findActiveByCampaignCharacterSheetId(id);
        if (!campaigns.isEmpty()) {
            log.info("Removing character_sheet_id: {} from {} campaign(s) before soft delete", id, campaigns.size());
            for (Campaign campaign : campaigns) {
                campaign.getPendingCharacterSheets().removeIf(cs -> cs.getId().equals(id));
                campaign.getPlayerCharacters().removeIf(cs -> cs.getId().equals(id));
                campaign.getNonPlayerCharacters().removeIf(cs -> cs.getId().equals(id));
            }
            campaignRepository.saveAll(campaigns);
        }

        // Soft delete the character sheet
        characterSheet.softDelete();
        characterSheetRepository.save(characterSheet);

        auditLogger.log(AuditAction.CHARACTER_DELETED, AuditContext.forUser(auth).build(),
                "\"" + characterSheet.getName() + "\" (character_sheet_id: " + id + ")");
    }

    /**
     * Validates that the current user has access to modify the character sheet.
     * <p>
     * Access is granted if the user is the character sheet owner OR has a
     * MODERATOR/ADMIN/OWNER role.
     * </p>
     *
     * @param characterSheet The character sheet to validate access for
     * @param auth The authentication object containing the current user
     * @param operation The operation being performed (for error message)
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    void validateAccess(CharacterSheet characterSheet, Authentication auth, String operation) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        Long ownerId = characterSheet.getOwner().getId();
        boolean isOwner = ownerId.equals(userId);
        boolean isModerator = roleHierarchyService.hasModeratorOrHigher(userDetails);

        if (!isOwner && !isModerator) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to " + operation + " this character sheet");
        }
    }

    /**
     * Validates character sheet constraints.
     * <p>
     * Validates the following rules:
     * - severeDamageThreshold >= majorDamageThreshold
     * </p>
     * <p>
     * Note: {@code marked <= max} is intentionally NOT enforced here. Equipped items
     * and features can raise a character's effective resource caps above the stored
     * (base) {@code *_max}, so a legitimate marked value may exceed the base max.
     * Clamping only occurs in {@link #updateCharacterSheet} when the caller explicitly
     * reduces a {@code *_max} field below its current marked value.
     * </p>
     *
     * @param sheet The character sheet to validate
     * @throws IllegalStateException if any constraint is violated
     */
    private void validateConstraints(CharacterSheet sheet) {
        if (sheet.getSevereDamageThreshold() < sheet.getMajorDamageThreshold()) {
            throw new IllegalStateException(
                    "Severe damage threshold (" + sheet.getSevereDamageThreshold() +
                    ") must be greater than or equal to major damage threshold (" + sheet.getMajorDamageThreshold() + ")");
        }
    }

    /**
     * Validates weapon slot assignments for a character sheet.
     * <p>
     * Enforces the following rules:
     * - At most one PRIMARY and one SECONDARY weapon slot
     * - Equipped weapons must have a slot (PRIMARY or SECONDARY)
     * - Unequipped weapons must not have a slot
     * - Slot value must be PRIMARY or SECONDARY
     * </p>
     *
     * @param weapons The set of character sheet weapons to validate
     * @throws IllegalStateException if any slot constraint is violated
     */
    private void validateWeaponSlots(Set<CharacterSheetWeapon> weapons) {
        long primaryCount = weapons.stream()
                .filter(w -> "PRIMARY".equals(w.getSlot()))
                .count();
        long secondaryCount = weapons.stream()
                .filter(w -> "SECONDARY".equals(w.getSlot()))
                .count();

        if (primaryCount > 1) {
            throw new IllegalStateException("Only one PRIMARY weapon slot is allowed");
        }
        if (secondaryCount > 1) {
            throw new IllegalStateException("Only one SECONDARY weapon slot is allowed");
        }

        for (CharacterSheetWeapon w : weapons) {
            if (Boolean.TRUE.equals(w.getEquipped()) && w.getSlot() == null) {
                throw new IllegalStateException("Equipped weapons must have a slot (PRIMARY or SECONDARY)");
            }
            if (!Boolean.TRUE.equals(w.getEquipped()) && w.getSlot() != null) {
                throw new IllegalStateException("Unequipped weapons must not have a slot");
            }
            if (w.getSlot() != null && !"PRIMARY".equals(w.getSlot()) && !"SECONDARY".equals(w.getSlot())) {
                throw new IllegalStateException("Weapon slot must be PRIMARY or SECONDARY");
            }
        }
    }

    /**
     * Converts a CharacterSheet entity to CharacterSheetResponse DTO.
     * <p>
     * Always includes IDs for relationships. Optionally expands full relationship
     * objects based on the expand set.
     * </p>
     * <p>
     * Supported expansions:
     * - owner: Full user object for the character owner
     * - experiences: List of experience objects
     * - communityCards: List of community card objects
     * - ancestryCards: List of ancestry card objects
     * - subclassCards: List of subclass card objects
     * - domainCards: List of domain card objects
     * - inventoryWeapons: Full weapon objects nested in inventory weapon entries
     * - inventoryArmors: Full armor objects nested in inventory armor entries
     * - inventoryItems: Full loot objects nested in inventory loot entries
     * - features: Full feature objects within weapons, armor, cards, and loot items
     * - costTags: Full cost tag objects within features and cards
     * - modifiers: Full feature modifier objects within features
     * - expansion: Full expansion objects within weapons, armor, cards, and loot items
     * </p>
     *
     * @param sheet The character sheet entity
     * @param expand Set of relationships to expand
     * @return CharacterSheetResponse DTO
     */
    CharacterSheetResponse toResponse(CharacterSheet sheet, Set<String> expand) {
        CharacterSheetResponse.CharacterSheetResponseBuilder builder = CharacterSheetResponse.builder()
                .id(sheet.getId())
                .name(sheet.getName())
                .pronouns(sheet.getPronouns())
                .level(sheet.getLevel())
                .proficiency(sheet.getProficiency())
                .evasion(sheet.getEvasion())
                .armorMax(sheet.getArmorMax())
                .armorMarked(sheet.getArmorMarked())
                .majorDamageThreshold(sheet.getMajorDamageThreshold())
                .severeDamageThreshold(sheet.getSevereDamageThreshold())
                .agilityModifier(sheet.getAgilityModifier())
                .agilityMarked(sheet.getAgilityMarked())
                .strengthModifier(sheet.getStrengthModifier())
                .strengthMarked(sheet.getStrengthMarked())
                .finesseModifier(sheet.getFinesseModifier())
                .finesseMarked(sheet.getFinesseMarked())
                .instinctModifier(sheet.getInstinctModifier())
                .instinctMarked(sheet.getInstinctMarked())
                .presenceModifier(sheet.getPresenceModifier())
                .presenceMarked(sheet.getPresenceMarked())
                .knowledgeModifier(sheet.getKnowledgeModifier())
                .knowledgeMarked(sheet.getKnowledgeMarked())
                .hitPointMax(sheet.getHitPointMax())
                .hitPointMarked(sheet.getHitPointMarked())
                .stressMax(sheet.getStressMax())
                .stressMarked(sheet.getStressMarked())
                .hopeMax(sheet.getHopeMax())
                .hopeMarked(sheet.getHopeMarked())
                .gold(sheet.getGold())
                .ownerId(sheet.getOwner().getId())
                .ownerName(sheet.getOwner().getUsername())
                .createdAt(sheet.getCreatedAt())
                .lastModifiedAt(sheet.getLastModifiedAt())
                .deletedAt(sheet.getDeletedAt());

        // Always include IDs for card collections
        builder.communityCardIds(sheet.getCommunityCards().stream().map(card -> card.getId()).collect(Collectors.toList()));
        builder.ancestryCardIds(sheet.getAncestryCards().stream().map(card -> card.getId()).collect(Collectors.toList()));
        builder.subclassCardIds(sheet.getSubclassCards().stream().map(card -> card.getId()).collect(Collectors.toList()));

        // Always include class info derived from subclass cards
        Class characterClass = sheet.getSubclassCards().stream()
                .map(SubclassCard::getSubclassPath)
                .filter(path -> path != null)
                .map(SubclassPath::getAssociatedClass)
                .filter(c -> c != null)
                .findFirst()
                .orElse(null);
        if (characterClass != null) {
            builder.classId(characterClass.getId());
            builder.className(characterClass.getName());
        }

        // Domain card IDs split by equipped/vault
        List<Long> equippedDomainCardIds = sheet.getCharacterSheetDomainCards().stream()
                .filter(CharacterSheetDomainCard::getEquipped)
                .map(csdc -> csdc.getDomainCard().getId())
                .collect(Collectors.toList());
        List<Long> vaultDomainCardIds = sheet.getCharacterSheetDomainCards().stream()
                .filter(csdc -> !csdc.getEquipped())
                .map(csdc -> csdc.getDomainCard().getId())
                .collect(Collectors.toList());
        List<Long> allDomainCardIds = new ArrayList<>(equippedDomainCardIds);
        allDomainCardIds.addAll(vaultDomainCardIds);

        builder.domainCardIds(allDomainCardIds);
        builder.equippedDomainCardIds(equippedDomainCardIds);
        builder.vaultDomainCardIds(vaultDomainCardIds);

        // Always include inventory linking entity responses
        builder.inventoryWeapons(sheet.getCharacterSheetWeapons().stream()
                .map(csw -> {
                    InventoryWeaponResponse.InventoryWeaponResponseBuilder iwb = InventoryWeaponResponse.builder()
                            .id(csw.getId())
                            .weaponId(csw.getWeapon().getId())
                            .equipped(csw.getEquipped())
                            .slot(csw.getSlot());
                    if (ExpandUtil.shouldExpand(expand, "inventoryWeapons")) {
                        iwb.weapon(toWeaponResponse(csw.getWeapon(), expand));
                    }
                    return iwb.build();
                })
                .collect(Collectors.toList()));

        builder.inventoryArmors(sheet.getCharacterSheetArmors().stream()
                .map(csa -> {
                    InventoryArmorResponse.InventoryArmorResponseBuilder iab = InventoryArmorResponse.builder()
                            .id(csa.getId())
                            .armorId(csa.getArmor().getId())
                            .equipped(csa.getEquipped());
                    if (ExpandUtil.shouldExpand(expand, "inventoryArmors")) {
                        iab.armor(toArmorResponse(csa.getArmor(), expand));
                    }
                    return iab.build();
                })
                .collect(Collectors.toList()));

        builder.inventoryItems(sheet.getCharacterSheetLoot().stream()
                .map(csl -> {
                    InventoryLootResponse.InventoryLootResponseBuilder ilb = InventoryLootResponse.builder()
                            .id(csl.getId())
                            .lootId(csl.getLoot().getId());
                    if (ExpandUtil.shouldExpand(expand, "inventoryItems")) {
                        ilb.loot(toLootResponse(csl.getLoot(), expand));
                    }
                    return ilb.build();
                })
                .collect(Collectors.toList()));

        // Always include IDs for experiences
        builder.experienceIds(sheet.getExperiences().stream().map(Experience::getId).collect(Collectors.toList()));

        // Expand owner if requested
        if (ExpandUtil.shouldExpand(expand, "owner")) {
            User owner = sheet.getOwner();
            builder.owner(UserResponse.builder()
                    .id(owner.getId())
                    .username(owner.getUsername())
                    .email(owner.getEmail())
                    .avatarUrl(owner.getAvatarUrl())
                    .timezone(owner.getTimezone())
                    .createdAt(owner.getCreatedAt())
                    .lastModifiedAt(owner.getLastModifiedAt())
                    .build());
        }

        // Expand experiences if requested
        if (ExpandUtil.shouldExpand(expand, "experiences")) {
            List<ExperienceResponse> experiences = sheet.getExperiences().stream()
                    .map(exp -> ExperienceResponse.builder()
                            .id(exp.getId())
                            .characterSheetId(exp.getCharacterSheet().getId())
                            .createdById(exp.getCreatedBy().getId())
                            .description(exp.getDescription())
                            .modifier(exp.getModifier())
                            .createdAt(exp.getCreatedAt())
                            .lastModifiedAt(exp.getLastModifiedAt())
                            .build())
                    .collect(Collectors.toList());
            builder.experiences(experiences);
        }

        // Expand community cards if requested
        if (ExpandUtil.shouldExpand(expand, "communityCards")) {
            builder.communityCards(sheet.getCommunityCards().stream()
                    .map(card -> toCommunityCardResponse(card, expand))
                    .collect(Collectors.toList()));
        }

        // Expand ancestry cards if requested
        if (ExpandUtil.shouldExpand(expand, "ancestryCards")) {
            builder.ancestryCards(sheet.getAncestryCards().stream()
                    .map(card -> toAncestryCardResponse(card, expand))
                    .collect(Collectors.toList()));
        }

        // Expand subclass cards if requested
        if (ExpandUtil.shouldExpand(expand, "subclassCards")) {
            builder.subclassCards(sheet.getSubclassCards().stream()
                    .map(card -> toSubclassCardResponse(card, expand))
                    .collect(Collectors.toList()));
        }

        // Expand class if requested
        if (ExpandUtil.shouldExpand(expand, "class") && characterClass != null) {
            builder.classObject(classService.toResponse(characterClass, expand));
        }

        // Expand domain cards if requested
        if (ExpandUtil.shouldExpand(expand, "domainCards")) {
            builder.domainCards(sheet.getCharacterSheetDomainCards().stream()
                    .map(csdc -> toDomainCardResponse(csdc.getDomainCard(), expand))
                    .collect(Collectors.toList()));
        }

        // Expand equipped domain cards if requested
        if (ExpandUtil.shouldExpand(expand, "equippedDomainCards")) {
            builder.equippedDomainCards(sheet.getCharacterSheetDomainCards().stream()
                    .filter(CharacterSheetDomainCard::getEquipped)
                    .map(csdc -> toDomainCardResponse(csdc.getDomainCard(), expand))
                    .collect(Collectors.toList()));
        }

        // Expand vault domain cards if requested
        if (ExpandUtil.shouldExpand(expand, "vaultDomainCards")) {
            builder.vaultDomainCards(sheet.getCharacterSheetDomainCards().stream()
                    .filter(csdc -> !csdc.getEquipped())
                    .map(csdc -> toDomainCardResponse(csdc.getDomainCard(), expand))
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    /**
     * Populates campaign information on a character sheet response if the viewer has access.
     * <p>
     * Looks up active campaigns containing this character sheet. If found, checks whether
     * the viewer is involved in the campaign or has moderator+ privileges.
     * </p>
     *
     * @param response The character sheet response to populate
     * @param characterSheetId The character sheet ID
     * @param auth The authentication object
     */
    private void populateCampaignInfo(CharacterSheetResponse response, Long characterSheetId, Authentication auth) {
        List<Campaign> campaigns = campaignRepository.findActiveByCampaignCharacterSheetId(characterSheetId);
        if (campaigns.isEmpty()) {
            return;
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();
        boolean isModerator = roleHierarchyService.hasModeratorOrHigher(userDetails);

        for (Campaign campaign : campaigns) {
            if (campaign.isInvolved(userId) || isModerator) {
                response.setCampaignId(campaign.getId());
                response.setCampaignName(campaign.getName());
                return;
            }
        }
    }

    /**
     * Converts a Weapon entity to WeaponResponse DTO, delegating to WeaponService for expand support.
     *
     * @param weapon The weapon entity
     * @param expand Set of relationships to expand
     * @return WeaponResponse DTO
     */
    private WeaponResponse toWeaponResponse(Weapon weapon, Set<String> expand) {
        return weaponService.toResponse(weapon, expand);
    }

    /**
     * Converts an Armor entity to ArmorResponse DTO, delegating to ArmorService for expand support.
     *
     * @param armor The armor entity
     * @param expand Set of relationships to expand
     * @return ArmorResponse DTO
     */
    private ArmorResponse toArmorResponse(Armor armor, Set<String> expand) {
        return armorService.toResponse(armor, expand);
    }

    /**
     * Converts a CommunityCard entity to CommunityCardResponse DTO, delegating to CommunityCardService for expand support.
     *
     * @param card The community card entity
     * @param expand Set of relationships to expand
     * @return CommunityCardResponse DTO
     */
    private CommunityCardResponse toCommunityCardResponse(CommunityCard card, Set<String> expand) {
        return communityCardService.toResponse(card, expand);
    }

    /**
     * Converts an AncestryCard entity to AncestryCardResponse DTO, delegating to AncestryCardService for expand support.
     *
     * @param card The ancestry card entity
     * @param expand Set of relationships to expand
     * @return AncestryCardResponse DTO
     */
    private AncestryCardResponse toAncestryCardResponse(AncestryCard card, Set<String> expand) {
        return ancestryCardService.toResponse(card, expand);
    }

    /**
     * Converts a SubclassCard entity to SubclassCardResponse DTO, delegating to SubclassCardService for expand support.
     *
     * @param card The subclass card entity
     * @param expand Set of relationships to expand
     * @return SubclassCardResponse DTO
     */
    private SubclassCardResponse toSubclassCardResponse(SubclassCard card, Set<String> expand) {
        return subclassCardService.toResponse(card, expand);
    }

    /**
     * Converts a DomainCard entity to DomainCardResponse DTO, delegating to DomainCardService for expand support.
     *
     * @param card The domain card entity
     * @param expand Set of relationships to expand
     * @return DomainCardResponse DTO
     */
    private DomainCardResponse toDomainCardResponse(DomainCard card, Set<String> expand) {
        return domainCardService.toResponse(card, expand);
    }

    /**
     * Converts a Loot entity to LootResponse DTO, delegating to LootService for expand support.
     *
     * @param loot The loot entity
     * @param expand Set of relationships to expand
     * @return LootResponse DTO
     */
    private LootResponse toLootResponse(Loot loot, Set<String> expand) {
        return lootService.toResponse(loot, expand);
    }
}

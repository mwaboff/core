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
import com.aboff.core.util.MarkdownSanitizerUtil;
import com.aboff.core.model.dto.response.PagedResponse;
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
import com.aboff.core.service.UserService;
import com.aboff.core.util.ExpandUtil;
import com.aboff.core.model.enums.AdvancementType;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final TransformationCardRepository transformationCardRepository;
    private final MartialStanceRepository martialStanceRepository;
    private final TransformationCardService transformationCardService;
    private final MartialStanceService martialStanceService;
    private final CompanionRepository companionRepository;
    private final CompanionService companionService;
    private final UserService userService;
    private final CharacterAdvancementLogRepository characterAdvancementLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Maximum value for Vampire "Feed" tokens ("You can hold up to 6 tokens at a time").
     * <p>
     * This is unrelated to martial stance tiers, which cap at 4 and are validated separately in
     * {@code validateMartialStanceConstraints} against the character's own tier.
     * </p>
     */
    private static final int TRANSFORMATION_TOKENS_MAX = 6;

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
                        .map(sheet -> toResponse(sheet, expandSet, auth))
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
        CharacterSheetResponse response = toResponse(characterSheet, expandSet, auth);

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

        return toResponse(savedSheet, Set.of(), auth);
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

        // Update Hope & Fear resources
        updateHopeAndFearResources(characterSheet, request);

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

        return toResponse(updatedSheet, Set.of(), auth);
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
     * Retrieves the notes for a character sheet without loading the full entity.
     * <p>
     * Notes are a private field: the caller must be the character sheet owner or hold
     * MODERATOR/ADMIN/OWNER role, the same rule enforced by {@link #updateNotes}. Soft-deleted
     * sheets return 404.
     * </p>
     *
     * @param id   the character sheet ID
     * @param auth the authentication context used for ownership and role checks
     * @return a slim response containing the sheet ID, current notes, and last-modified timestamp
     * @throws EntityNotFoundException          if the character sheet is not found or is soft-deleted
     * @throws InsufficientPermissionsException if the caller is neither the owner nor a MODERATOR+
     */
    @Transactional(readOnly = true)
    public CharacterSheetNotesResponse getNotes(Long id, Authentication auth) {
        CharacterSheet sheet = characterSheetRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + id));
        validateAccess(sheet, auth, "view notes for");
        return CharacterSheetNotesResponse.builder()
                .id(sheet.getId())
                .notes(sheet.getNotes())
                .lastModifiedAt(sheet.getLastModifiedAt())
                .build();
    }

    /**
     * Updates the notes field on a character sheet.
     * <p>
     * The caller must be the character sheet owner or have MODERATOR/ADMIN/OWNER role.
     * The raw notes string is sanitized via {@link MarkdownSanitizerUtil#sanitize(String)}
     * before being persisted to strip XSS vectors and dangerous URI schemes.
     * An empty string is accepted and clears any existing notes.
     * </p>
     *
     * @param id       the character sheet ID
     * @param rawNotes the unsanitized notes content supplied by the client; must not be null
     * @param auth     the authentication context used for ownership and role checks
     * @return the full updated character sheet response
     * @throws EntityNotFoundException          if the character sheet is not found or is soft-deleted
     * @throws InsufficientPermissionsException if the caller is neither the owner nor a MODERATOR+
     */
    @Transactional
    public CharacterSheetResponse updateNotes(Long id, String rawNotes, Authentication auth) {
        CharacterSheet sheet = characterSheetRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + id));
        validateAccess(sheet, auth, "update notes");
        log.info("Updating notes for character sheet id={}", id);
        sheet.setNotes(MarkdownSanitizerUtil.sanitize(rawNotes));
        return toResponse(characterSheetRepository.save(sheet), Set.of(), auth);
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
        if (!hasOwnerOrModeratorAccess(characterSheet, auth)) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to " + operation + " this character sheet");
        }
    }

    /**
     * Determines, without throwing, whether the current user is the character sheet's owner or
     * holds MODERATOR/ADMIN/OWNER role.
     * <p>
     * Backs both {@link #validateAccess} (which throws) and the private-field gating in
     * {@link #toResponse}, so notes visibility is defined in exactly one place. Fails closed for
     * an absent or unrecognized principal, e.g. the unauthenticated overload of
     * {@link #getCharacterSheetById(Long, String)}.
     * </p>
     *
     * @param characterSheet The character sheet to check access against
     * @param auth The authentication object containing the current user, may be null
     * @return true if the user owns the sheet or is MODERATOR/ADMIN/OWNER
     */
    private boolean hasOwnerOrModeratorAccess(CharacterSheet characterSheet, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }

        Long ownerId = characterSheet.getOwner().getId();
        boolean isOwner = ownerId.equals(userDetails.getUserId());
        boolean isModerator = roleHierarchyService.hasModeratorOrHigher(userDetails);
        return isOwner || isModerator;
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
        validateMartialStanceConstraints(sheet);
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
     * Applies partial updates for the Hope &amp; Fear resources (Focus, Favor, transformation
     * state, and known/active martial stances) on a character sheet.
     * <p>
     * Follows the same clamp-on-max-change convention used for hit points/stress/hope
     * ({@link #updateCharacterSheet}): lowering {@code focusMax} clamps {@code focusMarked} down
     * with it. Unlike those resources, Focus itself is also actively clamped to
     * {@code 0..focusMax} whenever it is set directly, and Vampire "Feed" tokens are clamped to
     * {@code 0..6} — see the design doc for why Focus and transformation tokens differ from the
     * "marked may legitimately exceed max" rule that governs HP/Stress/Hope.
     * </p>
     *
     * @param sheet   the character sheet being updated
     * @param request the partial update request
     * @throws EntityNotFoundException if a referenced transformation card or martial stance is not found
     * @throws IllegalStateException if the request mutates transformation state while the sheet is
     *                               not transformation-enabled (see {@link #validateTransformationAccess})
     */
    private void updateHopeAndFearResources(CharacterSheet sheet, UpdateCharacterSheetRequest request) {
        if (request.getFocusMax() != null) {
            sheet.setFocusMax(request.getFocusMax());
            if (sheet.getFocusMarked() > request.getFocusMax()) {
                sheet.setFocusMarked(request.getFocusMax());
            }
        }
        if (request.getFocusMarked() != null) {
            sheet.setFocusMarked(clamp(request.getFocusMarked(), 0, sheet.getFocusMax()));
        }
        if (request.getFavor() != null) {
            sheet.setFavor(request.getFavor());
        }

        validateTransformationAccess(sheet, request);

        // Transformation attachment: an explicit clear flag detaches the transformation card and
        // resets every piece of state that only makes sense while a transformation is attached.
        if (Boolean.TRUE.equals(request.getClearTransformationCard())) {
            sheet.setTransformationCard(null);
            sheet.setTransformationTokens(null);
            sheet.setWolfFormActive(false);
        } else if (request.getTransformationCardId() != null) {
            TransformationCard card = transformationCardRepository.findByIdAndDeletedAtIsNull(request.getTransformationCardId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "TransformationCard not found with id: " + request.getTransformationCardId()));
            sheet.setTransformationCard(card);
        }
        if (request.getTransformationTokens() != null) {
            sheet.setTransformationTokens(clamp(request.getTransformationTokens(), 0, TRANSFORMATION_TOKENS_MAX));
        }
        if (request.getWolfFormActive() != null) {
            sheet.setWolfFormActive(request.getWolfFormActive());
        }

        if (request.getKnownMartialStanceIds() != null) {
            Set<MartialStance> knownStances = new HashSet<>(
                    martialStanceRepository.findAllByIdInAndDeletedAtIsNull(request.getKnownMartialStanceIds()));
            sheet.setKnownMartialStances(knownStances);
        }

        if (Boolean.TRUE.equals(request.getClearActiveMartialStance())) {
            sheet.setActiveMartialStance(null);
        } else if (request.getActiveMartialStanceId() != null) {
            MartialStance stance = martialStanceRepository.findByIdAndDeletedAtIsNull(request.getActiveMartialStanceId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "MartialStance not found with id: " + request.getActiveMartialStanceId()));
            sheet.setActiveMartialStance(stance);
        }
    }

    /**
     * Rejects transformation mutations on a character whose Game Master has not enabled
     * transformations.
     * <p>
     * Transformations are granted by a GM through the campaign transformation endpoint, so the
     * player-facing update path must not be able to attach, detach, or operate a transformation
     * on a sheet that is not transformation-enabled. Without this check the flag would be purely
     * cosmetic, because {@code UpdateCharacterSheetRequest} deliberately has no
     * {@code transformationEnabled} field for a player to set.
     * </p>
     * <p>
     * The gate is on <em>change</em>, not on mention: a request that merely restates a value the
     * sheet already holds (most commonly {@code wolfFormActive: false} on a character that has
     * never transformed) changes nothing, so it is allowed through. A partial-update body that
     * carries a whole group of resources -- a rest, for one -- would otherwise be rejected in
     * full over a field it never moved. {@code clearTransformationCard} stays strict either way:
     * it is an explicit command flag, never an incidental restatement of current state.
     * </p>
     *
     * @param sheet   the character sheet being updated
     * @param request the partial update request
     * @throws IllegalStateException if the request would change transformation state while the
     *                               sheet is not transformation-enabled
     */
    private void validateTransformationAccess(CharacterSheet sheet, UpdateCharacterSheetRequest request) {
        if (sheet.isTransformationEnabled()) {
            return;
        }

        Long currentCardId = sheet.getTransformationCard() == null ? null : sheet.getTransformationCard().getId();
        boolean changesCard = request.getTransformationCardId() != null
                && !request.getTransformationCardId().equals(currentCardId);
        boolean changesTokens = request.getTransformationTokens() != null
                && !request.getTransformationTokens().equals(sheet.getTransformationTokens());
        boolean changesWolfForm = request.getWolfFormActive() != null
                && request.getWolfFormActive() != Boolean.TRUE.equals(sheet.getWolfFormActive());

        if (Boolean.TRUE.equals(request.getClearTransformationCard())
                || changesCard || changesTokens || changesWolfForm) {
            throw new IllegalStateException(
                    "Transformations are not enabled for this character. Ask your GM to enable them.");
        }
    }

    /**
     * Clamps an integer value to the inclusive range {@code [min, max]}.
     *
     * @param value the value to clamp
     * @param min   the inclusive lower bound
     * @param max   the inclusive upper bound
     * @return the clamped value
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * Validates the martial stance invariants on a character sheet:
     * <ul>
     *   <li>The active stance, if any, must be a member of the character's known stances.</li>
     *   <li>Every known stance's tier must be at or below the character's current tier.</li>
     * </ul>
     *
     * @param sheet The character sheet to validate
     * @throws IllegalStateException if either invariant is violated
     */
    private void validateMartialStanceConstraints(CharacterSheet sheet) {
        MartialStance activeStance = sheet.getActiveMartialStance();
        Set<MartialStance> knownStances = sheet.getKnownMartialStances();

        if (activeStance != null && (knownStances == null || !knownStances.contains(activeStance))) {
            throw new IllegalStateException(
                    "Active martial stance (id: " + activeStance.getId() + ") must be one of the character's known stances");
        }

        if (knownStances != null && !knownStances.isEmpty()) {
            int characterTier = getTierForLevel(sheet.getLevel());
            for (MartialStance stance : knownStances) {
                if (stance.getTier() != null && stance.getTier() > characterTier) {
                    throw new IllegalStateException(
                            "Martial stance '" + stance.getName() + "' (tier " + stance.getTier() +
                            ") exceeds the character's current tier (" + characterTier + ")");
                }
            }
        }
    }

    /**
     * Determines the tier for a given character level.
     * <p>
     * Mirrors {@link LevelUpService#getTierForLevel(int)}. Duplicated rather than shared to avoid
     * a circular service dependency (LevelUpService already depends on CharacterSheetService).
     * </p>
     *
     * @param level the character level (1-10)
     * @return the tier (1-4)
     */
    private int getTierForLevel(int level) {
        if (level <= 1) return 1;
        if (level <= 4) return 2;
        if (level <= 7) return 3;
        return 4;
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
     * - transformationCard: Full transformation card object
     * - knownMartialStances: Full martial stance objects for every known stance
     * - activeMartialStance: Full martial stance object for the currently active stance
     * </p>
     * <p>
     * {@code notes} is a private field, not an expansion: it is populated only when {@code auth}
     * identifies the sheet owner or a MODERATOR+ viewer (see {@link #hasOwnerOrModeratorAccess}),
     * and otherwise left {@code null} so it is dropped from the serialized JSON.
     * </p>
     *
     * @param sheet The character sheet entity
     * @param expand Set of relationships to expand
     * @return CharacterSheetResponse DTO, with {@code notes} omitted for the unauthenticated caller
     */
    CharacterSheetResponse toResponse(CharacterSheet sheet, Set<String> expand) {
        return toResponse(sheet, expand, null);
    }

    /**
     * Converts a CharacterSheet entity to CharacterSheetResponse DTO for a specific viewer.
     * <p>
     * Identical to {@link #toResponse(CharacterSheet, Set)}, except {@code notes} and the
     * {@code owner} expansion are resolved against {@code auth} rather than defaulting to the
     * fully-redacted, unauthenticated view.
     * </p>
     *
     * @param sheet  The character sheet entity
     * @param expand Set of relationships to expand
     * @param auth   The authentication object for the requesting viewer, or null to omit
     *               {@code notes} and redact the {@code owner} expansion
     * @return CharacterSheetResponse DTO
     */
    CharacterSheetResponse toResponse(CharacterSheet sheet, Set<String> expand, Authentication auth) {
        CharacterSheetResponse.CharacterSheetResponseBuilder builder = CharacterSheetResponse.builder()
                .id(sheet.getId())
                .name(sheet.getName())
                .pronouns(sheet.getPronouns())
                // Presence of this field is the client's authorization signal: an authorized
                // viewer always receives it, as "" when nothing is written, so an owner with
                // empty notes still gets an editor. NON_NULL strips null but keeps "".
                .notes(hasOwnerOrModeratorAccess(sheet, auth)
                        ? (sheet.getNotes() != null ? sheet.getNotes() : "")
                        : null)
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
                .focusMarked(sheet.getFocusMarked())
                .focusMax(sheet.getFocusMax())
                .favor(sheet.getFavor())
                .comboDie(sheet.getComboDie())
                .transformationEnabled(sheet.isTransformationEnabled())
                .transformationTokens(sheet.getTransformationTokens())
                .wolfFormActive(sheet.getWolfFormActive())
                .ownerId(sheet.getOwner().getId())
                .ownerName(sheet.getOwner().getUsername())
                .createdAt(sheet.getCreatedAt())
                .lastModifiedAt(sheet.getLastModifiedAt())
                .deletedAt(sheet.getDeletedAt());

        // Transformation card (always include ID; expand for the full object)
        if (sheet.getTransformationCard() != null) {
            builder.transformationCardId(sheet.getTransformationCard().getId());
            if (ExpandUtil.shouldExpand(expand, "transformationCard")) {
                builder.transformationCard(transformationCardService.toResponse(sheet.getTransformationCard(), Set.of()));
            }
        }

        // Known martial stances (always include IDs; expand for the full objects)
        builder.knownMartialStanceIds(sheet.getKnownMartialStances().stream()
                .map(MartialStance::getId)
                .collect(Collectors.toList()));
        if (ExpandUtil.shouldExpand(expand, "knownMartialStances")) {
            builder.knownMartialStances(sheet.getKnownMartialStances().stream()
                    .map(stance -> martialStanceService.toResponse(stance, Set.of()))
                    .collect(Collectors.toList()));
        }

        // Active martial stance (always include ID; expand for the full object)
        if (sheet.getActiveMartialStance() != null) {
            builder.activeMartialStanceId(sheet.getActiveMartialStance().getId());
            if (ExpandUtil.shouldExpand(expand, "activeMartialStance")) {
                builder.activeMartialStance(martialStanceService.toResponse(sheet.getActiveMartialStance(), Set.of()));
            }
        }

        // Always include IDs for card collections
        builder.communityCardIds(sheet.getCommunityCards().stream().map(card -> card.getId()).collect(Collectors.toList()));
        builder.ancestryCardIds(sheet.getAncestryCards().stream().map(card -> card.getId()).collect(Collectors.toList()));
        builder.subclassCardIds(sheet.getSubclassCards().stream().map(card -> card.getId()).collect(Collectors.toList()));

        // Always include class info derived from subclass cards (a multiclassed character has more than one)
        List<Class> characterClasses = resolveCharacterClasses(sheet);
        builder.classIds(characterClasses.stream().map(Class::getId).collect(Collectors.toList()));
        builder.classNames(characterClasses.stream().map(Class::getName).collect(Collectors.toList()));
        if (!characterClasses.isEmpty()) {
            Class primaryClass = characterClasses.get(0);
            builder.classId(primaryClass.getId());
            builder.className(primaryClass.getName());
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

        // Expand owner if requested. Routed through UserService.mapToUserResponse so the same
        // email/avatarUrl/timezone redaction GET /api/users/{id} applies here too -- a non-self,
        // non-privileged viewer must not see another user's private profile fields just because
        // they know a character sheet ID.
        if (ExpandUtil.shouldExpand(expand, "owner")) {
            builder.owner(userService.mapToUserResponse(sheet.getOwner(), auth));
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

        // Expand classes if requested
        if (ExpandUtil.shouldExpand(expand, "class") && !characterClasses.isEmpty()) {
            List<ClassResponse> classResponses = characterClasses.stream()
                    .map(c -> classService.toResponse(c, expand))
                    .collect(Collectors.toList());
            builder.classes(classResponses);
            builder.classObject(classResponses.get(0));
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

        // Companions: gate flag and derived Hope slots are always included; active companions
        // are fetched once (soft-deleted ones are excluded by the repository query) since the
        // Hope slot count needs them regardless of whether the full list is expanded.
        List<Companion> activeCompanions = companionRepository.findActiveByCharacterSheetId(sheet.getId());
        builder.companionsEnabled(sheet.isCompanionsEnabled());
        builder.companionGrantedHopeSlots(CompanionDerivationService.companionGrantedHopeSlots(activeCompanions));
        if (ExpandUtil.shouldExpand(expand, "companions")) {
            builder.companions(activeCompanions.stream()
                    .map(companion -> companionService.toResponse(companion, expand))
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    /**
     * Resolves every class a character belongs to by walking all of their subclass cards.
     * <p>
     * A multiclassed character holds subclass cards from more than one class, and a single class can
     * contribute several cards (foundation plus specialization). Classes are therefore deduplicated by
     * ID and returned in acquisition order: the character's original class first, followed by each
     * multiclass in the order it was selected during level-ups.
     * </p>
     * <p>
     * Acquisition order is recovered from the character's advancement log, since the subclass cards
     * themselves are held in a {@link java.util.HashSet} and carry no ordering. Characters with fewer
     * than two classes skip the log lookup entirely. When the log is missing, incomplete, or
     * unreadable — legacy characters created before level-up logging, or level-ups that were undone —
     * the remaining classes fall back to class ID ascending (then class name) so repeated calls still
     * produce identical responses.
     * </p>
     *
     * @param sheet The character sheet to inspect
     * @return An ordered, deduplicated list of the character's classes; empty if none can be resolved
     */
    private List<Class> resolveCharacterClasses(CharacterSheet sheet) {
        Map<Long, Class> classesById = new LinkedHashMap<>();
        for (SubclassCard card : sheet.getSubclassCards()) {
            SubclassPath path = card.getSubclassPath();
            if (path == null || path.getAssociatedClass() == null) {
                continue;
            }
            Class associatedClass = path.getAssociatedClass();
            classesById.putIfAbsent(associatedClass.getId(), associatedClass);
        }

        List<Class> classes = classesById.values().stream()
                .sorted(Comparator.comparing(Class::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Class::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        if (classes.size() < 2) {
            return classes;
        }

        List<Long> multiclassOrder = resolveMulticlassClassIdOrder(sheet);
        if (multiclassOrder.isEmpty()) {
            return classes;
        }

        List<Class> acquisitionOrdered = classes.stream()
                .filter(characterClass -> !multiclassOrder.contains(characterClass.getId()))
                .collect(Collectors.toList());
        for (Long classId : multiclassOrder) {
            Class multiclass = classesById.get(classId);
            if (multiclass != null) {
                acquisitionOrdered.add(multiclass);
            }
        }
        return acquisitionOrdered;
    }

    /**
     * Reads the character's advancement log to recover the order in which multiclasses were selected.
     * <p>
     * Each log entry stores its level-up choices as a JSON blob; entries of type
     * {@link AdvancementType#MULTICLASS} name the subclass card that was gained, which resolves to a
     * class through the card's subclass path. Entries that cannot be parsed, or that name a subclass
     * card the character no longer holds (an undone level-up), are skipped rather than failing the
     * whole response.
     * </p>
     *
     * @param sheet The character sheet whose logs should be inspected
     * @return Class IDs gained through multiclassing, oldest selection first and deduplicated; empty
     *         if the character has no readable multiclass advancements
     */
    @SuppressWarnings("unchecked")
    private List<Long> resolveMulticlassClassIdOrder(CharacterSheet sheet) {
        Map<Long, Long> classIdBySubclassCardId = new HashMap<>();
        for (SubclassCard card : sheet.getSubclassCards()) {
            SubclassPath path = card.getSubclassPath();
            if (card.getId() == null || path == null || path.getAssociatedClass() == null) {
                continue;
            }
            classIdBySubclassCardId.put(card.getId(), path.getAssociatedClass().getId());
        }

        List<Long> multiclassOrder = new ArrayList<>();
        List<CharacterAdvancementLog> logs =
                characterAdvancementLogRepository.findByCharacterSheetIdOrderByToLevelAscIdAsc(sheet.getId());
        for (CharacterAdvancementLog logEntry : logs) {
            try {
                Map<String, Object> data = objectMapper.readValue(logEntry.getAdvancementData(), Map.class);
                List<Map<String, Object>> advancements = (List<Map<String, Object>>) data.get("advancements");
                if (advancements == null) {
                    continue;
                }
                for (Map<String, Object> advancement : advancements) {
                    if (!AdvancementType.MULTICLASS.name().equals(advancement.get("type"))) {
                        continue;
                    }
                    Long classId = classIdBySubclassCardId.get(toLong(advancement.get("subclassCardId")));
                    if (classId != null && !multiclassOrder.contains(classId)) {
                        multiclassOrder.add(classId);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse advancement data for log {} while ordering classes for sheet {}: {}",
                        logEntry.getId(), sheet.getId(), e.getMessage());
            }
        }
        return multiclassOrder;
    }

    /**
     * Safely converts a value read out of an advancement JSON blob to a Long.
     *
     * @param value The raw JSON value
     * @return The value as a Long, or null if it is absent or not numeric
     */
    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
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

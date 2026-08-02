package com.aboff.core.service.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.AdvancementChoice;
import com.aboff.core.model.dto.dh.request.DomainCardTradeRequest;
import com.aboff.core.model.dto.dh.request.LevelUpRequest;
import com.aboff.core.model.dto.dh.response.*;
import com.aboff.core.model.entity.dh.*;
import com.aboff.core.model.enums.AdvancementType;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.SubclassLevel;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.*;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing character level-up operations in the Daggerheart TTRPG system.
 * <p>
 * Handles the complex level-up workflow including advancement selection, tier transitions,
 * domain card management, and undo support via advancement logs.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LevelUpService {

    private static final int MAX_LEVEL = 10;
    private static final int MAX_EQUIPPED_DOMAIN_CARDS = 5;

    private final CharacterSheetRepository characterSheetRepository;
    private final CharacterSheetDomainCardRepository characterSheetDomainCardRepository;
    private final CharacterAdvancementLogRepository characterAdvancementLogRepository;
    private final ExperienceRepository experienceRepository;
    private final DomainCardRepository domainCardRepository;
    private final SubclassCardRepository subclassCardRepository;
    private final SubclassPathRepository subclassPathRepository;
    private final UserRepository userRepository;
    private final RoleHierarchyService roleHierarchyService;
    private final CharacterSheetService characterSheetService;
    private final AuditLogger auditLogger;
    private final ObjectMapper objectMapper;

    /**
     * Retrieves the available level-up options for a character sheet.
     *
     * @param characterSheetId the character sheet ID
     * @param auth the authentication object containing the current user
     * @return LevelUpOptionsResponse with available advancements and constraints
     * @throws EntityNotFoundException if the character sheet is not found
     * @throws IllegalStateException if the character is at max level
     */
    @Transactional(readOnly = true)
    public LevelUpOptionsResponse getLevelUpOptions(Long characterSheetId, Authentication auth) {
        CharacterSheet sheet = characterSheetRepository.findActiveById(characterSheetId)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + characterSheetId));

        characterSheetService.validateAccess(sheet, auth, "view level-up options");

        int currentLevel = sheet.getLevel();
        if (currentLevel >= MAX_LEVEL) {
            throw new IllegalStateException("Character is already at maximum level " + MAX_LEVEL);
        }

        int nextLevel = currentLevel + 1;
        int currentTier = getTierForLevel(currentLevel);
        int nextTier = getTierForLevel(nextLevel);
        boolean isTierTransition = nextTier > currentTier;

        List<CharacterAdvancementLog> tierLogs = characterAdvancementLogRepository
                .findByCharacterSheetIdAndTier(characterSheetId, nextTier);
        Map<AdvancementType, Integer> usageMap = buildUsageMap(tierLogs);

        List<AvailableAdvancement> availableAdvancements = buildAvailableAdvancements(nextTier, usageMap);

        long equippedCount = characterSheetDomainCardRepository.countEquippedByCharacterSheetId(characterSheetId);

        log.debug("Retrieved level-up options for character sheet {} (level {} -> {})", characterSheetId, currentLevel, nextLevel);

        return LevelUpOptionsResponse.builder()
                .currentLevel(currentLevel)
                .nextLevel(nextLevel)
                .currentTier(currentTier)
                .nextTier(nextTier)
                .isTierTransition(isTierTransition)
                .availableAdvancements(availableAdvancements)
                .domainCardLevelCap(getDomainCardLevelCap(nextTier))
                .accessibleDomainIds(getAccessibleDomainIds(sheet))
                .equippedDomainCardCount(equippedCount)
                .maxEquippedDomainCards(MAX_EQUIPPED_DOMAIN_CARDS)
                .build();
    }

    /**
     * Performs a level-up operation on a character sheet.
     *
     * @param characterSheetId the character sheet ID
     * @param request the level-up request containing advancement choices
     * @param auth the authentication object containing the current user
     * @return LevelUpResponse with the updated character sheet and applied changes
     * @throws EntityNotFoundException if the character sheet or referenced entities are not found
     * @throws IllegalStateException if validation fails or the character is at max level
     */
    @Transactional
    public LevelUpResponse levelUp(Long characterSheetId, LevelUpRequest request, Authentication auth) {
        CharacterSheet sheet = characterSheetRepository.findActiveById(characterSheetId)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + characterSheetId));

        characterSheetService.validateAccess(sheet, auth, "level up");

        int currentLevel = sheet.getLevel();
        if (currentLevel >= MAX_LEVEL) {
            throw new IllegalStateException("Character is already at maximum level " + MAX_LEVEL);
        }

        int nextLevel = currentLevel + 1;
        int currentTier = getTierForLevel(currentLevel);
        int nextTier = getTierForLevel(nextLevel);
        boolean isTierTransition = nextTier > currentTier;

        List<CharacterAdvancementLog> tierLogs = characterAdvancementLogRepository
                .findByCharacterSheetIdAndTier(characterSheetId, nextTier);
        Map<AdvancementType, Integer> usageMap = buildUsageMap(tierLogs);

        // Validate request
        validateLevelUpRequest(request, sheet, nextTier, usageMap, isTierTransition, nextLevel);

        List<String> appliedChanges = new ArrayList<>();
        Map<String, Object> advancementDataMap = new LinkedHashMap<>();

        // Snapshot previous values
        Map<String, Object> previousValues = snapshotPreviousValues(sheet, request);
        advancementDataMap.put("previousValues", previousValues);

        // Step 1 - Tier Achievements
        Map<String, Object> tierAchievements = new LinkedHashMap<>();
        if (isTierTransition) {
            applyTierAchievements(sheet, request, nextLevel, tierAchievements, appliedChanges, auth);
        }
        if (!tierAchievements.isEmpty()) {
            advancementDataMap.put("tierAchievements", tierAchievements);
        }

        // Step 2 - Apply 2 Advancements
        Long newTierExpId = tierAchievements.containsKey("experienceCreatedId") ?
                toLong(tierAchievements.get("experienceCreatedId")) : null;
        List<Map<String, Object>> advancementsList = new ArrayList<>();
        for (AdvancementChoice choice : request.getAdvancements()) {
            Map<String, Object> advData = applyAdvancement(sheet, choice, appliedChanges, newTierExpId);
            advancementsList.add(advData);
        }
        advancementDataMap.put("advancements", advancementsList);

        // Step 3 - Damage Thresholds
        int prevMajor = sheet.getMajorDamageThreshold();
        int prevSevere = sheet.getSevereDamageThreshold();
        sheet.setMajorDamageThreshold(prevMajor + 1);
        sheet.setSevereDamageThreshold(prevSevere + 1);
        advancementDataMap.put("previousDamageThresholds", Map.of("major", prevMajor, "severe", prevSevere));
        appliedChanges.add("+1 to major and severe damage thresholds");

        // Step 4 - Domain Card
        if (request.getNewDomainCardId() != null) {
            DomainCard newCard = domainCardRepository.findById(request.getNewDomainCardId())
                    .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + request.getNewDomainCardId()));

            boolean equip = Boolean.TRUE.equals(request.getEquipNewDomainCard());
            CharacterSheetDomainCard csdc = CharacterSheetDomainCard.builder()
                    .characterSheet(sheet)
                    .domainCard(newCard)
                    .equipped(equip)
                    .build();
            characterSheetDomainCardRepository.save(csdc);

            Map<String, Object> newDomainCardData = new LinkedHashMap<>();
            newDomainCardData.put("domainCardId", newCard.getId());
            newDomainCardData.put("equipped", equip);
            advancementDataMap.put("newDomainCard", newDomainCardData);
            appliedChanges.add("Added domain card '" + newCard.getName() + "'");
        }

        if (request.getUnequipDomainCardId() != null) {
            CharacterSheetDomainCard toUnequip = characterSheetDomainCardRepository
                    .findByCharacterSheetIdAndDomainCardId(characterSheetId, request.getUnequipDomainCardId())
                    .orElseThrow(() -> new EntityNotFoundException("Domain card association not found for unequip"));
            toUnequip.setEquipped(false);
            characterSheetDomainCardRepository.save(toUnequip);
            advancementDataMap.put("unequipDomainCardId", request.getUnequipDomainCardId());
            appliedChanges.add("Unequipped domain card ID " + request.getUnequipDomainCardId());
        }

        // Process Trades
        if (request.getTrades() != null && !request.getTrades().isEmpty()) {
            List<Map<String, Object>> tradesData = processTrades(sheet, request.getTrades(), appliedChanges);
            advancementDataMap.put("trades", tradesData);
        }

        // Increment level
        sheet.setLevel(nextLevel);
        appliedChanges.add("Level up to " + nextLevel);

        // Validate equipped count
        long equippedCount = characterSheetDomainCardRepository.countEquippedByCharacterSheetId(characterSheetId);
        if (equippedCount > MAX_EQUIPPED_DOMAIN_CARDS) {
            throw new IllegalStateException("Equipped domain card count (" + equippedCount + ") exceeds maximum of " + MAX_EQUIPPED_DOMAIN_CARDS);
        }

        // Save character sheet
        CharacterSheet savedSheet = characterSheetRepository.save(sheet);

        // Save advancement log
        String advancementDataJson;
        try {
            advancementDataJson = objectMapper.writeValueAsString(advancementDataMap);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize advancement data", e);
        }

        CharacterAdvancementLog logEntry = CharacterAdvancementLog.builder()
                .characterSheet(savedSheet)
                .fromLevel(currentLevel)
                .toLevel(nextLevel)
                .tier(nextTier)
                .advancementData(advancementDataJson)
                .build();
        CharacterAdvancementLog savedLog = characterAdvancementLogRepository.save(logEntry);

        String advancementSummary = request.getAdvancements().stream()
                .map(a -> a.getType().name())
                .collect(Collectors.joining(", "));
        AuditContext levelUpCtx = AuditContext.forUser(auth).withCharacterSheetId(characterSheetId).build();
        auditLogger.log(AuditAction.CHARACTER_LEVELED_UP, levelUpCtx,
                String.format("level %d → %d, tier %d, advancements: %s", currentLevel, nextLevel, nextTier, advancementSummary));

        return LevelUpResponse.builder()
                .characterSheet(characterSheetService.toResponse(savedSheet, Set.of()))
                .advancementLogId(savedLog.getId())
                .appliedChanges(appliedChanges)
                .build();
    }

    /**
     * Undoes the most recent level-up for a character sheet.
     *
     * @param characterSheetId the character sheet ID
     * @param auth the authentication object containing the current user
     * @return CharacterSheetResponse with the character restored to their previous level
     * @throws EntityNotFoundException if the character sheet is not found
     * @throws IllegalStateException if no advancement history exists or level mismatch
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public CharacterSheetResponse undoLevelUp(Long characterSheetId, Authentication auth) {
        CharacterSheet sheet = characterSheetRepository.findActiveById(characterSheetId)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + characterSheetId));

        characterSheetService.validateAccess(sheet, auth, "undo level-up");

        CharacterAdvancementLog logEntry = characterAdvancementLogRepository
                .findTopByCharacterSheetIdOrderByToLevelDesc(characterSheetId)
                .orElseThrow(() -> new IllegalStateException("No level-up history found"));

        if (!sheet.getLevel().equals(logEntry.getToLevel())) {
            throw new IllegalStateException("Character level (" + sheet.getLevel() +
                    ") does not match advancement log's toLevel (" + logEntry.getToLevel() + ")");
        }

        Map<String, Object> data;
        try {
            data = objectMapper.readValue(logEntry.getAdvancementData(), Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize advancement data", e);
        }

        // Decrement level
        sheet.setLevel(logEntry.getFromLevel());

        // Restore damage thresholds
        Map<String, Object> prevThresholds = (Map<String, Object>) data.get("previousDamageThresholds");
        if (prevThresholds != null) {
            sheet.setMajorDamageThreshold(toInt(prevThresholds.get("major")));
            sheet.setSevereDamageThreshold(toInt(prevThresholds.get("severe")));
        }

        // Reverse trades
        List<Map<String, Object>> tradesData = (List<Map<String, Object>>) data.get("trades");
        if (tradesData != null) {
            for (Map<String, Object> trade : tradesData) {
                List<Number> inIds = (List<Number>) trade.get("inIds");
                List<Number> outIds = (List<Number>) trade.get("outIds");
                List<Number> outEquipped = (List<Number>) trade.get("outEquipped");

                // Remove traded-in cards from collection (orphanRemoval handles DB delete)
                if (inIds != null) {
                    for (Number inId : inIds) {
                        sheet.getCharacterSheetDomainCards()
                                .removeIf(csdc -> csdc.getDomainCard().getId().equals(inId.longValue()));
                    }
                }

                // Re-add traded-out cards
                if (outIds != null) {
                    Set<Long> outEquippedSet = outEquipped != null ?
                            outEquipped.stream().map(Number::longValue).collect(Collectors.toSet()) :
                            Set.of();
                    for (Number outId : outIds) {
                        DomainCard card = domainCardRepository.findById(outId.longValue())
                                .orElseThrow(() -> new EntityNotFoundException("DomainCard not found for undo trade"));
                        CharacterSheetDomainCard csdc = CharacterSheetDomainCard.builder()
                                .characterSheet(sheet)
                                .domainCard(card)
                                .equipped(outEquippedSet.contains(outId.longValue()))
                                .build();
                        sheet.getCharacterSheetDomainCards().add(csdc);
                    }
                }
            }
        }

        // Remove Step 4 domain card from collection (orphanRemoval handles DB delete)
        Map<String, Object> newDomainCard = (Map<String, Object>) data.get("newDomainCard");
        if (newDomainCard != null) {
            Long domainCardId = toLong(newDomainCard.get("domainCardId"));
            sheet.getCharacterSheetDomainCards()
                    .removeIf(csdc -> csdc.getDomainCard().getId().equals(domainCardId));
        }

        // Re-equip unequipped card via collection (entity is managed, no explicit save needed)
        Object unequipId = data.get("unequipDomainCardId");
        if (unequipId != null) {
            Long unequipDomainCardId = toLong(unequipId);
            sheet.getCharacterSheetDomainCards().stream()
                    .filter(csdc -> csdc.getDomainCard().getId().equals(unequipDomainCardId))
                    .findFirst()
                    .ifPresent(csdc -> csdc.setEquipped(true));
        }

        // Reverse advancements
        Map<String, Object> previousValues = (Map<String, Object>) data.get("previousValues");
        List<Map<String, Object>> advancements = (List<Map<String, Object>>) data.get("advancements");
        if (advancements != null) {
            for (Map<String, Object> adv : advancements) {
                reverseAdvancement(sheet, adv, previousValues);
            }
        }

        // Reverse tier achievements
        Map<String, Object> tierAchievements = (Map<String, Object>) data.get("tierAchievements");
        if (tierAchievements != null) {
            reverseTierAchievements(sheet, tierAchievements);
        }

        CharacterSheet savedSheet = characterSheetRepository.save(sheet);
        characterAdvancementLogRepository.delete(logEntry);

        AuditContext undoCtx = AuditContext.forUser(auth).withCharacterSheetId(characterSheetId).build();
        auditLogger.log(AuditAction.CHARACTER_LEVEL_UNDONE, undoCtx,
                String.format("level %d → %d", logEntry.getToLevel(), logEntry.getFromLevel()));

        return characterSheetService.toResponse(savedSheet, Set.of());
    }

    // ==================== HELPER METHODS ====================

    /**
     * Determines the tier for a given character level.
     *
     * @param level the character level (1-10)
     * @return the tier (1-4)
     */
    int getTierForLevel(int level) {
        if (level <= 1) return 1;
        if (level <= 4) return 2;
        if (level <= 7) return 3;
        return 4;
    }

    /**
     * Returns the maximum domain card level for a given tier, or null if uncapped.
     *
     * @param tier the tier
     * @return the domain card level cap, or null for tier 4
     */
    private Integer getDomainCardLevelCap(int tier) {
        return switch (tier) {
            case 2 -> 4;
            case 3 -> 7;
            default -> null;
        };
    }

    /**
     * Collects all domain IDs accessible to a character through their subclass paths.
     *
     * @param sheet the character sheet
     * @return set of accessible domain IDs
     */
    private Set<Long> getAccessibleDomainIds(CharacterSheet sheet) {
        Set<Long> domainIds = new HashSet<>();
        for (SubclassCard sc : sheet.getSubclassCards()) {
            SubclassPath path = sc.getSubclassPath();
            if (path != null && path.getAssociatedDomains() != null) {
                for (Domain domain : path.getAssociatedDomains()) {
                    domainIds.add(domain.getId());
                }
            }
        }
        return domainIds;
    }

    /**
     * Gets the trait modifier value from a character sheet for a specific trait.
     *
     * @param sheet the character sheet
     * @param trait the trait to get
     * @return the modifier value
     */
    private int getTraitModifier(CharacterSheet sheet, Trait trait) {
        return switch (trait) {
            case AGILITY -> sheet.getAgilityModifier();
            case STRENGTH -> sheet.getStrengthModifier();
            case FINESSE -> sheet.getFinesseModifier();
            case INSTINCT -> sheet.getInstinctModifier();
            case PRESENCE -> sheet.getPresenceModifier();
            case KNOWLEDGE -> sheet.getKnowledgeModifier();
        };
    }

    /**
     * Sets the trait modifier value on a character sheet for a specific trait.
     *
     * @param sheet the character sheet
     * @param trait the trait to set
     * @param value the new modifier value
     */
    private void setTraitModifier(CharacterSheet sheet, Trait trait, int value) {
        switch (trait) {
            case AGILITY -> sheet.setAgilityModifier(value);
            case STRENGTH -> sheet.setStrengthModifier(value);
            case FINESSE -> sheet.setFinesseModifier(value);
            case INSTINCT -> sheet.setInstinctModifier(value);
            case PRESENCE -> sheet.setPresenceModifier(value);
            case KNOWLEDGE -> sheet.setKnowledgeModifier(value);
        }
    }

    /**
     * Gets the trait marked status from a character sheet for a specific trait.
     *
     * @param sheet the character sheet
     * @param trait the trait to check
     * @return true if the trait is marked
     */
    private boolean getTraitMarked(CharacterSheet sheet, Trait trait) {
        return switch (trait) {
            case AGILITY -> sheet.getAgilityMarked();
            case STRENGTH -> sheet.getStrengthMarked();
            case FINESSE -> sheet.getFinesseMarked();
            case INSTINCT -> sheet.getInstinctMarked();
            case PRESENCE -> sheet.getPresenceMarked();
            case KNOWLEDGE -> sheet.getKnowledgeMarked();
        };
    }

    /**
     * Sets the trait marked status on a character sheet for a specific trait.
     *
     * @param sheet the character sheet
     * @param trait the trait to set
     * @param value the new marked status
     */
    private void setTraitMarked(CharacterSheet sheet, Trait trait, boolean value) {
        switch (trait) {
            case AGILITY -> sheet.setAgilityMarked(value);
            case STRENGTH -> sheet.setStrengthMarked(value);
            case FINESSE -> sheet.setFinesseMarked(value);
            case INSTINCT -> sheet.setInstinctMarked(value);
            case PRESENCE -> sheet.setPresenceMarked(value);
            case KNOWLEDGE -> sheet.setKnowledgeMarked(value);
        }
    }

    /**
     * Returns the maximum number of times an advancement type can be used per tier.
     *
     * @param type the advancement type
     * @return the per-tier usage limit
     */
    private int getAdvancementLimitPerTier(AdvancementType type) {
        return switch (type) {
            case BOOST_TRAITS -> 3;
            case GAIN_HP -> 2;
            case GAIN_STRESS -> 2;
            case BOOST_EXPERIENCES -> 1;
            case GAIN_DOMAIN_CARD -> 1;
            case BOOST_EVASION -> 1;
            case UPGRADE_SUBCLASS -> 1;
            case BOOST_PROFICIENCY -> 2;
            case MULTICLASS -> 2;
            case UPGRADE_COMBO_DIE -> 1;
            // Feature-granted cards have no per-tier cap — quantity is gated by the subclass
            // feature's modifier, not by level-up usage limits. Value is unused because
            // FEATURE_DOMAIN_CARD is never counted in usage maps, but Integer.MAX_VALUE makes
            // the "unlimited" intent explicit if anyone does consult it.
            case FEATURE_DOMAIN_CARD -> Integer.MAX_VALUE;
        };
    }

    /**
     * Builds a map of advancement type usage counts from advancement log entries.
     *
     * @param tierLogs the advancement logs for the tier
     * @return map of advancement type to usage count
     */
    @SuppressWarnings("unchecked")
    private Map<AdvancementType, Integer> buildUsageMap(List<CharacterAdvancementLog> tierLogs) {
        Map<AdvancementType, Integer> usageMap = new EnumMap<>(AdvancementType.class);
        for (AdvancementType type : AdvancementType.values()) {
            usageMap.put(type, 0);
        }

        for (CharacterAdvancementLog logEntry : tierLogs) {
            try {
                Map<String, Object> data = objectMapper.readValue(logEntry.getAdvancementData(), Map.class);
                List<Map<String, Object>> advancements = (List<Map<String, Object>>) data.get("advancements");
                if (advancements != null) {
                    for (Map<String, Object> adv : advancements) {
                        String typeStr = (String) adv.get("type");
                        AdvancementType type = AdvancementType.valueOf(typeStr);
                        usageMap.merge(type, 1, Integer::sum);
                    }
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse advancement data for log {}: {}", logEntry.getId(), e.getMessage());
            }
        }

        return usageMap;
    }

    /**
     * Builds the list of available advancements for a given tier and usage.
     *
     * @param nextTier the target tier
     * @param usageMap the current tier usage counts
     * @return list of available advancements
     */
    private List<AvailableAdvancement> buildAvailableAdvancements(int nextTier, Map<AdvancementType, Integer> usageMap) {
        List<AvailableAdvancement> available = new ArrayList<>();

        int upgradeUsed = usageMap.getOrDefault(AdvancementType.UPGRADE_SUBCLASS, 0);
        int multiclassUsed = usageMap.getOrDefault(AdvancementType.MULTICLASS, 0);

        for (AdvancementType type : AdvancementType.values()) {
            if (type.getMinTier() > nextTier) continue;
            // FEATURE_DOMAIN_CARD is not a player-selectable advancement — the client injects
            // it based on the subclass feature's BONUS_DOMAIN_CARD_SELECTIONS modifier.
            if (type == AdvancementType.FEATURE_DOMAIN_CARD) continue;

            int limit = getAdvancementLimitPerTier(type);
            int used = usageMap.getOrDefault(type, 0);
            int remaining = limit - used;

            // Mutual exclusion: UPGRADE_SUBCLASS and MULTICLASS cannot be mixed within a tier
            if (type == AdvancementType.UPGRADE_SUBCLASS && multiclassUsed > 0) {
                remaining = 0;
            } else if (type == AdvancementType.MULTICLASS && upgradeUsed > 0) {
                remaining = 0;
            }

            List<AdvancementType> mutuallyExclusiveWith;
            if (type == AdvancementType.UPGRADE_SUBCLASS) {
                mutuallyExclusiveWith = List.of(AdvancementType.MULTICLASS);
            } else if (type == AdvancementType.MULTICLASS) {
                mutuallyExclusiveWith = List.of(AdvancementType.UPGRADE_SUBCLASS);
            } else {
                mutuallyExclusiveWith = List.of();
            }

            available.add(AvailableAdvancement.builder()
                    .type(type)
                    .remaining(Math.max(0, remaining))
                    .mutuallyExclusiveWith(mutuallyExclusiveWith)
                    .build());
        }

        return available;
    }

    /**
     * Validates the entire level-up request.
     *
     * @param request the level-up request
     * @param sheet the character sheet
     * @param nextTier the target tier
     * @param usageMap the current tier usage counts
     * @param isTierTransition whether this is a tier transition
     * @param nextLevel the target level
     */
    private void validateLevelUpRequest(LevelUpRequest request, CharacterSheet sheet,
                                        int nextTier, Map<AdvancementType, Integer> usageMap,
                                        boolean isTierTransition, int nextLevel) {
        if (request.getAdvancements() == null) {
            throw new IllegalStateException("Advancements are required");
        }

        // Partition player-chosen entries from feature-granted entries. Only player entries
        // are subject to the "exactly 2 advancements" rule and the per-tier usage limits.
        List<AdvancementChoice> playerEntries = request.getAdvancements().stream()
                .filter(c -> c.getType() != AdvancementType.FEATURE_DOMAIN_CARD)
                .toList();
        List<AdvancementChoice> featureEntries = request.getAdvancements().stream()
                .filter(c -> c.getType() == AdvancementType.FEATURE_DOMAIN_CARD)
                .toList();

        if (playerEntries.size() != 2) {
            throw new IllegalStateException("Exactly 2 player advancements are required");
        }

        if (isTierTransition && (request.getNewExperienceDescription() == null || request.getNewExperienceDescription().isBlank())) {
            throw new IllegalStateException("New experience description is required for tier transitions");
        }

        // Count types in this request (player entries only — feature entries are not limit-counted).
        Map<AdvancementType, Integer> requestCounts = new EnumMap<>(AdvancementType.class);
        for (AdvancementChoice choice : playerEntries) {
            requestCounts.merge(choice.getType(), 1, Integer::sum);
        }

        // Check mutual exclusion between UPGRADE_SUBCLASS and MULTICLASS
        boolean requestHasUpgrade = requestCounts.containsKey(AdvancementType.UPGRADE_SUBCLASS);
        boolean requestHasMulticlass = requestCounts.containsKey(AdvancementType.MULTICLASS);
        int existingUpgrade = usageMap.getOrDefault(AdvancementType.UPGRADE_SUBCLASS, 0);
        int existingMulticlass = usageMap.getOrDefault(AdvancementType.MULTICLASS, 0);

        if (requestHasUpgrade && (existingMulticlass > 0 || requestHasMulticlass)) {
            throw new IllegalStateException("UPGRADE_SUBCLASS and MULTICLASS are mutually exclusive within a tier");
        }
        if (requestHasMulticlass && existingUpgrade > 0) {
            throw new IllegalStateException("UPGRADE_SUBCLASS and MULTICLASS are mutually exclusive within a tier");
        }

        Set<Long> accessibleDomainIds = getAccessibleDomainIds(sheet);
        Integer domainCardLevelCap = getDomainCardLevelCap(nextTier);

        for (AdvancementChoice choice : playerEntries) {
            AdvancementType type = choice.getType();

            // Check tier availability
            if (type.getMinTier() > nextTier) {
                throw new IllegalStateException("Advancement " + type + " is not available in tier " + nextTier);
            }

            // Check remaining usage
            int limit = getAdvancementLimitPerTier(type);
            int used = usageMap.getOrDefault(type, 0);
            int requestedCount = requestCounts.get(type);

            if (used + requestedCount > limit) {
                throw new IllegalStateException("Advancement " + type + " usage (" + (used + requestedCount) +
                        ") exceeds tier limit of " + limit);
            }

            // Type-specific validation
            switch (type) {
                case BOOST_TRAITS -> validateBoostTraits(choice, sheet, nextLevel);
                case BOOST_EXPERIENCES -> validateBoostExperiences(choice, sheet, isTierTransition);
                case GAIN_DOMAIN_CARD -> validateGainDomainCard(choice, accessibleDomainIds, domainCardLevelCap);
                case UPGRADE_SUBCLASS -> validateUpgradeSubclass(choice, sheet);
                case MULTICLASS -> validateMulticlass(choice, sheet);
                case UPGRADE_COMBO_DIE -> validateUpgradeComboDie(sheet);
                default -> { /* GAIN_HP, GAIN_STRESS, BOOST_EVASION, BOOST_PROFICIENCY need no extra validation */ }
            }
        }

        // Feature-granted domain card entries: per-entry validation only (no tier-usage counting).
        for (AdvancementChoice choice : featureEntries) {
            validateGainDomainCard(choice, accessibleDomainIds, domainCardLevelCap);
        }

        // Cross-validation for duplicate advancement types in the same request
        if (requestCounts.getOrDefault(AdvancementType.BOOST_TRAITS, 0) == 2) {
            Set<Trait> allTraits = new HashSet<>();
            int totalTraitCount = 0;
            for (AdvancementChoice choice : playerEntries) {
                if (choice.getType() == AdvancementType.BOOST_TRAITS) {
                    allTraits.addAll(choice.getTraits());
                    totalTraitCount += choice.getTraits().size();
                }
            }
            if (allTraits.size() < totalTraitCount) {
                throw new IllegalStateException("Traits must be distinct across both BOOST_TRAITS choices");
            }
        }

        if (requestCounts.getOrDefault(AdvancementType.MULTICLASS, 0) == 2) {
            Set<Long> targetClassIds = new HashSet<>();
            for (AdvancementChoice choice : playerEntries) {
                if (choice.getType() == AdvancementType.MULTICLASS) {
                    SubclassCard card = subclassCardRepository.findById(choice.getSubclassCardId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "SubclassCard not found with id: " + choice.getSubclassCardId()));
                    Long classId = card.getSubclassPath().getAssociatedClass().getId();
                    if (!targetClassIds.add(classId)) {
                        throw new IllegalStateException(
                                "Cannot multiclass into the same class twice in one level-up");
                    }
                }
            }
        }

        // Validate trades
        if (request.getTrades() != null) {
            for (DomainCardTradeRequest trade : request.getTrades()) {
                validateTrade(trade, sheet, accessibleDomainIds, domainCardLevelCap);
            }
        }

        // Validate new domain card from Step 4
        if (request.getNewDomainCardId() != null) {
            DomainCard newCard = domainCardRepository.findById(request.getNewDomainCardId())
                    .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + request.getNewDomainCardId()));
            if (!accessibleDomainIds.contains(newCard.getAssociatedDomain().getId())) {
                throw new IllegalStateException("Domain card is not from an accessible domain");
            }
            if (domainCardLevelCap != null && newCard.getLevel() > domainCardLevelCap) {
                throw new IllegalStateException("Domain card level exceeds cap of " + domainCardLevelCap);
            }
        }

        // Validate equipped count won't exceed limit
        if (Boolean.TRUE.equals(request.getEquipNewDomainCard())) {
            long equippedCount = characterSheetDomainCardRepository.countEquippedByCharacterSheetId(sheet.getId());
            if (equippedCount >= MAX_EQUIPPED_DOMAIN_CARDS && request.getUnequipDomainCardId() == null) {
                throw new IllegalStateException("Equipping new domain card would exceed maximum of " +
                        MAX_EQUIPPED_DOMAIN_CARDS + ". Provide unequipDomainCardId.");
            }
        }
    }

    private void validateBoostTraits(AdvancementChoice choice, CharacterSheet sheet, int nextLevel) {
        if (choice.getTraits() == null || choice.getTraits().size() != 2) {
            throw new IllegalStateException("BOOST_TRAITS requires exactly 2 traits");
        }
        // Trait marks are cleared at levels 5 and 8 (entering Tier 3 and Tier 4),
        // so previously marked traits can be re-selected during those tier upgrades.
        boolean marksWillBeCleared = (nextLevel == 5 || nextLevel == 8);
        for (Trait trait : choice.getTraits()) {
            if (getTraitMarked(sheet, trait) && !marksWillBeCleared) {
                throw new IllegalStateException("Trait " + trait + " is already marked");
            }
        }
    }

    private void validateBoostExperiences(AdvancementChoice choice, CharacterSheet sheet, boolean isTierTransition) {
        if (Boolean.TRUE.equals(choice.getBoostNewExperience())) {
            if (!isTierTransition) {
                throw new IllegalStateException("boostNewExperience is only valid during tier transitions");
            }
            if (choice.getExperienceIds() == null || choice.getExperienceIds().size() != 1) {
                throw new IllegalStateException("BOOST_EXPERIENCES with boostNewExperience requires exactly 1 experience ID");
            }
        } else {
            if (choice.getExperienceIds() == null || choice.getExperienceIds().size() != 2) {
                throw new IllegalStateException("BOOST_EXPERIENCES requires exactly 2 experience IDs");
            }
        }
        for (Long expId : choice.getExperienceIds()) {
            boolean belongs = sheet.getExperiences().stream().anyMatch(e -> e.getId().equals(expId));
            if (!belongs) {
                throw new IllegalStateException("Experience " + expId + " does not belong to this character");
            }
        }
    }

    private void validateGainDomainCard(AdvancementChoice choice, Set<Long> accessibleDomainIds, Integer cap) {
        if (choice.getDomainCardId() == null) {
            throw new IllegalStateException("GAIN_DOMAIN_CARD requires a domainCardId");
        }
        DomainCard card = domainCardRepository.findById(choice.getDomainCardId())
                .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + choice.getDomainCardId()));
        if (!accessibleDomainIds.contains(card.getAssociatedDomain().getId())) {
            throw new IllegalStateException("Domain card is not from an accessible domain");
        }
        if (cap != null && card.getLevel() > cap) {
            throw new IllegalStateException("Domain card level exceeds cap of " + cap);
        }
    }

    private void validateUpgradeSubclass(AdvancementChoice choice, CharacterSheet sheet) {
        if (choice.getSubclassCardId() == null) {
            throw new IllegalStateException("UPGRADE_SUBCLASS requires a subclassCardId");
        }
        SubclassCard card = subclassCardRepository.findById(choice.getSubclassCardId())
                .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + choice.getSubclassCardId()));

        // Card's path must match an existing subclass card's path
        boolean pathMatch = sheet.getSubclassCards().stream()
                .anyMatch(sc -> sc.getSubclassPath().getId().equals(card.getSubclassPath().getId()));
        if (!pathMatch) {
            throw new IllegalStateException("Subclass card's path does not match any of the character's existing subclass paths");
        }

        // Card level must be the next level up
        SubclassLevel expectedLevel = getNextSubclassLevel(sheet, card.getSubclassPath().getId());
        if (expectedLevel == null || card.getLevel() != expectedLevel) {
            throw new IllegalStateException("Subclass card is not the next level in the path");
        }
    }

    private void validateMulticlass(AdvancementChoice choice, CharacterSheet sheet) {
        if (choice.getSubclassCardId() == null) {
            throw new IllegalStateException("MULTICLASS requires a subclassCardId");
        }
        SubclassCard card = subclassCardRepository.findById(choice.getSubclassCardId())
                .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + choice.getSubclassCardId()));

        if (card.getLevel() != SubclassLevel.FOUNDATION) {
            throw new IllegalStateException("MULTICLASS requires a FOUNDATION level subclass card");
        }

        // Must be from a class the character doesn't already have
        Long newClassId = card.getSubclassPath().getAssociatedClass().getId();
        boolean hasClass = sheet.getSubclassCards().stream()
                .anyMatch(sc -> sc.getSubclassPath().getAssociatedClass().getId().equals(newClassId));
        if (hasClass) {
            throw new IllegalStateException("Character already has a subclass from this class");
        }
    }

    /**
     * Validates that a character's Combo Die can still be stepped up.
     * <p>
     * The per-tier usage limit (once per tier) is already enforced generically in
     * {@link #validateLevelUpRequest}; this only guards against stepping past the largest
     * available die size.
     * </p><p>
     * Note the printed ceiling is lower than this guard: the die starts at d4 and steps once per
     * tier across 4 tiers, so play can only reach d12. The check is against the largest
     * {@link DiceType} rather than d12 so that it stays correct if the tier count or the
     * per-tier allowance ever changes, rather than silently permitting growth past the rules.
     * </p>
     *
     * @param sheet the character sheet
     * @throws IllegalStateException if the Combo Die is already at its maximum size
     */
    private void validateUpgradeComboDie(CharacterSheet sheet) {
        DiceType currentDie = sheet.getComboDie() != null ? sheet.getComboDie() : DiceType.D4;
        if (currentDie.ordinal() >= DiceType.values().length - 1) {
            throw new IllegalStateException("Combo Die is already at its maximum size (" + currentDie.getCode() + ")");
        }
    }

    private void validateTrade(DomainCardTradeRequest trade, CharacterSheet sheet,
                               Set<Long> accessibleDomainIds, Integer domainCardLevelCap) {
        if (trade.getTradeOutCardIds().size() != trade.getTradeInCardIds().size()) {
            throw new IllegalStateException("Trade must have equal number of cards traded in and out");
        }
        for (Long outId : trade.getTradeOutCardIds()) {
            boolean belongs = characterSheetDomainCardRepository
                    .findByCharacterSheetIdAndDomainCardId(sheet.getId(), outId).isPresent();
            if (!belongs) {
                throw new IllegalStateException("Traded-out card " + outId + " does not belong to this character");
            }
        }
        for (Long inId : trade.getTradeInCardIds()) {
            DomainCard card = domainCardRepository.findById(inId)
                    .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + inId));
            if (!accessibleDomainIds.contains(card.getAssociatedDomain().getId())) {
                throw new IllegalStateException("Traded-in card is not from an accessible domain");
            }
            if (domainCardLevelCap != null && card.getLevel() > domainCardLevelCap) {
                throw new IllegalStateException("Traded-in card level exceeds cap");
            }
        }
    }

    /**
     * Gets the next subclass level for a given path based on the character's current cards.
     */
    private SubclassLevel getNextSubclassLevel(CharacterSheet sheet, Long pathId) {
        boolean hasFoundation = false;
        boolean hasSpecialization = false;

        for (SubclassCard sc : sheet.getSubclassCards()) {
            if (sc.getSubclassPath().getId().equals(pathId)) {
                if (sc.getLevel() == SubclassLevel.FOUNDATION) hasFoundation = true;
                if (sc.getLevel() == SubclassLevel.SPECIALIZATION) hasSpecialization = true;
            }
        }

        if (!hasFoundation) return SubclassLevel.FOUNDATION;
        if (!hasSpecialization) return SubclassLevel.SPECIALIZATION;
        return SubclassLevel.MASTERY;
    }

    /**
     * Snapshots previous values needed for undo.
     */
    private Map<String, Object> snapshotPreviousValues(CharacterSheet sheet, LevelUpRequest request) {
        Map<String, Object> prev = new LinkedHashMap<>();
        prev.put("proficiency", sheet.getProficiency());
        prev.put("evasion", sheet.getEvasion());
        prev.put("hitPointMax", sheet.getHitPointMax());
        prev.put("stressMax", sheet.getStressMax());

        // Snapshot trait modifiers and marks for traits being boosted
        Map<String, Integer> traitModifiers = new LinkedHashMap<>();
        Map<String, Boolean> traitMarks = new LinkedHashMap<>();
        for (Trait t : Trait.values()) {
            traitModifiers.put(t.name(), getTraitModifier(sheet, t));
            traitMarks.put(t.name(), getTraitMarked(sheet, t));
        }
        prev.put("traitModifiers", traitModifiers);
        prev.put("traitMarks", traitMarks);

        // Snapshot experience modifiers
        Map<String, Integer> experienceModifiers = new LinkedHashMap<>();
        for (AdvancementChoice choice : request.getAdvancements()) {
            if (choice.getType() == AdvancementType.BOOST_EXPERIENCES && choice.getExperienceIds() != null) {
                for (Long expId : choice.getExperienceIds()) {
                    sheet.getExperiences().stream()
                            .filter(e -> e.getId().equals(expId))
                            .findFirst()
                            .ifPresent(exp -> experienceModifiers.put(expId.toString(), exp.getModifier()));
                }
            }
        }
        prev.put("experienceModifiers", experienceModifiers);

        return prev;
    }

    /**
     * Applies tier transition achievements.
     */
    private void applyTierAchievements(CharacterSheet sheet, LevelUpRequest request, int nextLevel,
                                        Map<String, Object> tierAchievements, List<String> appliedChanges,
                                        Authentication auth) {
        // Create new experience
        com.aboff.core.security.CustomUserDetails userDetails =
                (com.aboff.core.security.CustomUserDetails) auth.getPrincipal();
        com.aboff.core.model.entity.User owner = userDetails.getUser();

        Experience newExp = Experience.builder()
                .characterSheet(sheet)
                .createdBy(owner)
                .description(request.getNewExperienceDescription())
                .modifier(2)
                .build();
        Experience savedExp = experienceRepository.save(newExp);
        sheet.getExperiences().add(savedExp);
        tierAchievements.put("experienceCreatedId", savedExp.getId());
        appliedChanges.add("New experience: '" + request.getNewExperienceDescription() + "' (+2)");

        // Increment proficiency
        sheet.setProficiency(sheet.getProficiency() + 1);
        tierAchievements.put("proficiencyIncremented", true);
        appliedChanges.add("+1 proficiency");

        // Clear trait marks at levels 5 and 8 (entering Tier 3 and Tier 4)
        if (nextLevel == 5 || nextLevel == 8) {
            Map<String, Boolean> previousTraitMarks = new LinkedHashMap<>();
            for (Trait trait : Trait.values()) {
                previousTraitMarks.put(trait.name(), getTraitMarked(sheet, trait));
                setTraitMarked(sheet, trait, false);
            }
            tierAchievements.put("traitsCleared", true);
            tierAchievements.put("previousTraitMarks", previousTraitMarks);
            appliedChanges.add("Cleared all trait marks");
        }
    }

    /**
     * Applies a single advancement choice and returns the data map for logging.
     */
    private Map<String, Object> applyAdvancement(CharacterSheet sheet, AdvancementChoice choice,
                                                   List<String> appliedChanges, Long newTierExperienceId) {
        Map<String, Object> advData = new LinkedHashMap<>();
        advData.put("type", choice.getType().name());

        switch (choice.getType()) {
            case BOOST_TRAITS -> {
                List<String> traitNames = new ArrayList<>();
                for (Trait trait : choice.getTraits()) {
                    setTraitModifier(sheet, trait, getTraitModifier(sheet, trait) + 1);
                    setTraitMarked(sheet, trait, true);
                    traitNames.add(trait.name());
                }
                advData.put("traits", traitNames);
                appliedChanges.add("Boosted " + traitNames.get(0) + " and " + traitNames.get(1) + " traits");
            }
            case GAIN_HP -> {
                sheet.setHitPointMax(sheet.getHitPointMax() + 1);
                appliedChanges.add("+1 hit point max");
            }
            case GAIN_STRESS -> {
                sheet.setStressMax(sheet.getStressMax() + 1);
                appliedChanges.add("+1 stress max");
            }
            case BOOST_EXPERIENCES -> {
                List<Long> expIds = new ArrayList<>(choice.getExperienceIds());
                if (Boolean.TRUE.equals(choice.getBoostNewExperience()) && newTierExperienceId != null) {
                    expIds.add(newTierExperienceId);
                }
                for (Long expId : expIds) {
                    Experience exp = sheet.getExperiences().stream()
                            .filter(e -> e.getId().equals(expId))
                            .findFirst()
                            .orElseThrow(() -> new EntityNotFoundException("Experience not found with id: " + expId));
                    exp.setModifier(exp.getModifier() + 1);
                    experienceRepository.save(exp);
                }
                advData.put("experienceIds", expIds);
                advData.put("boostNewExperience", Boolean.TRUE.equals(choice.getBoostNewExperience()));
                appliedChanges.add("Boosted " + expIds.size() + " experience modifiers");
            }
            case GAIN_DOMAIN_CARD -> {
                DomainCard card = domainCardRepository.findById(choice.getDomainCardId())
                        .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + choice.getDomainCardId()));
                boolean equip = Boolean.TRUE.equals(choice.getEquipDomainCard());
                CharacterSheetDomainCard csdc = CharacterSheetDomainCard.builder()
                        .characterSheet(sheet)
                        .domainCard(card)
                        .equipped(equip)
                        .build();
                characterSheetDomainCardRepository.save(csdc);
                advData.put("domainCardId", card.getId());
                advData.put("equipped", equip);
                appliedChanges.add("Gained domain card '" + card.getName() + "'");
            }
            case BOOST_EVASION -> {
                sheet.setEvasion(sheet.getEvasion() + 1);
                appliedChanges.add("+1 evasion");
            }
            case UPGRADE_SUBCLASS -> {
                SubclassCard card = subclassCardRepository.findById(choice.getSubclassCardId())
                        .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + choice.getSubclassCardId()));
                sheet.getSubclassCards().add(card);
                advData.put("subclassCardId", card.getId());
                appliedChanges.add("Upgraded subclass: " + card.getName());
            }
            case BOOST_PROFICIENCY -> {
                sheet.setProficiency(sheet.getProficiency() + 1);
                appliedChanges.add("+1 proficiency");
            }
            case UPGRADE_COMBO_DIE -> {
                DiceType storedDie = sheet.getComboDie();
                DiceType currentDie = storedDie != null ? storedDie : DiceType.D4;
                DiceType nextDie = DiceType.values()[currentDie.ordinal() + 1];
                advData.put("previousComboDie", storedDie != null ? storedDie.name() : null);
                sheet.setComboDie(nextDie);
                appliedChanges.add("Combo Die upgraded to " + nextDie.getCode());
            }
            case MULTICLASS -> {
                SubclassCard card = subclassCardRepository.findById(choice.getSubclassCardId())
                        .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + choice.getSubclassCardId()));
                sheet.getSubclassCards().add(card);
                advData.put("subclassCardId", card.getId());
                appliedChanges.add("Multiclassed into " + card.getName());
            }
            case FEATURE_DOMAIN_CARD -> {
                DomainCard card = domainCardRepository.findById(choice.getDomainCardId())
                        .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + choice.getDomainCardId()));
                // Feature-granted cards are always added unequipped; they do not consume an equipped slot.
                CharacterSheetDomainCard csdc = CharacterSheetDomainCard.builder()
                        .characterSheet(sheet)
                        .domainCard(card)
                        .equipped(false)
                        .build();
                characterSheetDomainCardRepository.save(csdc);
                advData.put("domainCardId", card.getId());
                advData.put("equipped", false);
                appliedChanges.add("Gained bonus domain card '" + card.getName() + "' from subclass feature");
            }
        }

        return advData;
    }

    /**
     * Processes domain card trades.
     */
    private List<Map<String, Object>> processTrades(CharacterSheet sheet, List<DomainCardTradeRequest> trades,
                                                     List<String> appliedChanges) {
        List<Map<String, Object>> tradesData = new ArrayList<>();

        for (DomainCardTradeRequest trade : trades) {
            Map<String, Object> tradeData = new LinkedHashMap<>();

            // Record which traded-out cards were equipped for undo
            List<Long> outEquipped = new ArrayList<>();
            for (Long outId : trade.getTradeOutCardIds()) {
                CharacterSheetDomainCard csdc = characterSheetDomainCardRepository
                        .findByCharacterSheetIdAndDomainCardId(sheet.getId(), outId)
                        .orElseThrow(() -> new EntityNotFoundException("Domain card association not found for trade"));
                if (Boolean.TRUE.equals(csdc.getEquipped())) {
                    outEquipped.add(outId);
                }
                characterSheetDomainCardRepository.delete(csdc);
            }

            Set<Long> equipSet = trade.getEquipTradedInCardIds() != null ?
                    new HashSet<>(trade.getEquipTradedInCardIds()) : Set.of();

            for (Long inId : trade.getTradeInCardIds()) {
                DomainCard card = domainCardRepository.findById(inId)
                        .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + inId));
                CharacterSheetDomainCard csdc = CharacterSheetDomainCard.builder()
                        .characterSheet(sheet)
                        .domainCard(card)
                        .equipped(equipSet.contains(inId))
                        .build();
                characterSheetDomainCardRepository.save(csdc);
            }

            tradeData.put("outIds", trade.getTradeOutCardIds());
            tradeData.put("inIds", trade.getTradeInCardIds());
            tradeData.put("outEquipped", outEquipped);
            tradeData.put("inEquipped", trade.getEquipTradedInCardIds() != null ? trade.getEquipTradedInCardIds() : List.of());
            tradesData.add(tradeData);
            appliedChanges.add("Traded " + trade.getTradeOutCardIds().size() + " domain cards");
        }

        return tradesData;
    }

    /**
     * Reverses a single advancement during undo.
     */
    @SuppressWarnings("unchecked")
    private void reverseAdvancement(CharacterSheet sheet, Map<String, Object> adv,
                                     Map<String, Object> previousValues) {
        String typeStr = (String) adv.get("type");
        AdvancementType type = AdvancementType.valueOf(typeStr);

        Map<String, Integer> prevTraitModifiers = previousValues != null ?
                (Map<String, Integer>) previousValues.get("traitModifiers") : null;

        switch (type) {
            case BOOST_TRAITS -> {
                List<String> traits = (List<String>) adv.get("traits");
                if (traits != null && prevTraitModifiers != null) {
                    for (String traitName : traits) {
                        Trait trait = Trait.valueOf(traitName);
                        Integer prevValue = prevTraitModifiers.get(traitName);
                        if (prevValue != null) {
                            setTraitModifier(sheet, trait, prevValue);
                        }
                        setTraitMarked(sheet, trait, false);
                    }
                }
            }
            case GAIN_HP -> {
                sheet.setHitPointMax(sheet.getHitPointMax() - 1);
                sheet.setHitPointMarked(Math.min(sheet.getHitPointMarked(), sheet.getHitPointMax()));
            }
            case GAIN_STRESS -> {
                sheet.setStressMax(sheet.getStressMax() - 1);
                sheet.setStressMarked(Math.min(sheet.getStressMarked(), sheet.getStressMax()));
            }
            case BOOST_EXPERIENCES -> {
                Map<String, Integer> prevExpModifiers = previousValues != null ?
                        (Map<String, Integer>) previousValues.get("experienceModifiers") : null;
                List<Number> expIds = (List<Number>) adv.get("experienceIds");
                if (expIds != null && prevExpModifiers != null) {
                    for (Number expId : expIds) {
                        String expIdStr = String.valueOf(expId.longValue());
                        Integer prevMod = prevExpModifiers.get(expIdStr);
                        if (prevMod != null) {
                            experienceRepository.findById(expId.longValue()).ifPresent(exp -> {
                                exp.setModifier(prevMod);
                                experienceRepository.save(exp);
                            });
                        }
                    }
                }
            }
            case GAIN_DOMAIN_CARD, FEATURE_DOMAIN_CARD -> {
                Long domainCardId = toLong(adv.get("domainCardId"));
                if (domainCardId != null) {
                    sheet.getCharacterSheetDomainCards()
                            .removeIf(csdc -> csdc.getDomainCard().getId().equals(domainCardId));
                }
            }
            case BOOST_EVASION -> sheet.setEvasion(sheet.getEvasion() - 1);
            case UPGRADE_SUBCLASS, MULTICLASS -> {
                Long subclassCardId = toLong(adv.get("subclassCardId"));
                if (subclassCardId != null) {
                    sheet.getSubclassCards().removeIf(sc -> sc.getId().equals(subclassCardId));
                }
            }
            case BOOST_PROFICIENCY -> sheet.setProficiency(sheet.getProficiency() - 1);
            case UPGRADE_COMBO_DIE -> {
                Object previous = adv.get("previousComboDie");
                sheet.setComboDie(previous != null ? DiceType.valueOf((String) previous) : null);
            }
        }
    }

    /**
     * Reverses tier achievements during undo.
     */
    @SuppressWarnings("unchecked")
    private void reverseTierAchievements(CharacterSheet sheet, Map<String, Object> tierAchievements) {
        // Delete created experience
        Object expIdObj = tierAchievements.get("experienceCreatedId");
        if (expIdObj != null) {
            Long expId = toLong(expIdObj);
            sheet.getExperiences().removeIf(e -> e.getId().equals(expId));
            experienceRepository.deleteById(expId);
        }

        // Decrement proficiency
        if (Boolean.TRUE.equals(tierAchievements.get("proficiencyIncremented"))) {
            sheet.setProficiency(sheet.getProficiency() - 1);
        }

        // Restore trait marks
        if (Boolean.TRUE.equals(tierAchievements.get("traitsCleared"))) {
            Map<String, Boolean> prevMarks = (Map<String, Boolean>) tierAchievements.get("previousTraitMarks");
            if (prevMarks != null) {
                for (Trait trait : Trait.values()) {
                    Boolean wasMarked = prevMarks.get(trait.name());
                    if (wasMarked != null) {
                        setTraitMarked(sheet, trait, wasMarked);
                    }
                }
            }
        }
    }

    /**
     * Safely converts an Object to int.
     */
    private int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        return Integer.parseInt(obj.toString());
    }

    /**
     * Safely converts an Object to Long.
     */
    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.longValue();
        return Long.parseLong(obj.toString());
    }
}

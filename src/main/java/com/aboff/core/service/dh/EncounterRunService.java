package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dh.ImprovisedTierStatistics;
import com.aboff.core.model.dto.dh.request.CreateEncounterRunRequest;
import com.aboff.core.model.dto.dh.request.UpdateEncounterRunAdversaryRequest;
import com.aboff.core.model.dto.dh.response.AdversaryResponse;
import com.aboff.core.model.dto.dh.response.EncounterResponse;
import com.aboff.core.model.dto.dh.response.EncounterRunResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Encounter;
import com.aboff.core.model.entity.dh.EncounterAdversary;
import com.aboff.core.model.entity.dh.EncounterRun;
import com.aboff.core.model.entity.dh.EncounterRunAdversary;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.model.enums.EncounterRunStatus;
import com.aboff.core.repository.dh.AdversaryRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.EncounterRepository;
import com.aboff.core.repository.dh.EncounterRunAdversaryRepository;
import com.aboff.core.repository.dh.EncounterRunRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import com.aboff.core.util.MarkdownSanitizerUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for running a fight: starting a run from a saved {@link Encounter}, tracking
 * per-instance live state, and completing or discarding it.
 * <p>
 * <strong>Snapshotting.</strong> {@link #startRun} copies the encounter's adversary instances
 * into {@link EncounterRunAdversary} rows. From that point the run is independent of the source
 * encounter -- editing the saved encounter mid-fight has no effect on a run already in progress.
 * </p>
 * <p>
 * <strong>Authorization.</strong> A run is visible and mutable to the user who started it, plus
 * -- only when it is tagged to a campaign -- anyone {@link CampaignService#hasGameMasterAccess}
 * approves for that campaign, plus any MODERATOR/ADMIN/OWNER regardless of campaign tag. This is
 * a single rule with no "GM mode" branch: for a standalone run (no campaign) it collapses to
 * owner-or-moderator-only, which is exactly what lets a user with no campaign at all start and
 * play a run. GM access itself is never reimplemented here -- it is delegated to
 * {@link CampaignService#hasGameMasterAccess}, the application's single definition of "is a GM".
 * </p>
 * <p>
 * <strong>Concurrency.</strong> There is no optimistic locking anywhere in this codebase.
 * {@link #updateRunAdversary} follows the same convention as {@code CountdownService#updateCountdownValue}
 * and {@code CampaignService#updateFear}: absolute values, not deltas, so two GMs (or two open
 * tabs) resolve to last-write-wins instead of compounding a lost update.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EncounterRunService {

    private final EncounterRunRepository encounterRunRepository;
    private final EncounterRunAdversaryRepository encounterRunAdversaryRepository;
    private final EncounterRepository encounterRepository;
    private final CampaignRepository campaignRepository;
    private final AdversaryRepository adversaryRepository;
    private final EncounterService encounterService;
    private final CampaignService campaignService;
    private final AdversaryService adversaryService;
    private final RoleHierarchyService roleHierarchyService;
    private final AuditLogger auditLogger;

    /**
     * Starts a run of an encounter, snapshotting its current adversary instances.
     * <p>
     * Requires only that the caller can view the source encounter -- the same rule
     * {@link EncounterService} already enforces for reads, delegated to rather than
     * reimplemented. Starting a run never requires a campaign: omitting {@code campaignId} in
     * the request starts a standalone run owned solely by the caller.
     * </p>
     *
     * @param encounterId The encounter to run
     * @param request The start request, whose {@code campaignId} is optional
     * @param auth The authentication object containing the current user
     * @return The newly started run, with adversary stat blocks expanded
     * @throws EntityNotFoundException if the encounter or, when provided, the campaign is not found
     * @throws InsufficientPermissionsException if the caller cannot view the source encounter
     * @throws IllegalStateException if the target campaign has ended
     */
    @Transactional
    public EncounterRunResponse startRun(Long encounterId, CreateEncounterRunRequest request, Authentication auth) {
        Encounter encounter = encounterRepository.findByIdAndDeletedAtIsNull(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with id: " + encounterId));
        encounterService.validateViewPermission(encounter, auth);

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User startedBy = userDetails.getUser();

        Long campaignId = request != null ? request.getCampaignId() : null;
        Campaign campaign = null;
        if (campaignId != null) {
            campaign = campaignRepository.findActiveById(campaignId)
                    .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));
            campaignService.validateNotEnded(campaign, "start an encounter run for");
        }

        EncounterRun run = EncounterRun.builder()
                .encounter(encounter)
                .campaign(campaign)
                .startedBy(startedBy)
                .status(EncounterRunStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .encounterRunAdversaries(new ArrayList<>())
                .build();

        List<EncounterAdversary> source = encounter.getEncounterAdversaries().stream()
                .sorted(Comparator.comparing(EncounterAdversary::getDisplayOrder))
                .toList();

        for (EncounterAdversary template : source) {
            EncounterRunAdversary snapshot = EncounterRunAdversary.builder()
                    .encounterRun(run)
                    .adversary(template.getAdversary())
                    .label(template.getLabel())
                    .tierOverride(template.getTierOverride())
                    .hitPointsMarked(0)
                    .stressMarked(0)
                    .isDefeated(false)
                    .tokens(0)
                    .displayOrder(template.getDisplayOrder())
                    .build();
            run.getEncounterRunAdversaries().add(snapshot);
        }

        EncounterRun saved = encounterRunRepository.save(run);
        log.info("Started encounter run {} from encounter {} ({} adversaries)",
                saved.getId(), encounterId, saved.getEncounterRunAdversaries().size());

        auditLogger.log(AuditAction.ENCOUNTER_RUN_STARTED,
                AuditContext.forUser(auth).withCampaignId(campaignId).build(),
                String.format("run_id: %d from encounter_id: %d (%d adversaries)",
                        saved.getId(), encounterId, saved.getEncounterRunAdversaries().size()));

        return toResponse(saved, true);
    }

    /**
     * Retrieves a single run, with every instance's full adversary stat block expanded.
     *
     * @param runId The run ID
     * @param auth The authentication object containing the current user
     * @return The run
     * @throws EntityNotFoundException if the run is not found
     * @throws InsufficientPermissionsException if the caller lacks access to the run
     */
    @Transactional(readOnly = true)
    public EncounterRunResponse getRun(Long runId, Authentication auth) {
        EncounterRun run = loadRun(runId);
        validateRunAccess(run, auth, "view");

        return toResponse(run, true);
    }

    /**
     * Lists the runs visible to the caller.
     * <p>
     * Omitting {@code campaignId} lists the caller's own runs (the standalone page's "resume"
     * list). Providing it lists that campaign's tagged runs (the GM screen panel), which
     * requires game master-level access to that campaign -- an unrelated user cannot discover a
     * campaign's runs just by guessing its ID.
     * </p>
     *
     * @param status Optional status filter
     * @param campaignId Optional campaign filter; when present, switches from "my runs" to
     *                    "that campaign's runs"
     * @param auth The authentication object containing the current user
     * @return The matching runs, newest first, with adversary stat blocks unexpanded
     * @throws EntityNotFoundException if {@code campaignId} is provided but not found
     * @throws InsufficientPermissionsException if the caller lacks game master access to
     *                                          {@code campaignId}
     */
    @Transactional(readOnly = true)
    public List<EncounterRunResponse> listRuns(EncounterRunStatus status, Long campaignId, Authentication auth) {
        List<EncounterRun> runs;

        if (campaignId != null) {
            Campaign campaign = campaignRepository.findActiveById(campaignId)
                    .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));
            campaignService.validateGameMasterAccess(campaign, auth, "view encounter runs for");

            runs = encounterRunRepository.findByCampaignIdAndOptionalStatus(campaignId, status);
        } else {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            runs = encounterRunRepository.findByStartedByIdAndOptionalStatus(userDetails.getUserId(), status);
        }

        return runs.stream().map(run -> toResponse(run, false)).toList();
    }

    /**
     * Updates a single adversary instance's live state within a run: marked HP/Stress, tokens,
     * defeated, and/or note. Every provided field is an absolute value, not a delta -- see the
     * class javadoc on concurrency.
     * <p>
     * {@code hitPointsMarked} and {@code stressMarked} are clamped to the adversary's
     * {@code hitPointMax}/{@code stressMax} rather than rejected outright, since a GM firing off
     * several clicks in a row should not see an error for briefly overshooting. {@code tokens}
     * (Daggerheart Core ch. 4, "Adversary Tokens") has no such ceiling -- a Pool can hold any
     * number -- so it is only floored at zero, never clamped to a max.
     * </p>
     *
     * @param runId The run ID
     * @param instanceId The run adversary instance ID to update
     * @param request The fields to update; a null field is left unchanged
     * @param auth The authentication object containing the current user
     * @return The updated run, with adversary stat blocks expanded
     * @throws EntityNotFoundException if the run or instance is not found, or the instance does
     *                                 not belong to the run
     * @throws InsufficientPermissionsException if the caller lacks access to the run
     * @throws IllegalStateException if the run is already completed, or its campaign has ended
     */
    @Transactional
    public EncounterRunResponse updateRunAdversary(
            Long runId, Long instanceId, UpdateEncounterRunAdversaryRequest request, Authentication auth) {

        EncounterRun run = loadRun(runId);
        validateRunAccess(run, auth, "update");

        if (run.getStatus() == EncounterRunStatus.COMPLETED) {
            throw new IllegalStateException("Cannot update adversaries in a completed encounter run");
        }
        if (run.getCampaign() != null) {
            campaignService.validateNotEnded(run.getCampaign(), "update encounter run adversaries for");
        }

        EncounterRunAdversary instance = encounterRunAdversaryRepository.findById(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter run adversary not found with id: " + instanceId));
        if (!instance.getEncounterRun().getId().equals(runId)) {
            throw new EntityNotFoundException(
                    "Encounter run adversary not found with id: " + instanceId + " in run: " + runId);
        }

        Adversary adversary = instance.getAdversary();
        if (request.getHitPointsMarked() != null) {
            instance.setHitPointsMarked(clamp(request.getHitPointsMarked(), 0, adversary.getHitPointMax()));
        }
        if (request.getStressMarked() != null) {
            instance.setStressMarked(clamp(request.getStressMarked(), 0, adversary.getStressMax()));
        }
        if (request.getTokens() != null) {
            // No ceiling to clamp against -- only floor at zero, same as the @Min(0) validation
            // on the request, kept here too since the service is called directly in tests and by
            // any future caller that bypasses bean validation.
            instance.setTokens(Math.max(0, request.getTokens()));
        }
        if (request.getIsDefeated() != null) {
            instance.setIsDefeated(request.getIsDefeated());
        }
        if (request.getNote() != null) {
            instance.setNote(MarkdownSanitizerUtil.sanitize(request.getNote()));
        }

        encounterRunAdversaryRepository.save(instance);

        auditLogger.log(AuditAction.ENCOUNTER_RUN_ADVERSARY_UPDATED,
                AuditContext.forUser(auth).withCampaignId(campaignIdOf(run)).build(),
                String.format("run_id: %d instance_id: %d (hp: %d/%d, stress: %d/%d, tokens: %d, defeated: %b)",
                        runId, instanceId, instance.getHitPointsMarked(), adversary.getHitPointMax(),
                        instance.getStressMarked(), adversary.getStressMax(), instance.getTokens(),
                        instance.getIsDefeated()));

        return toResponse(run, true);
    }

    /**
     * Marks a run complete.
     *
     * @param runId The run ID
     * @param auth The authentication object containing the current user
     * @return The completed run
     * @throws EntityNotFoundException if the run is not found
     * @throws InsufficientPermissionsException if the caller lacks access to the run
     * @throws IllegalStateException if the run is already completed, or its campaign has ended
     */
    @Transactional
    public EncounterRunResponse completeRun(Long runId, Authentication auth) {
        EncounterRun run = loadRun(runId);
        validateRunAccess(run, auth, "complete");

        if (run.getStatus() == EncounterRunStatus.COMPLETED) {
            throw new IllegalStateException("Encounter run is already completed");
        }
        if (run.getCampaign() != null) {
            campaignService.validateNotEnded(run.getCampaign(), "complete encounter runs for");
        }

        run.setStatus(EncounterRunStatus.COMPLETED);
        run.setEndedAt(LocalDateTime.now());
        EncounterRun updated = encounterRunRepository.save(run);

        auditLogger.log(AuditAction.ENCOUNTER_RUN_COMPLETED,
                AuditContext.forUser(auth).withCampaignId(campaignIdOf(run)).build(),
                "run_id: " + runId);

        return toResponse(updated, true);
    }

    /**
     * Permanently discards a run.
     * <p>
     * Unlike {@link #updateRunAdversary} and {@link #completeRun}, this does not check
     * {@link CampaignService#validateNotEnded} -- discarding is cleanup, not play, matching
     * {@code CampaignService#removeCharacterSheet}'s and {@code CountdownService#deleteCountdown}'s
     * treatment of deletion as always allowed.
     * </p>
     *
     * @param runId The run ID to discard
     * @param auth The authentication object containing the current user
     * @throws EntityNotFoundException if the run is not found
     * @throws InsufficientPermissionsException if the caller lacks access to the run
     */
    @Transactional
    public void deleteRun(Long runId, Authentication auth) {
        EncounterRun run = loadRun(runId);
        validateRunAccess(run, auth, "discard");

        encounterRunRepository.delete(run);

        auditLogger.log(AuditAction.ENCOUNTER_RUN_DELETED,
                AuditContext.forUser(auth).withCampaignId(campaignIdOf(run)).build(),
                "run_id: " + runId);
    }

    /**
     * Loads a run or fails.
     *
     * @param runId The run ID
     * @return The run
     * @throws EntityNotFoundException if no run has that ID
     */
    private EncounterRun loadRun(Long runId) {
        return encounterRunRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter run not found with id: " + runId));
    }

    /**
     * Validates that the caller has access to view or mutate a run, throwing if not.
     *
     * @param run The run being accessed
     * @param auth The authentication object containing the current user
     * @param operation The operation being performed, for the error message
     * @throws InsufficientPermissionsException if the caller lacks access
     */
    private void validateRunAccess(EncounterRun run, Authentication auth, String operation) {
        if (!hasRunAccess(run, auth)) {
            throw new InsufficientPermissionsException("You do not have permission to " + operation + " this encounter run");
        }
    }

    /**
     * Determines, without throwing, whether the caller may view or mutate a run.
     * <p>
     * A run is accessible to the user who started it, to any MODERATOR/ADMIN/OWNER regardless
     * of campaign tag, and -- only when the run is tagged to a campaign -- to that campaign's
     * game masters via {@link CampaignService#hasGameMasterAccess}. For a standalone run (no
     * campaign) this collapses to owner-or-moderator-only, which is what lets a user with no
     * campaign at all start and play a run.
     * </p>
     *
     * @param run The run to check access against
     * @param auth The authentication object containing the current user, may be null
     * @return true if the caller may view or mutate the run
     */
    private boolean hasRunAccess(EncounterRun run, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }

        if (run.getStartedBy().getId().equals(userDetails.getUserId())) {
            return true;
        }
        if (roleHierarchyService.hasModeratorOrHigher(userDetails)) {
            return true;
        }
        return run.getCampaign() != null && campaignService.hasGameMasterAccess(run.getCampaign(), auth);
    }

    /**
     * Clamps a value to the inclusive range {@code [min, max]}.
     *
     * @param value The value to clamp
     * @param min The inclusive lower bound
     * @param max The inclusive upper bound
     * @return {@code value}, or the nearer bound if it falls outside {@code [min, max]}
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * Extracts a run's campaign ID for audit context, or null for a standalone run.
     *
     * @param run The run
     * @return The campaign ID, or null
     */
    private Long campaignIdOf(EncounterRun run) {
        return run.getCampaign() != null ? run.getCampaign().getId() : null;
    }

    /**
     * Converts an EncounterRun entity to its response DTO.
     *
     * @param run The run entity
     * @param expandAdversary Whether to include each instance's full adversary stat block
     * @return The response DTO
     */
    private EncounterRunResponse toResponse(EncounterRun run, boolean expandAdversary) {
        // Only expanding requests batch-load features/experiences -- the unexpanded list
        // endpoint never touches these repositories at all.
        List<Long> adversaryIds = expandAdversary ? distinctAdversaryIds(run) : List.of();
        Map<Long, Set<Feature>> featuresByAdversaryId = loadFeaturesByAdversaryId(adversaryIds);
        Map<Long, Set<Experience>> experiencesByAdversaryId = loadExperiencesByAdversaryId(adversaryIds);

        EncounterRunResponse.EncounterRunResponseBuilder builder = EncounterRunResponse.builder()
                .id(run.getId())
                .encounterId(run.getEncounter().getId())
                .environmentId(environmentIdOf(run))
                .campaignId(campaignIdOf(run))
                .startedById(run.getStartedBy().getId())
                .status(run.getStatus())
                .startedAt(run.getStartedAt())
                .endedAt(run.getEndedAt())
                .createdAt(run.getCreatedAt())
                .lastModifiedAt(run.getLastModifiedAt());

        List<EncounterRunResponse.EncounterRunAdversaryResponse> adversaries =
                run.getEncounterRunAdversaries().stream()
                        .sorted(Comparator.comparing(EncounterRunAdversary::getDisplayOrder))
                        .map(instance -> toRunAdversaryResponse(
                                instance, expandAdversary, featuresByAdversaryId, experiencesByAdversaryId))
                        .toList();
        builder.adversaries(adversaries);

        return builder.build();
    }

    /**
     * Converts an EncounterRunAdversary entity to its nested response DTO.
     *
     * @param instance The run adversary instance
     * @param expandAdversary Whether to include the full adversary stat block
     * @param featuresByAdversaryId Each referenced adversary's features, keyed by adversary ID,
     *                              batch-loaded by the caller
     * @param experiencesByAdversaryId Each referenced adversary's experiences, keyed by
     *                                 adversary ID, batch-loaded by the caller
     * @return The nested response DTO
     */
    private EncounterRunResponse.EncounterRunAdversaryResponse toRunAdversaryResponse(
            EncounterRunAdversary instance, boolean expandAdversary,
            Map<Long, Set<Feature>> featuresByAdversaryId, Map<Long, Set<Experience>> experiencesByAdversaryId) {

        Adversary adversary = instance.getAdversary();

        EncounterRunResponse.EncounterRunAdversaryResponse.EncounterRunAdversaryResponseBuilder builder =
                EncounterRunResponse.EncounterRunAdversaryResponse.builder()
                        .id(instance.getId())
                        .adversaryId(adversary.getId())
                        .label(instance.getLabel())
                        .tierOverride(instance.getTierOverride())
                        .hitPointsMarked(instance.getHitPointsMarked())
                        .hitPointMax(adversary.getHitPointMax())
                        .stressMarked(instance.getStressMarked())
                        .stressMax(adversary.getStressMax())
                        .isDefeated(instance.getIsDefeated())
                        .note(instance.getNote())
                        .tokens(instance.getTokens())
                        .displayOrder(instance.getDisplayOrder());

        if (expandAdversary) {
            builder.adversary(toAdversaryStatBlock(
                    adversary,
                    featuresByAdversaryId.getOrDefault(adversary.getId(), Set.of()),
                    experiencesByAdversaryId.getOrDefault(adversary.getId(), Set.of())));
        }

        ImprovisedTierStatistics.forTier(instance.getTierOverride()).ifPresent(stats ->
                builder.retieredStatistics(EncounterResponse.RetieredStatisticsResponse.builder()
                        .tier(stats.tier())
                        .attackModifier(stats.attackModifier())
                        .difficulty(stats.difficulty())
                        .majorThreshold(stats.majorThreshold())
                        .severeThreshold(stats.severeThreshold())
                        .damageDiceRange(stats.damageDiceRange())
                        .build()));

        return builder.build();
    }

    /**
     * Builds an adversary's full stat block for the run view: enough for a GM to actually run
     * the fight, including its Features (passives/actions/reactions) and Experiences -- without
     * them, the GM has thresholds and a weapon line but none of the abilities that make the
     * adversary play differently from any other.
     * <p>
     * {@code features} and {@code experiences} are batch-loaded by the caller ({@link #toResponse})
     * rather than read off {@code adversary} directly, since {@link Adversary#getFeatures()} and
     * {@link Adversary#getExperiences()} are lazy {@code @ManyToMany} collections that a run with
     * many instances would otherwise trigger once per instance.
     * </p>
     *
     * @param adversary The catalog adversary
     * @param features The adversary's features, already loaded
     * @param experiences The adversary's experiences, already loaded
     * @return The adversary's stat block
     */
    private AdversaryResponse toAdversaryStatBlock(Adversary adversary, Set<Feature> features, Set<Experience> experiences) {
        AdversaryResponse.AdversaryResponseBuilder builder = AdversaryResponse.builder()
                .id(adversary.getId())
                .name(adversary.getName())
                .tier(adversary.getTier())
                .adversaryType(adversary.getAdversaryType())
                .description(adversary.getDescription())
                .motivesAndTactics(adversary.getMotivesAndTactics())
                .difficulty(adversary.getDifficulty())
                .majorThreshold(adversary.getMajorThreshold())
                .severeThreshold(adversary.getSevereThreshold())
                .hitPointMax(adversary.getHitPointMax())
                .stressMax(adversary.getStressMax())
                .attackModifier(adversary.getAttackModifier())
                .weaponName(adversary.getWeaponName())
                .attackRange(adversary.getAttackRange());

        if (adversary.getDamage() != null) {
            builder.damage(AdversaryResponse.DamageRollResponse.builder()
                    .diceCount(adversary.getDamage().getDiceCount())
                    .diceType(adversary.getDamage().getDiceType())
                    .modifier(adversary.getDamage().getModifier())
                    .damageType(adversary.getDamage().getDamageType())
                    .notation(adversary.getDamage().toNotation())
                    .build());
        }

        if (!experiences.isEmpty()) {
            builder.experienceIds(experiences.stream().map(Experience::getId).collect(Collectors.toSet()));
            builder.experiences(adversaryService.toExperienceResponses(experiences));
        }

        if (!features.isEmpty()) {
            builder.featureIds(features.stream().map(Feature::getId).collect(Collectors.toSet()));
            builder.features(adversaryService.toFeatureResponses(features, Set.of()));
        }

        return builder.build();
    }

    /**
     * Collects the distinct catalog adversary IDs referenced by a run's instances.
     * <p>
     * A run can hold multiple instances of the same adversary (e.g. three copies of the same
     * minion), so this dedupes before the batch-load queries in {@link #loadFeaturesByAdversaryId}
     * and {@link #loadExperiencesByAdversaryId} -- each distinct adversary is loaded once,
     * regardless of how many run instances reference it.
     * </p>
     *
     * @param run The run whose instances to scan
     * @return The distinct adversary IDs
     */
    private List<Long> distinctAdversaryIds(EncounterRun run) {
        return run.getEncounterRunAdversaries().stream()
                .map(instance -> instance.getAdversary().getId())
                .distinct()
                .toList();
    }

    /**
     * Batch-loads the given adversaries' features, keyed by adversary ID.
     *
     * @param adversaryIds The adversary IDs to load; an empty list short-circuits to no query
     * @return Each adversary's features, keyed by adversary ID
     */
    private Map<Long, Set<Feature>> loadFeaturesByAdversaryId(List<Long> adversaryIds) {
        if (adversaryIds.isEmpty()) {
            return Map.of();
        }
        return adversaryRepository.findAllByIdInWithFeatures(adversaryIds).stream()
                .collect(Collectors.toMap(Adversary::getId, Adversary::getFeatures));
    }

    /**
     * Batch-loads the given adversaries' experiences, keyed by adversary ID.
     *
     * @param adversaryIds The adversary IDs to load; an empty list short-circuits to no query
     * @return Each adversary's experiences, keyed by adversary ID
     */
    private Map<Long, Set<Experience>> loadExperiencesByAdversaryId(List<Long> adversaryIds) {
        if (adversaryIds.isEmpty()) {
            return Map.of();
        }
        return adversaryRepository.findAllByIdInWithExperiences(adversaryIds).stream()
                .collect(Collectors.toMap(Adversary::getId, Adversary::getExperiences));
    }

    /**
     * Extracts a run's source encounter's environment ID.
     *
     * @param run The run
     * @return The environment ID, or null if the encounter has no environment set
     */
    private Long environmentIdOf(EncounterRun run) {
        Environment environment = run.getEncounter().getEnvironment();
        return environment != null ? environment.getId() : null;
    }
}

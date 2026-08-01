package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCountdownRequest;
import com.aboff.core.model.dto.dh.request.UpdateCountdownRequest;
import com.aboff.core.model.dto.dh.request.UpdateCountdownValueRequest;
import com.aboff.core.model.dto.dh.response.CountdownResponse;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Countdown;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.model.enums.CountdownLoop;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.CountdownRepository;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.util.MarkdownSanitizerUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing a campaign's countdowns.
 * <p>
 * Given its own CRUD surface rather than being folded into {@link CampaignService}: like a
 * character's condition instance, a countdown carries per-row state and has an independent
 * lifecycle (it is renamed, ticked, and deleted on its own).
 * </p>
 * <p>
 * Access control: countdowns are GM-only state. Reads and writes alike require game
 * master-level access, which {@link CampaignService#hasGameMasterAccess} grants to the
 * campaign creator, any game master, and any MODERATOR/ADMIN/OWNER. That single definition
 * is delegated to rather than reimplemented here, so "is a GM" cannot drift between services.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CountdownService {

    private static final Logger log = LoggerFactory.getLogger(CountdownService.class);

    private final CountdownRepository countdownRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignService campaignService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves every countdown in a campaign, in display order.
     *
     * @param campaignId The campaign whose countdowns should be listed
     * @param auth The authentication object containing the current user
     * @return The campaign's countdowns, ordered for display
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user is not a GM of the campaign
     */
    @Transactional(readOnly = true)
    public List<CountdownResponse> getCountdownsForCampaign(Long campaignId, Authentication auth) {
        Campaign campaign = loadCampaign(campaignId);
        campaignService.validateGameMasterAccess(campaign, auth, "view countdowns for");

        return countdownRepository.findByCampaignId(campaignId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves a single countdown by ID.
     *
     * @param id The countdown ID
     * @param auth The authentication object containing the current user
     * @return The countdown
     * @throws EntityNotFoundException if the countdown is not found
     * @throws InsufficientPermissionsException if the user is not a GM of its campaign
     */
    @Transactional(readOnly = true)
    public CountdownResponse getCountdownById(Long id, Authentication auth) {
        Countdown countdown = loadCountdown(id);
        campaignService.validateGameMasterAccess(countdown.getCampaign(), auth, "view countdowns for");

        return toResponse(countdown);
    }

    /**
     * Creates a countdown in a campaign, appended to the end of its list.
     *
     * @param request The creation request
     * @param auth The authentication object containing the current user
     * @return The created countdown
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user is not a GM of the campaign
     * @throws IllegalStateException if the campaign has ended
     */
    @Transactional
    public CountdownResponse createCountdown(CreateCountdownRequest request, Authentication auth) {
        Campaign campaign = loadCampaign(request.getCampaignId());
        campaignService.validateGameMasterAccess(campaign, auth, "create countdowns for");
        campaignService.validateNotEnded(campaign, "create countdowns for");

        CountdownLoop loopBehavior =
                request.getLoopBehavior() == null ? CountdownLoop.NONE : request.getLoopBehavior();

        Countdown countdown = Countdown.builder()
                .campaign(campaign)
                .name(request.getName())
                .type(request.getType())
                .loopBehavior(loopBehavior)
                .startingValue(request.getStartingValue())
                .currentValue(request.getStartingValue())
                .note(sanitizeNote(request.getNote()))
                .displayOrder(nextDisplayOrder(campaign.getId()))
                .build();

        Countdown saved = countdownRepository.save(countdown);
        log.info("Created countdown {} ({}) in campaign {}", saved.getId(), saved.getType(), campaign.getId());

        auditLogger.log(AuditAction.CAMPAIGN_COUNTDOWN_CREATED,
                AuditContext.forUser(auth).withCampaignId(campaign.getId()).build(),
                String.format("countdown_id: %d \"%s\" (%s, starting at %d)",
                        saved.getId(), saved.getName(), saved.getType(), saved.getStartingValue()));

        return toResponse(saved);
    }

    /**
     * Updates a countdown's definition. The current value is untouched here — see
     * {@link #updateCountdownValue}.
     *
     * @param id The countdown ID to update
     * @param request The update request
     * @param auth The authentication object containing the current user
     * @return The updated countdown
     * @throws EntityNotFoundException if the countdown is not found
     * @throws InsufficientPermissionsException if the user is not a GM of its campaign
     * @throws IllegalStateException if the campaign has ended
     */
    @Transactional
    public CountdownResponse updateCountdown(Long id, UpdateCountdownRequest request, Authentication auth) {
        Countdown countdown = loadCountdown(id);
        Campaign campaign = countdown.getCampaign();
        campaignService.validateGameMasterAccess(campaign, auth, "update countdowns for");
        campaignService.validateNotEnded(campaign, "update countdowns for");

        countdown.setName(request.getName());
        countdown.setType(request.getType());
        countdown.setLoopBehavior(request.getLoopBehavior());
        countdown.setStartingValue(request.getStartingValue());
        countdown.setNote(sanitizeNote(request.getNote()));

        if (countdown.getCurrentValue() > request.getStartingValue()) {
            countdown.setCurrentValue(request.getStartingValue());
        }

        Countdown updated = countdownRepository.save(countdown);

        auditLogger.log(AuditAction.CAMPAIGN_COUNTDOWN_UPDATED,
                AuditContext.forUser(auth).withCampaignId(campaign.getId()).build(),
                String.format("countdown_id: %d \"%s\"", updated.getId(), updated.getName()));

        return toResponse(updated);
    }

    /**
     * Sets a countdown's remaining segments, applying its loop behaviour if this brings it to 0.
     * <p>
     * Takes an absolute value rather than a delta so that concurrent ticks resolve to
     * last-write-wins instead of compounding.
     * </p>
     *
     * @param id The countdown ID to tick
     * @param request The request carrying the new absolute current value
     * @param auth The authentication object containing the current user
     * @return The updated countdown, after any loop has been applied
     * @throws EntityNotFoundException if the countdown is not found
     * @throws InsufficientPermissionsException if the user is not a GM of its campaign
     * @throws IllegalStateException if the campaign has ended
     */
    @Transactional
    public CountdownResponse updateCountdownValue(
            Long id, UpdateCountdownValueRequest request, Authentication auth) {

        Countdown countdown = loadCountdown(id);
        Campaign campaign = countdown.getCampaign();
        campaignService.validateGameMasterAccess(campaign, auth, "update countdowns for");
        campaignService.validateNotEnded(campaign, "update countdowns for");

        countdown.setCurrentValue(Math.min(request.getCurrentValue(), countdown.getStartingValue()));

        boolean triggered = countdown.getCurrentValue() == 0;
        if (triggered) {
            countdown.applyLoop();
            log.info("Countdown {} triggered in campaign {} (loop: {})",
                    countdown.getId(), campaign.getId(), countdown.getLoopBehavior());
        }

        Countdown updated = countdownRepository.save(countdown);

        auditLogger.log(AuditAction.CAMPAIGN_COUNTDOWN_UPDATED,
                AuditContext.forUser(auth).withCampaignId(campaign.getId()).build(),
                String.format("countdown_id: %d \"%s\" now at %d%s",
                        updated.getId(), updated.getName(), updated.getCurrentValue(),
                        triggered ? " (triggered)" : ""));

        return toResponse(updated);
    }

    /**
     * Permanently removes a countdown.
     *
     * @param id The countdown ID to delete
     * @param auth The authentication object containing the current user
     * @throws EntityNotFoundException if the countdown is not found
     * @throws InsufficientPermissionsException if the user is not a GM of its campaign
     */
    @Transactional
    public void deleteCountdown(Long id, Authentication auth) {
        Countdown countdown = loadCountdown(id);
        Campaign campaign = countdown.getCampaign();
        campaignService.validateGameMasterAccess(campaign, auth, "delete countdowns for");

        countdownRepository.delete(countdown);

        auditLogger.log(AuditAction.CAMPAIGN_COUNTDOWN_DELETED,
                AuditContext.forUser(auth).withCampaignId(campaign.getId()).build(),
                String.format("countdown_id: %d \"%s\"", id, countdown.getName()));
    }

    /**
     * Loads an active campaign or fails.
     *
     * @param campaignId The campaign ID
     * @return The campaign
     * @throws EntityNotFoundException if no active campaign has that ID
     */
    private Campaign loadCampaign(Long campaignId) {
        return campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));
    }

    /**
     * Loads a countdown or fails.
     *
     * @param id The countdown ID
     * @return The countdown
     * @throws EntityNotFoundException if no countdown has that ID
     */
    private Countdown loadCountdown(Long id) {
        return countdownRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Countdown not found with id: " + id));
    }

    /**
     * Sanitizes a GM-authored note, which is optional.
     *
     * @param note The raw note, may be null
     * @return The sanitized note, or null if none was supplied
     */
    private String sanitizeNote(String note) {
        return note == null ? null : MarkdownSanitizerUtil.sanitize(note);
    }

    /**
     * Determines the display order for a countdown being appended to a campaign's list.
     *
     * @param campaignId The campaign gaining a countdown
     * @return One past the campaign's current highest display order, or 0 if it has none
     */
    private int nextDisplayOrder(Long campaignId) {
        return countdownRepository.findMaxDisplayOrderByCampaignId(campaignId) + 1;
    }

    /**
     * Converts a Countdown entity to its response DTO.
     *
     * @param countdown The countdown entity
     * @return The response DTO
     */
    private CountdownResponse toResponse(Countdown countdown) {
        return CountdownResponse.builder()
                .id(countdown.getId())
                .campaignId(countdown.getCampaign().getId())
                .name(countdown.getName())
                .type(countdown.getType())
                .loopBehavior(countdown.getLoopBehavior())
                .startingValue(countdown.getStartingValue())
                .currentValue(countdown.getCurrentValue())
                .note(countdown.getNote())
                .displayOrder(countdown.getDisplayOrder())
                .createdAt(countdown.getCreatedAt())
                .lastModifiedAt(countdown.getLastModifiedAt())
                .build();
    }
}

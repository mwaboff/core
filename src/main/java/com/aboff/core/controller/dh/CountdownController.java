package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCountdownRequest;
import com.aboff.core.model.dto.dh.request.UpdateCountdownRequest;
import com.aboff.core.model.dto.dh.request.UpdateCountdownValueRequest;
import com.aboff.core.model.dto.dh.response.CountdownResponse;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.CountdownService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing a campaign's countdowns.
 * <p>
 * Access control: every endpoint, reads included, requires game master-level access to the
 * owning campaign — countdowns are GM-only state. Enforced in the service layer.
 * </p>
 */
@RestController
@RequestMapping("/api/dh/countdowns")
@RequiredArgsConstructor
public class CountdownController {

    private static final String BASE_PATH = "/api/dh/countdowns";

    private final CountdownService countdownService;
    private final AuditLogger auditLogger;

    /**
     * Lists a campaign's countdowns in display order.
     *
     * @param campaignId The campaign whose countdowns should be listed
     * @param authentication The authenticated user
     * @return The campaign's countdowns
     */
    @GetMapping
    public ResponseEntity<List<CountdownResponse>> getCountdownsForCampaign(
            @RequestParam Long campaignId,
            Authentication authentication) {

        return ResponseEntity.ok(countdownService.getCountdownsForCampaign(campaignId, authentication));
    }

    /**
     * Retrieves a single countdown.
     *
     * @param id The countdown ID
     * @param authentication The authenticated user
     * @return The countdown
     */
    @GetMapping("/{id}")
    public ResponseEntity<CountdownResponse> getCountdownById(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(countdownService.getCountdownById(id, authentication));
    }

    /**
     * Creates a countdown in a campaign.
     *
     * @param request The creation request
     * @param authentication The authenticated user
     * @param httpRequest The servlet request, for audit context
     * @return The created countdown
     */
    @PostMapping
    public ResponseEntity<CountdownResponse> createCountdown(
            @Valid @RequestBody CreateCountdownRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", BASE_PATH);

        CountdownResponse response = countdownService.createCountdown(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", BASE_PATH, startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates a countdown's definition.
     *
     * @param id The countdown ID to update
     * @param request The update request
     * @param authentication The authenticated user
     * @param httpRequest The servlet request, for audit context
     * @return The updated countdown
     */
    @PutMapping("/{id}")
    public ResponseEntity<CountdownResponse> updateCountdown(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCountdownRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", BASE_PATH + "/" + id);

        CountdownResponse response = countdownService.updateCountdown(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", BASE_PATH + "/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Ticks a countdown to an absolute value, applying its loop behaviour if it reaches 0.
     *
     * @param id The countdown ID to tick
     * @param request The request carrying the new current value
     * @param authentication The authenticated user
     * @param httpRequest The servlet request, for audit context
     * @return The updated countdown
     */
    @PatchMapping("/{id}/value")
    public ResponseEntity<CountdownResponse> updateCountdownValue(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCountdownValueRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PATCH", BASE_PATH + "/" + id + "/value");

        CountdownResponse response = countdownService.updateCountdownValue(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PATCH", BASE_PATH + "/" + id + "/value", startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a countdown.
     *
     * @param id The countdown ID to delete
     * @param authentication The authenticated user
     * @param httpRequest The servlet request, for audit context
     * @return An empty 204 response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCountdown(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", BASE_PATH + "/" + id);

        countdownService.deleteCountdown(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", BASE_PATH + "/" + id, startTime);
        return ResponseEntity.noContent().build();
    }
}

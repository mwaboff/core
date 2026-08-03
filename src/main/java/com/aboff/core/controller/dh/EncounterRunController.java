package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateEncounterRunRequest;
import com.aboff.core.model.dto.dh.request.UpdateEncounterRunAdversaryRequest;
import com.aboff.core.model.dto.dh.response.EncounterRunResponse;
import com.aboff.core.model.enums.EncounterRunStatus;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.EncounterRunService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for running encounters: starting a run, tracking live per-instance state, and
 * completing or discarding it.
 * <p>
 * Runs are deliberately top-level ({@code /api/dh/encounter-runs/...}) rather than nested under
 * campaigns -- a nested path would imply a campaign is required, and running a fight is
 * campaign-free by design. Only starting a run is nested under its source encounter
 * ({@code POST /api/dh/encounters/{id}/runs}), since that is the one action that always has an
 * encounter as its subject.
 * </p>
 * <p>
 * Access control is enforced in the service layer: a run is visible and mutable to whoever
 * started it, plus -- only when it is tagged to a campaign -- that campaign's game masters, plus
 * any MODERATOR/ADMIN/OWNER regardless of campaign tag.
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class EncounterRunController {

    private static final String RUNS_PATH = "/api/dh/encounter-runs";

    private final EncounterRunService encounterRunService;
    private final AuditLogger auditLogger;

    /**
     * Starts a run of an encounter, snapshotting its current adversary instances.
     *
     * @param encounterId The encounter to run
     * @param request The start request; an empty or absent body starts a standalone run
     * @param authentication The authenticated user
     * @param httpRequest The servlet request, for audit context
     * @return The newly started run
     */
    @PostMapping("/api/dh/encounters/{encounterId}/runs")
    public ResponseEntity<EncounterRunResponse> startRun(
            @PathVariable Long encounterId,
            @RequestBody(required = false) CreateEncounterRunRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        String path = "/api/dh/encounters/" + encounterId + "/runs";
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", path);

        EncounterRunResponse response = encounterRunService.startRun(encounterId, request, authentication);

        auditLogger.requestCompleted(ctx, "POST", path, startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a single run, with every instance's full adversary stat block expanded.
     *
     * @param runId The run ID
     * @param authentication The authenticated user
     * @return The run
     */
    @GetMapping(RUNS_PATH + "/{runId}")
    public ResponseEntity<EncounterRunResponse> getRun(
            @PathVariable Long runId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", RUNS_PATH + "/" + runId);

        EncounterRunResponse response = encounterRunService.getRun(runId, authentication);

        auditLogger.requestCompleted(ctx, "GET", RUNS_PATH + "/" + runId, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists the runs visible to the caller.
     * <p>
     * Omitting {@code campaignId} lists the caller's own runs; providing it lists that
     * campaign's tagged runs (requires game master access to that campaign).
     * </p>
     *
     * @param status Optional status filter
     * @param campaignId Optional campaign filter
     * @param authentication The authenticated user
     * @return The matching runs, newest first
     */
    @GetMapping(RUNS_PATH)
    public ResponseEntity<List<EncounterRunResponse>> listRuns(
            @RequestParam(required = false) EncounterRunStatus status,
            @RequestParam(required = false) Long campaignId,
            Authentication authentication) {

        return ResponseEntity.ok(encounterRunService.listRuns(status, campaignId, authentication));
    }

    /**
     * Updates a single adversary instance's live state within a run: marked HP/Stress, defeated,
     * and/or note. Every provided field is an absolute value, not a delta.
     *
     * @param runId The run ID
     * @param instanceId The run adversary instance ID to update
     * @param request The fields to update; a null field is left unchanged
     * @param authentication The authenticated user
     * @param httpRequest The servlet request, for audit context
     * @return The updated run
     */
    @PatchMapping(RUNS_PATH + "/{runId}/adversaries/{instanceId}")
    public ResponseEntity<EncounterRunResponse> updateRunAdversary(
            @PathVariable Long runId,
            @PathVariable Long instanceId,
            @Valid @RequestBody UpdateEncounterRunAdversaryRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        String path = RUNS_PATH + "/" + runId + "/adversaries/" + instanceId;
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PATCH", path);

        EncounterRunResponse response = encounterRunService.updateRunAdversary(runId, instanceId, request, authentication);

        auditLogger.requestCompleted(ctx, "PATCH", path, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Marks a run complete.
     *
     * @param runId The run ID
     * @param authentication The authenticated user
     * @param httpRequest The servlet request, for audit context
     * @return The completed run
     */
    @PostMapping(RUNS_PATH + "/{runId}/complete")
    public ResponseEntity<EncounterRunResponse> completeRun(
            @PathVariable Long runId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        String path = RUNS_PATH + "/" + runId + "/complete";
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", path);

        EncounterRunResponse response = encounterRunService.completeRun(runId, authentication);

        auditLogger.requestCompleted(ctx, "POST", path, startTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Permanently discards a run.
     *
     * @param runId The run ID to discard
     * @param authentication The authenticated user
     * @param httpRequest The servlet request, for audit context
     * @return 204 No Content on success
     */
    @DeleteMapping(RUNS_PATH + "/{runId}")
    public ResponseEntity<Void> deleteRun(
            @PathVariable Long runId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", RUNS_PATH + "/" + runId);

        encounterRunService.deleteRun(runId, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", RUNS_PATH + "/" + runId, startTime);
        return ResponseEntity.noContent().build();
    }
}

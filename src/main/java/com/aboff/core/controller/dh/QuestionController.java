package com.aboff.core.controller.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateQuestionRequest;
import com.aboff.core.model.dto.dh.request.UpdateQuestionRequest;
import com.aboff.core.model.dto.dh.response.QuestionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.QuestionType;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.dh.QuestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing Question resources.
 */
@RestController
@RequestMapping("/api/dh/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final AuditLogger auditLogger;

    @GetMapping
    public ResponseEntity<PagedResponse<QuestionResponse>> getAllQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) Long expansionId,
            @RequestParam(required = false) QuestionType questionType,
            @RequestParam(required = false) String expand) {

        PagedResponse<QuestionResponse> response = questionService.getAllQuestions(
                page, size, includeDeleted, expansionId, questionType, expand);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> getQuestionById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/dh/questions/" + id);

        QuestionResponse response = questionService.getQuestionById(id, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/dh/questions/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<QuestionResponse> createQuestion(
            @Valid @RequestBody CreateQuestionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/questions");

        QuestionResponse response = questionService.createQuestion(request, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/questions", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<QuestionResponse>> createQuestionsBulk(
            @Valid @RequestBody List<CreateQuestionRequest> requests,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/questions/bulk");

        List<QuestionResponse> responses = questionService.createQuestionsBulk(requests, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/questions/bulk", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuestionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "PUT", "/api/dh/questions/" + id);

        QuestionResponse response = questionService.updateQuestion(id, request, authentication);

        auditLogger.requestCompleted(ctx, "PUT", "/api/dh/questions/" + id, startTime);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "DELETE", "/api/dh/questions/" + id);

        questionService.deleteQuestion(id, authentication);

        auditLogger.requestCompleted(ctx, "DELETE", "/api/dh/questions/" + id, startTime);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<QuestionResponse> restoreQuestion(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication).withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/dh/questions/" + id + "/restore");

        QuestionResponse response = questionService.restoreQuestion(id, authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/dh/questions/" + id + "/restore", startTime);
        return ResponseEntity.ok(response);
    }
}

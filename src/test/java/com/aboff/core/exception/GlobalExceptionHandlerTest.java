package com.aboff.core.exception;

import com.aboff.core.model.dto.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * <p>
 * Covers the constraint-violation mapping. The unique index behind the feature find-or-create
 * key is what stops two concurrent requests from both inserting the same row, and the losing
 * request needs an answer it can act on: a 409 says the conflict is transient and the request
 * can be retried, where the previous unhandled 500 said nothing.
 * </p>
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        return request;
    }

    @Test
    void handleDataIntegrityViolation_ReturnsConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate key value violates unique constraint"),
                request("/api/dh/weapons/custom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleDataIntegrityViolation_TellsTheCallerToRetry() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate key value violates unique constraint"),
                request("/api/dh/weapons/custom"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("retry");
    }

    @Test
    void handleDataIntegrityViolation_DoesNotLeakTheConstraintNameToTheCaller() {
        // The constraint name is a schema detail. It is logged, not returned.
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("violates unique constraint \"uq_features_dedupe_key\""),
                request("/api/dh/weapons/custom"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).doesNotContain("uq_features_dedupe_key");
        assertThat(response.getBody().getPath()).isEqualTo("/api/dh/weapons/custom");
    }
}

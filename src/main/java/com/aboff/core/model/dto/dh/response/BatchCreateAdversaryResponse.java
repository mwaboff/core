package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for batch adversary creation operations.
 * Contains both successfully created adversaries and any errors that occurred.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchCreateAdversaryResponse {

    /**
     * List of successfully created adversaries.
     */
    private List<AdversaryResponse> created;

    /**
     * List of errors for adversaries that failed to create.
     */
    private List<BatchError> errors;

    /**
     * Total number of adversaries requested to be created.
     */
    private int totalRequested;

    /**
     * Number of adversaries successfully created.
     */
    private int totalCreated;

    /**
     * Number of adversaries that failed to create.
     */
    private int totalFailed;

    /**
     * Represents an error that occurred during batch creation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchError {

        /**
         * The index (0-based) of the adversary in the original request.
         */
        private int index;

        /**
         * The name of the adversary that failed (for easier identification).
         */
        private String name;

        /**
         * Description of the error that occurred.
         */
        private String error;
    }
}

package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch creating multiple Adversaries.
 * Supports partial success - individual failures do not roll back successful creates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreateAdversaryRequest {

    /**
     * List of adversaries to create.
     * Must contain at least one adversary and at most 100.
     */
    @NotEmpty(message = "At least one adversary is required")
    @Size(max = 100, message = "Maximum 100 adversaries per batch")
    @Valid
    private List<CreateAdversaryRequest> adversaries;
}

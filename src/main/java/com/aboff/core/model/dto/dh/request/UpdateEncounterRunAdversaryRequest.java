package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating a single adversary instance's live state within an encounter run.
 * <p>
 * Partial update: a null field is left unchanged, matching {@code UpdateEncounterRequest}. Where
 * a field is provided it is applied as an <strong>absolute</strong> value, never a delta --
 * matching {@code UpdateCountdownValueRequest} and {@code UpdateCampaignFearRequest} -- so a
 * fast-clicking GM or two open tabs resolve to last-write-wins instead of compounding. There is
 * no optimistic locking anywhere in this codebase; this convention is how concurrency is handled
 * instead.
 * </p>
 * <p>
 * {@code hitPointsMarked} and {@code stressMarked} are only floor-validated here (must not be
 * negative); the ceiling depends on the adversary's own {@code hitPointMax}/{@code stressMax}
 * and is enforced by the service, which clamps rather than rejects. {@code tokens} is also only
 * floor-validated -- unlike HP/Stress it has no ceiling to clamp against at all, since a Pool
 * (Hope &amp; Fear) can hold any number of tokens.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEncounterRunAdversaryRequest {

    @Min(value = 0, message = "Hit points marked must be at least 0")
    private Integer hitPointsMarked;

    @Min(value = 0, message = "Stress marked must be at least 0")
    private Integer stressMarked;

    private Boolean isDefeated;

    @Size(max = 2000, message = "Note must not exceed 2000 characters")
    private String note;

    /**
     * Tokens placed on this instance's stat block (Daggerheart Core ch. 4, "Adversary Tokens").
     * Absolute value, never a delta -- same convention as {@link #hitPointsMarked}/
     * {@link #stressMarked}. Not clamped to any maximum.
     */
    @Min(value = 0, message = "Tokens must be at least 0")
    private Integer tokens;
}

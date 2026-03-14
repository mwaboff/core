package com.aboff.core.model.dto.dh.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for trading domain cards during a level-up.
 * <p>
 * Represents an equal-swap trade where a character gives up one or more domain cards
 * and receives the same number of replacement cards. The count of traded-out cards
 * must match the count of traded-in cards.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainCardTradeRequest {

    /**
     * IDs of the domain cards to give up. Must not be empty.
     */
    @NotEmpty(message = "Traded out domain card IDs are required")
    private List<Long> tradedOutDomainCardIds;

    /**
     * IDs of the domain cards to receive. Must not be empty and must match the count
     * of {@code tradedOutDomainCardIds}.
     */
    @NotEmpty(message = "Traded in domain card IDs are required")
    private List<Long> tradedInDomainCardIds;

    /**
     * Optional subset of {@code tradedInDomainCardIds} to immediately equip.
     */
    private List<Long> equipTradedInCardIds;
}

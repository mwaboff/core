package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a domain card trade during level-up.
 * <p>
 * Trades allow exchanging owned domain cards for new ones from accessible domains.
 * The number of cards traded out must equal the number traded in.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainCardTradeRequest {

    /**
     * IDs of domain cards to trade away (remove from character).
     */
    @NotEmpty(message = "Trade-out card IDs are required")
    private List<Long> tradeOutCardIds;

    /**
     * IDs of domain cards to receive (add to character).
     */
    @NotEmpty(message = "Trade-in card IDs are required")
    private List<Long> tradeInCardIds;

    /**
     * IDs of traded-in cards that should be equipped.
     */
    private List<Long> equipTradedInCardIds;
}

package com.aboff.core.model.dto.dh;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single Seraph Prayer Die: the face value rolled and whether it has been spent.
 * <p>
 * Used by both the character sheet update request and the character sheet response, so the API
 * contract stays structured even though the sheet persists the dice as one encoded string (see
 * {@link com.aboff.core.service.dh.PrayerDiceCodec}).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrayerDieDto {

    /**
     * The face value rolled on this d4. An omitted value deserializes to 0 and is rejected by the
     * lower bound, so the die always carries a real roll.
     */
    @Min(value = 1, message = "Prayer die value must be at least 1")
    @Max(value = 4, message = "Prayer die value must not exceed 4")
    private int value;

    /**
     * Whether the die has already been spent this session. Spent dice keep their value on the
     * sheet so the player can still read what each one was worth until the next reset.
     */
    private boolean spent;
}

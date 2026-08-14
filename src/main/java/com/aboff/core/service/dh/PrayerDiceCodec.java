package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.PrayerDieDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Translates the Seraph Prayer Dice between the structured API shape and the single string the
 * {@code character_sheets.prayer_dice} column stores.
 * <p>
 * The encoded form is the face values in roll order, comma separated, with a {@code *} suffix
 * marking a die that has been spent:
 * </p>
 * <pre>
 * "3,1*,4,2"   four dice; the 1 has been spent
 * null or ""   no dice rolled this session
 * </pre>
 * <p>
 * This class is the only place in the application that knows that format. Parsing is deliberately
 * total: a stored value that does not decode yields an empty list rather than an exception, so a
 * corrupt or hand-edited column can never stop a character sheet from loading.
 * </p>
 */
public final class PrayerDiceCodec {

    private static final Logger log = LoggerFactory.getLogger(PrayerDiceCodec.class);

    /** Separates one encoded die from the next. */
    private static final String SEPARATOR = ",";

    /** Suffix appended to a die that has been spent. */
    private static final String SPENT_MARKER = "*";

    /** Lowest face a d4 can show. */
    private static final int MIN_VALUE = 1;

    /** Highest face a d4 can show. */
    private static final int MAX_VALUE = 4;

    /**
     * Most dice a single encoded value may hold. Matches the bound enforced on the update request
     * and keeps the encoded string comfortably inside the column's 64 characters.
     */
    private static final int MAX_DICE = 16;

    private PrayerDiceCodec() {
    }

    /**
     * Decodes the stored column value into the structured dice the API exposes.
     *
     * @param encoded the stored value, may be null or empty
     * @return the dice in roll order; an empty list when the input is null, empty, or does not
     *         decode cleanly
     */
    public static List<PrayerDieDto> parse(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return List.of();
        }

        String[] tokens = encoded.split(SEPARATOR, -1);
        if (tokens.length > MAX_DICE) {
            log.warn("Ignoring prayer dice value with {} entries, more than the {} allowed", tokens.length, MAX_DICE);
            return List.of();
        }

        List<PrayerDieDto> dice = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            PrayerDieDto die = parseDie(token);
            if (die == null) {
                log.warn("Ignoring unparseable prayer dice value: '{}'", encoded);
                return List.of();
            }
            dice.add(die);
        }
        return dice;
    }

    /**
     * Encodes the dice for storage.
     *
     * @param dice the dice in roll order, may be null or empty
     * @return the encoded value, or null when there are no dice so the column stays NULL
     */
    public static String format(List<PrayerDieDto> dice) {
        if (dice == null || dice.isEmpty()) {
            return null;
        }
        return dice.stream()
                .map(die -> die.getValue() + (die.isSpent() ? SPENT_MARKER : ""))
                .collect(Collectors.joining(SEPARATOR));
    }

    /**
     * Decodes a single token such as {@code "3"} or {@code "1*"}.
     *
     * @param token one comma-separated entry from the encoded value
     * @return the die, or null if the token is not a d4 face optionally followed by the spent marker
     */
    private static PrayerDieDto parseDie(String token) {
        String trimmed = token.trim();
        boolean spent = trimmed.endsWith(SPENT_MARKER);
        String face = spent ? trimmed.substring(0, trimmed.length() - SPENT_MARKER.length()) : trimmed;

        int value;
        try {
            value = Integer.parseInt(face.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        if (value < MIN_VALUE || value > MAX_VALUE) {
            return null;
        }
        return PrayerDieDto.builder().value(value).spent(spent).build();
    }
}

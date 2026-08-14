package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.PrayerDieDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PrayerDiceCodecTest {

    private static PrayerDieDto die(int value, boolean spent) {
        return PrayerDieDto.builder().value(value).spent(spent).build();
    }

    // ==================== PARSE ====================

    @Test
    void parse_Null_ReturnsEmptyList() {
        // Act
        List<PrayerDieDto> result = PrayerDiceCodec.parse(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void parse_EmptyString_ReturnsEmptyList() {
        // Act
        List<PrayerDieDto> result = PrayerDiceCodec.parse("");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void parse_WhitespaceOnly_ReturnsEmptyList() {
        // Act
        List<PrayerDieDto> result = PrayerDiceCodec.parse("   ");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void parse_SingleReadyDie_ReturnsOneUnspentDie() {
        // Act
        List<PrayerDieDto> result = PrayerDiceCodec.parse("3");

        // Assert
        assertThat(result).containsExactly(die(3, false));
    }

    @Test
    void parse_AllReady_ReturnsEveryDieUnspent() {
        // Act
        List<PrayerDieDto> result = PrayerDiceCodec.parse("1,2,3,4");

        // Assert
        assertThat(result).containsExactly(die(1, false), die(2, false), die(3, false), die(4, false));
    }

    @Test
    void parse_AllSpent_ReturnsEveryDieSpent() {
        // Act
        List<PrayerDieDto> result = PrayerDiceCodec.parse("1*,2*,3*");

        // Assert
        assertThat(result).containsExactly(die(1, true), die(2, true), die(3, true));
    }

    @Test
    void parse_MixedSpentAndReady_PreservesRollOrderAndSpentFlags() {
        // Act
        List<PrayerDieDto> result = PrayerDiceCodec.parse("3,1*,4,2");

        // Assert
        assertThat(result).containsExactly(die(3, false), die(1, true), die(4, false), die(2, false));
    }

    @Test
    void parse_SurroundingWhitespace_IsTolerated() {
        // Act
        List<PrayerDieDto> result = PrayerDiceCodec.parse(" 3 , 1* ");

        // Assert
        assertThat(result).containsExactly(die(3, false), die(1, true));
    }

    @Test
    void parse_MaximumDiceCount_IsAccepted() {
        // Arrange - 16 dice is the bound the update request enforces
        String encoded = String.join(",", Collections.nCopies(16, "4"));

        // Act
        List<PrayerDieDto> result = PrayerDiceCodec.parse(encoded);

        // Assert
        assertThat(result).hasSize(16);
    }

    @Test
    void parse_MoreDiceThanAllowed_ReturnsEmptyList() {
        // Arrange
        String encoded = String.join(",", Collections.nCopies(17, "4"));

        // Act
        List<PrayerDieDto> result = PrayerDiceCodec.parse(encoded);

        // Assert
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",          // not a number
            "3,,4",         // empty entry
            "0",            // below a d4's lowest face
            "5",            // above a d4's highest face
            "-1",           // negative
            "3;1",          // wrong separator
            "3,x*",         // one bad entry poisons the whole value
            "*",            // marker with no face
            "3**",          // doubled marker
            "3.5",          // not an integer
            "999999999999"  // overflows int
    })
    void parse_MalformedInput_ReturnsEmptyListWithoutThrowing(String encoded) {
        // Act
        List<PrayerDieDto> result = PrayerDiceCodec.parse(encoded);

        // Assert
        assertThat(result).isEmpty();
    }

    // ==================== FORMAT ====================

    @Test
    void format_Null_ReturnsNullSoTheColumnStaysEmpty() {
        // Act
        String result = PrayerDiceCodec.format(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void format_EmptyList_ReturnsNullSoTheColumnStaysEmpty() {
        // Act
        String result = PrayerDiceCodec.format(List.of());

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void format_AllReady_OmitsTheSpentMarker() {
        // Act
        String result = PrayerDiceCodec.format(List.of(die(1, false), die(4, false)));

        // Assert
        assertThat(result).isEqualTo("1,4");
    }

    @Test
    void format_AllSpent_MarksEveryDie() {
        // Act
        String result = PrayerDiceCodec.format(List.of(die(2, true), die(3, true)));

        // Assert
        assertThat(result).isEqualTo("2*,3*");
    }

    @Test
    void format_Mixed_MarksOnlySpentDice() {
        // Act
        String result = PrayerDiceCodec.format(List.of(die(3, false), die(1, true), die(4, false), die(2, false)));

        // Assert
        assertThat(result).isEqualTo("3,1*,4,2");
    }

    @Test
    void format_MaximumDiceCount_FitsTheColumnLength() {
        // Arrange - every die spent is the longest an encoded value can be
        List<PrayerDieDto> dice = IntStream.range(0, 16).mapToObj(i -> die(4, true)).toList();

        // Act
        String result = PrayerDiceCodec.format(dice);

        // Assert
        assertThat(result).hasSizeLessThanOrEqualTo(64);
    }

    // ==================== ROUND TRIP ====================

    @Test
    void roundTrip_MixedDice_ReturnsTheOriginalValue() {
        // Arrange
        List<PrayerDieDto> dice = List.of(die(3, false), die(1, true), die(4, false), die(2, true));

        // Act
        List<PrayerDieDto> result = PrayerDiceCodec.parse(PrayerDiceCodec.format(dice));

        // Assert
        assertThat(result).isEqualTo(dice);
    }
}

package com.aboff.core.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PostgresArrayUtil}.
 *
 * <p>The literal these tests pin down is bound straight into a {@code CAST(? AS bigint[])} in
 * the search access clause, so its exact shape is load-bearing: a malformed literal is a
 * runtime SQL error, and an over-broad one would widen who can see campaign-shared items.
 */
class PostgresArrayUtilTest {

    @Test
    void toBigintArrayLiteral_WithNull_ReturnsEmptyArrayLiteral() {
        assertThat(PostgresArrayUtil.toBigintArrayLiteral(null)).isEqualTo("{}");
    }

    @Test
    void toBigintArrayLiteral_WithEmptyCollection_ReturnsEmptyArrayLiteral() {
        // An empty array overlaps nothing, which is what a user in no campaigns must match.
        assertThat(PostgresArrayUtil.toBigintArrayLiteral(List.of())).isEqualTo("{}");
    }

    @Test
    void toBigintArrayLiteral_WithSingleId_WrapsItInBraces() {
        assertThat(PostgresArrayUtil.toBigintArrayLiteral(List.of(42L))).isEqualTo("{42}");
    }

    @Test
    void toBigintArrayLiteral_WithMultipleIds_JoinsWithCommasAndNoSpaces() {
        assertThat(PostgresArrayUtil.toBigintArrayLiteral(List.of(3L, 7L, 11L))).isEqualTo("{3,7,11}");
    }

    @Test
    void toBigintArrayLiteral_PreservesCollectionOrder() {
        assertThat(PostgresArrayUtil.toBigintArrayLiteral(List.of(11L, 3L, 7L))).isEqualTo("{11,3,7}");
    }

    @Test
    void toBigintArrayLiteral_WithNegativeId_RendersTheSign() {
        // ItemAccessService binds -1 as its "matches nothing" sentinel, so the sign must survive.
        assertThat(PostgresArrayUtil.toBigintArrayLiteral(List.of(-1L))).isEqualTo("{-1}");
    }

    @Test
    void toBigintArrayLiteral_WithNullElement_SkipsIt() {
        // A null would render as the four characters "null", which is not a valid bigint.
        List<Long> ids = new ArrayList<>();
        ids.add(5L);
        ids.add(null);
        ids.add(9L);

        assertThat(PostgresArrayUtil.toBigintArrayLiteral(ids)).isEqualTo("{5,9}");
    }
}

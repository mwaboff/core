package com.aboff.core.util;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class for rendering Java ID collections as PostgreSQL array literals.
 * <p>
 * Hibernate expands a bound {@link Collection} into a comma-separated list of placeholders,
 * which is what an {@code IN (:ids)} clause needs and exactly wrong for an array-valued
 * parameter — {@code CAST(?,?,? AS bigint[])} is a syntax error. Rendering the collection
 * to its literal text form up front and letting PostgreSQL cast it
 * ({@code CAST(:ids AS bigint[])}) keeps the parameter a single scalar bind.
 * </p>
 * <p>
 * The literal is always non-null: an absent or empty collection becomes {@code "{}"}. That
 * matters for the overlap operator — an empty array overlaps nothing, so a caller who
 * belongs to no campaigns matches no campaign-shared rows rather than all of them.
 * </p>
 */
public final class PostgresArrayUtil {

    /** The PostgreSQL literal for an empty array. */
    public static final String EMPTY_ARRAY_LITERAL = "{}";

    private PostgresArrayUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Renders a collection of IDs as a PostgreSQL {@code bigint[]} literal.
     * <p>
     * Example: {@code List.of(3L, 7L)} becomes {@code "{3,7}"}.
     * </p>
     * <p>
     * Null elements are skipped rather than emitted as {@code NULL}, since a null ID cannot
     * match any row and {@code NULL} inside the literal would only muddy the comparison.
     * </p>
     *
     * @param ids the IDs to render; may be null or empty
     * @return the array literal, never null; {@code "{}"} when there is nothing to render
     */
    public static String toBigintArrayLiteral(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return EMPTY_ARRAY_LITERAL;
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(",", "{", "}"));
    }
}

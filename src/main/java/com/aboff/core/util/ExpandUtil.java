package com.aboff.core.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for parsing expand parameters in API responses.
 * <p>
 * Expand parameters allow API consumers to request related entities
 * to be included in the response, reducing the number of API calls needed.
 * Use the special value {@code "all"} to expand all available relationships.
 * </p>
 */
public final class ExpandUtil {

    /** Special expand value that causes all available relationships to be expanded. */
    public static final String EXPAND_ALL = "all";

    private ExpandUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Parses a comma-separated expand parameter into a set of relationship names.
     * <p>
     * Example: "owner,experiences,inventoryWeapons" becomes
     * Set.of("owner", "experiences", "inventoryWeapons").
     * Pass {@code "all"} to expand every available relationship.
     * </p>
     *
     * @param expand Comma-separated list of relationships to expand, may be null or empty
     * @return Set of relationship names to expand, empty set if input is null or empty
     */
    public static Set<String> parseExpand(String expand) {
        if (expand == null || expand.trim().isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(List.of(expand.split(",")));
    }

    /**
     * Checks whether a specific field should be expanded based on the expand set.
     * Returns true if the field is explicitly in the set OR if {@code "all"} is in the set.
     *
     * @param expandSet Set of relationship names to expand, typically from {@link #parseExpand}
     * @param field     The relationship field name to check
     * @return true if the field should be expanded, false otherwise
     */
    public static boolean shouldExpand(Set<String> expandSet, String field) {
        return expandSet.contains(field) || expandSet.contains(EXPAND_ALL);
    }
}

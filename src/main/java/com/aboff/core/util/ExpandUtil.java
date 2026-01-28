package com.aboff.core.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for parsing expand parameters in API responses.
 * <p>
 * Expand parameters allow API consumers to request related entities
 * to be included in the response, reducing the number of API calls needed.
 * </p>
 */
public final class ExpandUtil {

    private ExpandUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Parses a comma-separated expand parameter into a set of relationship names.
     * <p>
     * Example: "owner,experiences,inventoryWeapons" becomes
     * Set.of("owner", "experiences", "inventoryWeapons")
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
}

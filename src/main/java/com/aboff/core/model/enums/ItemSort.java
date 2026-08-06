package com.aboff.core.model.enums;

import org.springframework.data.domain.Sort;

/**
 * The orderings a caller may request when browsing weapons, armor, or loot.
 * <p>
 * An allowlist rather than a free-text Spring {@code sort} parameter: binding an arbitrary
 * property path from a query string lets a caller order by, and so infer, fields the response
 * never exposes.
 * </p>
 * <p>
 * {@link #ID} stays the default so existing callers are unaffected. It is rarely what a person
 * wants, though — official content occupies the low ids, so anything a user creates sorts to the
 * very end. Pickers and browse screens should ask for {@link #NAME}.
 * </p>
 */
public enum ItemSort {

    /** Insertion order. The historical default; buries user-authored content. */
    ID(Sort.by("id").ascending()),

    /** Alphabetical. The sensible default for any list a person reads. */
    NAME(Sort.by("name").ascending()),

    /** Lowest tier first, alphabetical within a tier. */
    TIER(Sort.by("tier").ascending().and(Sort.by("name").ascending())),

    /** Most recently created first — surfaces what the caller just made. */
    NEWEST(Sort.by("createdAt").descending().and(Sort.by("id").descending()));

    private final Sort sort;

    ItemSort(Sort sort) {
        this.sort = sort;
    }

    /**
     * Returns the Spring Data sort this ordering maps to.
     *
     * @return the sort specification
     */
    public Sort toSort() {
        return sort;
    }
}

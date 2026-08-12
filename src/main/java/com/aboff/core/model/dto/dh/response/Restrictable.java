package com.aboff.core.model.dto.dh.response;

/**
 * Marker interface for response DTOs that can be returned as a redacted stub in place of
 * their full content, for gated non-SRD content embedded in something a caller owns (e.g. a
 * character sheet's class card) that they may not browse directly.
 * <p>
 * Declares no behavior of its own — the DTOs implementing this already carry {@code @Data},
 * so Lombok generates each of these setters for free. Implementing this interface only asserts
 * that the DTO has these fields and that {@link com.aboff.core.util.ContentRedaction#stub} may
 * be used to build a redacted instance of it.
 * </p>
 *
 * @see com.aboff.core.util.ContentRedaction
 */
public interface Restrictable {

    /**
     * Sets the entity's ID — the one field a redacted stub always carries, alongside
     * {@link #setRestricted}, so the frontend can still key on it.
     *
     * @param id the entity ID
     */
    void setId(Long id);

    /**
     * Sets the entity's display name.
     * <p>
     * Declared here for completeness of the DTO contract, but {@link ContentRedaction#stub}
     * deliberately never calls this — a redacted stub carries no name, so
     * {@code @JsonInclude(NON_NULL)} erases the field from the response entirely.
     * </p>
     *
     * @param name the entity's name
     */
    void setName(String name);

    /**
     * Sets the name of the expansion this content belongs to, so a redacted stub can tell the
     * viewer which book to buy.
     *
     * @param expansionName the expansion's display name
     */
    void setExpansionName(String expansionName);

    /**
     * Sets whether this DTO represents redacted content.
     * <p>
     * {@code restricted: true} is the sole key the frontend needs to render the
     * "Content Not Available" placeholder face in place of the real card/item.
     * </p>
     *
     * @param restricted true if this DTO is a redacted stub
     */
    void setRestricted(Boolean restricted);
}

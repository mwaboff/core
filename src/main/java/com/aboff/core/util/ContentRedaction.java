package com.aboff.core.util;

import com.aboff.core.model.dto.dh.response.Restrictable;

import java.util.function.Supplier;

/**
 * Builds redacted response stubs for gated non-SRD content embedded in something a caller
 * owns (e.g. a character sheet's class card) that they may not otherwise browse.
 * <p>
 * A restricted entity comes back as the <strong>same DTO type</strong> it would normally
 * serialize as, with every field left unset except {@code id}, {@code restricted}, and
 * {@code expansionName}. Every gated response DTO already carries
 * {@code @JsonInclude(Include.NON_NULL)}, which erases the unset fields from the response
 * entirely rather than serializing them as {@code null}. For a domain card, that produces:
 * </p>
 * <pre>{@code
 * { "id": 412, "cardType": "DOMAIN", "expansionName": "Hope & Fear", "restricted": true }
 * }</pre>
 * <p>
 * {@code restricted: true} is the sole key the frontend needs — no name, no description, no
 * features, no {@code srd}, no {@code isOfficial}. Leaking any of those onto a redacted stub
 * defeats the point of gating the content in the first place.
 * </p>
 * <p>
 * {@link #stub} sets only {@code id}, {@code restricted}, and {@code expansionName}. Callers
 * are responsible for setting any type discriminator the DTO carries afterward (e.g.
 * {@code cardType} on a card response) — that field is what lets a caller distinguish which
 * kind of restricted stub it is looking at without exposing anything else about the content.
 * </p>
 */
public final class ContentRedaction {

    private ContentRedaction() {
        // Utility class - prevent instantiation
    }

    /**
     * Builds a redacted stub of the given response DTO type.
     *
     * @param factory creates a fresh, empty instance of the target DTO (e.g. {@code
     *                DomainCardResponse::new} or a builder call)
     * @param id the entity's ID
     * @param expansionName the display name of the expansion the gated content belongs to
     * @param <T> the response DTO type, which must implement {@link Restrictable}
     * @return a new instance of {@code T} carrying only {@code id}, {@code restricted = true},
     *         and {@code expansionName}
     */
    public static <T extends Restrictable> T stub(Supplier<T> factory, Long id, String expansionName) {
        T stub = factory.get();
        stub.setId(id);
        stub.setRestricted(true);
        stub.setExpansionName(expansionName);
        return stub;
    }
}

package com.aboff.core.model;

import com.aboff.core.model.enums.Role;
import com.aboff.core.security.CustomUserDetails;
import org.springframework.security.core.Authentication;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Structured metadata context for audit log entries.
 * <p>
 * Produces bracketed key-value metadata strings such as:
 * {@code [user_id: 42; username: mwaboff; role: owner; campaign_id: 3]}
 * </p>
 * <p>
 * Use the builder API to construct contexts:
 * <pre>
 * AuditContext ctx = AuditContext.forUser(authentication)
 *     .withCampaignId(3L)
 *     .build();
 * </pre>
 * </p>
 */
public class AuditContext {

    private final Map<String, String> fields;

    private AuditContext(Map<String, String> fields) {
        this.fields = fields;
    }

    /**
     * Formats the context as a bracketed metadata string.
     *
     * @return formatted string, e.g. {@code [user_id: 42; username: mwaboff; role: owner]}
     */
    public String format() {
        if (fields.isEmpty()) {
            return "[]";
        }
        String inner = fields.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));
        return "[" + inner + "]";
    }

    /**
     * Creates a builder pre-populated with user information from an Authentication object.
     *
     * @param authentication the Spring Security authentication
     * @return a new builder with user_id, username, and role populated
     */
    public static Builder forUser(Authentication authentication) {
        Builder builder = new Builder();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            builder.fields.put("user_id", String.valueOf(userDetails.getUserId()));
            builder.fields.put("username", userDetails.getUsername());
            Role role = userDetails.getUser().getRole();
            if (role != null) {
                builder.fields.put("role", role.name().toLowerCase());
            }
        }
        return builder;
    }

    /**
     * Creates a builder with only an IP address (for unauthenticated requests).
     *
     * @param ip the client IP address
     * @return a new builder with ip populated
     */
    public static Builder forIp(String ip) {
        Builder builder = new Builder();
        if (ip != null) {
            builder.fields.put("ip", ip);
        }
        return builder;
    }

    /**
     * Builder for constructing {@link AuditContext} instances with composable metadata fields.
     */
    public static class Builder {
        private final Map<String, String> fields = new LinkedHashMap<>();

        /**
         * Adds the client IP address to the context.
         *
         * @param ip the client IP address
         * @return this builder
         */
        public Builder withIp(String ip) {
            if (ip != null) {
                fields.put("ip", ip);
            }
            return this;
        }

        /**
         * Adds a campaign ID to the context.
         *
         * @param campaignId the campaign ID
         * @return this builder
         */
        public Builder withCampaignId(Long campaignId) {
            if (campaignId != null) {
                fields.put("campaign_id", String.valueOf(campaignId));
            }
            return this;
        }

        /**
         * Adds a character sheet ID to the context.
         *
         * @param characterSheetId the character sheet ID
         * @return this builder
         */
        public Builder withCharacterSheetId(Long characterSheetId) {
            if (characterSheetId != null) {
                fields.put("character_sheet_id", String.valueOf(characterSheetId));
            }
            return this;
        }

        /**
         * Adds a target user ID to the context (the user being acted upon).
         *
         * @param targetUserId the target user ID
         * @return this builder
         */
        public Builder withTargetUserId(Long targetUserId) {
            if (targetUserId != null) {
                fields.put("target_user_id", String.valueOf(targetUserId));
            }
            return this;
        }

        /**
         * Adds a generic entity type label to the context.
         *
         * @param entityType the entity type name (e.g., "weapon", "armor")
         * @return this builder
         */
        public Builder withEntityType(String entityType) {
            if (entityType != null) {
                fields.put("entity_type", entityType);
            }
            return this;
        }

        /**
         * Builds the {@link AuditContext} with the accumulated fields.
         *
         * @return a new AuditContext
         */
        public AuditContext build() {
            return new AuditContext(new LinkedHashMap<>(fields));
        }
    }
}

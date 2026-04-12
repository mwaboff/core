package com.aboff.core.service;

import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.UserIdentity;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.UserIdentityRepository;
import com.aboff.core.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service responsible for provisioning users from OAuth2 authentication.
 * <p>
 * Handles the find-or-create pattern for OAuth2 sign-ins: looks up an existing
 * {@link UserIdentity} by provider and subject identifier, and creates a new
 * {@link User} and {@link UserIdentity} on first sign-in.
 * </p>
 */
@Slf4j
@Service
public class OAuth2UserProvisioningService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;

    @Value("${application.user.default-avatar-url}")
    private String defaultAvatarUrl;

    @Value("${application.user.default-timezone}")
    private String defaultTimezone;

    /**
     * Constructs a new OAuth2UserProvisioningService with required dependencies.
     *
     * @param userRepository         the user repository
     * @param userIdentityRepository the user identity repository
     */
    public OAuth2UserProvisioningService(
            UserRepository userRepository,
            UserIdentityRepository userIdentityRepository) {
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
    }

    /**
     * Finds or creates a user from an OAuth2 authentication callback.
     * <p>
     * If a {@link UserIdentity} already exists for the given provider and subject
     * identifier, the linked user is returned and the identity's {@code lastUsedAt}
     * timestamp is updated. Otherwise, a new {@link User} and {@link UserIdentity}
     * are created from the OAuth2 principal's attributes.
     * </p>
     *
     * @param provider    the OAuth2 provider identifier (e.g. {@code "google"})
     * @param principal   the authenticated OAuth2 user principal
     * @return the existing or newly created user
     */
    @Transactional
    public User findOrCreateUserFromOAuth2(String provider, OAuth2User principal) {
        String sub = principal.getAttribute("sub");
        // For dev provider, sub might be the email itself
        if (sub == null) {
            sub = principal.getAttribute("email");
        }

        String finalSub = sub;

        Optional<UserIdentity> existingIdentity = userIdentityRepository.findByProviderAndProviderSub(provider, sub);

        if (existingIdentity.isPresent()) {
            UserIdentity identity = existingIdentity.get();
            identity.setLastUsedAt(LocalDateTime.now());
            userIdentityRepository.save(identity);
            return identity.getUser();
        }

        // Create new user
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        User user = User.builder()
                .email(email)
                .username(generateUsername(email, finalSub))
                .avatarUrl(picture != null ? picture : defaultAvatarUrl)
                .timezone(defaultTimezone)
                .build();
        user = userRepository.save(user);

        UserIdentity identity = UserIdentity.builder()
                .user(user)
                .provider(provider)
                .providerSub(finalSub)
                .email(email)
                .displayName(name)
                .avatarUrl(picture)
                .linkedAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .build();
        userIdentityRepository.save(identity);

        log.info("Created new user '{}' via {} OAuth", user.getUsername(), provider);
        return user;
    }

    /**
     * Finds or creates a user for the dev-login endpoint (provider {@code "dev"}).
     * <p>
     * If the user already exists, updates their {@code lastUsedAt} and optionally
     * promotes/demotes the role if a different role is supplied. On first call,
     * creates a new user and identity with the given email and role.
     * </p>
     *
     * @param email the email address used as the dev user's identifier
     * @param role  the desired role; if {@code null}, defaults to {@link Role#USER}
     * @return the existing or newly created user
     */
    @Transactional
    public User findOrCreateDevUser(String email, Role role) {
        Optional<UserIdentity> existing = userIdentityRepository.findByProviderAndProviderSub("dev", email);

        if (existing.isPresent()) {
            UserIdentity identity = existing.get();
            identity.setLastUsedAt(LocalDateTime.now());
            userIdentityRepository.save(identity);
            User user = identity.getUser();
            if (role != null && !role.equals(user.getRole())) {
                user.setRole(role);
                userRepository.save(user);
            }
            return user;
        }

        User user = User.builder()
                .email(email)
                .username(generateUsername(email, email))
                .avatarUrl(defaultAvatarUrl)
                .timezone(defaultTimezone)
                .role(role != null ? role : Role.USER)
                .build();
        user = userRepository.save(user);

        UserIdentity identity = UserIdentity.builder()
                .user(user)
                .provider("dev")
                .providerSub(email)
                .email(email)
                .displayName(email.split("@")[0])
                .linkedAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .build();
        userIdentityRepository.save(identity);

        log.info("Created dev user '{}' with role {}", user.getUsername(), user.getRole());
        return user;
    }

    /**
     * Generates a URL-safe username from the user's email and OAuth subject identifier.
     * <p>
     * The base is derived from the email local-part, slugified to lowercase
     * alphanumeric characters with dashes. If the base is already taken, the first
     * six characters of the subject identifier are appended as a suffix.
     * </p>
     *
     * @param email the user's email address; may be {@code null}
     * @param sub   the OAuth subject identifier used as a collision-resolution suffix
     * @return a unique username string
     */
    private String generateUsername(String email, String sub) {
        String base = email != null ? email.split("@")[0] : "user";
        // Slugify: lowercase, replace non-alphanumeric with dashes
        base = base.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (base.isEmpty()) base = "user";

        if (!userRepository.findByUsername(base).isPresent()) {
            return base;
        }
        // Collision: append first 6 chars of sub
        String suffix = sub.length() > 6 ? sub.substring(0, 6) : sub;
        return base + "-" + suffix.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}

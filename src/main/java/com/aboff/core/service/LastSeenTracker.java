package com.aboff.core.service;

import com.aboff.core.model.entity.User;
import com.aboff.core.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records authenticated-request activity for a user by updating
 * {@code users.last_seen_at}.
 * <p>
 * Called from {@link com.aboff.core.security.JwtAuthenticationFilter} after the
 * authentication context has been populated. Throttles writes through an
 * in-memory per-user cache so that every authenticated request does not cause
 * a database write.
 * </p>
 *
 * <p><strong>Scope:</strong> the throttle cache is per-JVM. That is acceptable
 * for the current single-node deployment; multi-node deployments should
 * replace the cache with a shared store.</p>
 */
@Slf4j
@Component
public class LastSeenTracker {

    /**
     * Minimum interval between {@code last_seen_at} writes for the same user.
     * A shorter interval produces excessive writes under normal API traffic;
     * a longer interval makes the admin-facing timestamp stale.
     */
    static final Duration THROTTLE_INTERVAL = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final Clock clock;
    private final ConcurrentHashMap<Long, Instant> lastWriteByUser = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code LastSeenTracker}.
     *
     * @param userRepository repository used to persist the updated timestamp
     * @param clock          clock source, injected to keep the throttle testable
     */
    public LastSeenTracker(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * Records that the given user was seen. If the user was seen within
     * {@link #THROTTLE_INTERVAL}, the call is a no-op.
     *
     * @param user the authenticated user; ignored if {@code null} or has no id
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSeen(User user) {
        if (user == null || user.getId() == null) {
            return;
        }

        Instant now = clock.instant();
        Instant previous = lastWriteByUser.get(user.getId());
        if (previous != null && Duration.between(previous, now).compareTo(THROTTLE_INTERVAL) < 0) {
            return;
        }

        lastWriteByUser.put(user.getId(), now);

        try {
            LocalDateTime ts = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
            userRepository.updateLastSeenAt(user.getId(), ts);
        } catch (Exception ex) {
            // Never let activity tracking break a user's request.
            log.debug("Failed to update last_seen_at for userId={}: {}", user.getId(), ex.getMessage());
        }
    }
}

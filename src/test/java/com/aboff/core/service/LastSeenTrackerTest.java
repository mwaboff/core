package com.aboff.core.service;

import com.aboff.core.model.entity.User;
import com.aboff.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link LastSeenTracker}, using an injected {@link Clock}
 * to exercise the throttle deterministically.
 */
@ExtendWith(MockitoExtension.class)
class LastSeenTrackerTest {

    @Mock
    private UserRepository userRepository;

    private SteppingClock clock;
    private LastSeenTracker tracker;
    private User user;

    @BeforeEach
    void setUp() {
        clock = new SteppingClock(Instant.parse("2026-04-23T12:00:00Z"));
        tracker = new LastSeenTracker(userRepository, clock);
        user = User.builder().id(7L).username("u7").build();
    }

    @Test
    void recordSeen_FirstCall_WritesUpdate() {
        tracker.recordSeen(user);
        verify(userRepository).updateLastSeenAt(anyLong(), any());
    }

    @Test
    void recordSeen_ImmediateSecondCall_IsThrottled() {
        tracker.recordSeen(user);
        clock.advance(Duration.ofMinutes(1));
        tracker.recordSeen(user);
        verify(userRepository, times(1)).updateLastSeenAt(anyLong(), any());
    }

    @Test
    void recordSeen_AfterThrottleInterval_WritesAgain() {
        tracker.recordSeen(user);
        clock.advance(Duration.ofMinutes(6));
        tracker.recordSeen(user);
        verify(userRepository, times(2)).updateLastSeenAt(anyLong(), any());
    }

    @Test
    void recordSeen_NullUser_IsNoOp() {
        tracker.recordSeen(null);
        verify(userRepository, never()).updateLastSeenAt(anyLong(), any());
    }

    @Test
    void recordSeen_UserWithoutId_IsNoOp() {
        tracker.recordSeen(User.builder().username("nope").build());
        verify(userRepository, never()).updateLastSeenAt(anyLong(), any());
    }

    /** Minimal steppable Clock so the tests don't need to sleep. */
    private static final class SteppingClock extends Clock {
        private Instant now;

        SteppingClock(Instant start) {
            this.now = start;
        }

        void advance(Duration d) {
            this.now = this.now.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}

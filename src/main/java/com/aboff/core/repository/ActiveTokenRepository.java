package com.aboff.core.repository;

import com.aboff.core.model.entity.ActiveToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveTokenRepository extends JpaRepository<ActiveToken, Long> {

    /**
     * Finds a token by its hash
     */
    Optional<ActiveToken> findByTokenHash(String tokenHash);

    /**
     * Finds a valid (not revoked and not expired) token by its hash
     */
    @Query("SELECT at FROM ActiveToken at WHERE at.tokenHash = :tokenHash " +
           "AND at.revokedAt IS NULL AND at.expiresAt > :now")
    Optional<ActiveToken> findValidToken(
        @Param("tokenHash") String tokenHash,
        @Param("now") LocalDateTime now);

    /**
     * Finds all non-revoked tokens for a user
     */
    List<ActiveToken> findByUserIdAndRevokedAtIsNull(Long userId);

    /**
     * Revokes all tokens for a user
     */
    @Modifying
    @Query("UPDATE ActiveToken at SET at.revokedAt = :now WHERE at.userId = :userId " +
           "AND at.revokedAt IS NULL")
    int revokeAllUserTokens(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Deletes expired tokens and old revoked tokens
     */
    @Modifying
    @Query("DELETE FROM ActiveToken at WHERE at.expiresAt < :before OR at.revokedAt < :before")
    int deleteExpiredAndOldRevokedTokens(@Param("before") LocalDateTime before);
}

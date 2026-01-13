package com.aboff.core.security;

import com.aboff.core.model.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;

/**
 * Utility class for generating and validating JWT tokens.
 * Handles token creation, claim extraction, and hashing.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long jwtExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long jwtExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Generates a JWT token for the given user.
     *
     * @param user the user to generate the token for
     * @return the generated JWT token string
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates the JWT token and returns the claims.
     *
     * @param token the JWT token to validate
     * @return the claims if the token is valid
     * @throws JwtException if the token is invalid or expired
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SignatureException ex) {
            throw new JwtException("Invalid JWT signature", ex);
        } catch (MalformedJwtException ex) {
            throw new JwtException("Invalid JWT token", ex);
        } catch (ExpiredJwtException ex) {
            throw new JwtException("Expired JWT token", ex);
        } catch (UnsupportedJwtException ex) {
            throw new JwtException("Unsupported JWT token", ex);
        } catch (IllegalArgumentException ex) {
            throw new JwtException("JWT claims string is empty", ex);
        }
    }

    /**
     * Extracts the user ID from the JWT token.
     *
     * @param token the JWT token
     * @return the user ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Checks if the token is expired.
     *
     * @param token the JWT token
     * @return true if the token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * Generates a SHA-256 hash of the token for database storage.
     *
     * @param token the JWT token
     * @return the token hash
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Returns the JWT expiration time in milliseconds.
     *
     * @return the expiration time in milliseconds
     */
    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}

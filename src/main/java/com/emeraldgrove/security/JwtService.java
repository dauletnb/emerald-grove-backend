package com.emeraldgrove.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Handles JWT access token creation and validation.
 *
 * How it works (beginner explanation):
 * - A JWT is a string with three parts separated by dots: header.payload.signature
 * - The payload contains the user's email and token expiry time
 * - The signature is created with a secret key — only the backend can create valid signatures
 * - The frontend sends the token on every request; the backend checks the signature to trust it
 * - No database lookup needed to validate — the signature proves authenticity
 */
@Service
public class JwtService {
    @Value("${emerald-grove.security.jwt-secret}")
    private String jwtSecret;
    @Value("${emerald-grove.security.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    public String generateAccessToken(String email) {
        return Jwts.builder()
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

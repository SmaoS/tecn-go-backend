package com.tecngo.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.tecngo.users.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private final String secret;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserDetails user) {
        Instant now = Instant.now();
        return generateToken(user, UUID.randomUUID(), now.plusMillis(expirationMs));
    }

    public String generateToken(UserDetails user, UUID sessionId, Instant expiresAt) {
        Instant now = Instant.now();
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .sorted()
                .toList();
        String legacyRole = user instanceof User tecngoUser && tecngoUser.getRole() != null
                ? tecngoUser.getRole().name()
                : roles.stream().findFirst().orElse("");
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", legacyRole);
        claims.put("roles", roles);
        if (user instanceof User tecngoUser && tecngoUser.getActiveMode() != null) {
            claims.put("active_mode", tecngoUser.getActiveMode().name());
        }
        return Jwts.builder()
                .claims(claims)
                .subject(user instanceof User tecngoUser && tecngoUser.getId() != null
                        ? tecngoUser.getId().toString()
                        : user.getUsername())
                .id(sessionId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key())
                .compact();
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    public String extractRole(String token) {
        return claims(token).get("role", String.class);
    }

    public List<String> extractRoles(String token) {
        Object value = claims(token).get("roles");
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        String legacyRole = extractRole(token);
        return legacyRole == null || legacyRole.isBlank() ? List.of() : List.of(legacyRole);
    }

    public String extractActiveMode(String token) {
        return claims(token).get("active_mode", String.class);
    }

    public UUID extractSessionId(String token) {
        String id = claims(token).getId();
        return id == null || id.isBlank() ? null : UUID.fromString(id);
    }

    public boolean isValid(String token, UserDetails user) {
        Claims claims = claims(token);
        String expectedSubject = user instanceof User tecngoUser && tecngoUser.getId() != null
                ? tecngoUser.getId().toString()
                : user.getUsername();
        boolean matchingSubject = claims.getSubject().equals(expectedSubject)
                || claims.getSubject().equals(user.getUsername());
        return matchingSubject && claims.getExpiration().after(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

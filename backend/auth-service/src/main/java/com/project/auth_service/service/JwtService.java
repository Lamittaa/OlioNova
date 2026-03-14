package com.project.auth_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String JTI = "jti";

    private final Key key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${security.jwt.secret:MySuperSecretKeyForJwt1234567890MyExtra}") String secret,
            @Value("${security.jwt.access-expiration-ms:900000}") long accessExpirationMs,
            @Value("${security.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object value = claims.get(AUTHORITIES_CLAIM);
        if (value instanceof List<?> list) {
            List<String> roles = new ArrayList<>();
            for (Object o : list)
                if (o != null)
                    roles.add(o.toString());
            return roles;
        }
        return Collections.emptyList();
    }

    public String generateAccessToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(AUTHORITIES_CLAIM,
                user.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        claims.put(JTI, UUID.randomUUID().toString());
        return createToken(claims, user.getUsername(), accessExpirationMs);
    }

    public String generateAccessToken(
            String username,
            Collection<String> authorities,
            Long userId) {
        Map<String, Object> claims = new HashMap<>();

        claims.put(AUTHORITIES_CLAIM, authorities);
        claims.put("userId", userId);
        claims.put(JTI, UUID.randomUUID().toString());

        return createToken(claims, username, accessExpirationMs);
    }

    public String generateRefreshToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JTI, UUID.randomUUID().toString());
        return createToken(claims, user.getUsername(), refreshExpirationMs);
    }

    public long getAccessTokenTtlSeconds() {
        return accessExpirationMs / 1000;
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshExpirationMs / 1000;
    }

    public boolean isAccessTokenValid(String token, UserDetails user) {
        final String username = extractUsername(token);
        return username.equals(user.getUsername()) && !isTokenExpired(token);
    }

    private String createToken(Map<String, Object> claims, String subject, long ttlMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

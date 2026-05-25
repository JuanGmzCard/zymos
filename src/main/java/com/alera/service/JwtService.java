package com.alera.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long      ttlMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.ttl-hours:24}") long ttlHours) {
        this.key   = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMs = ttlHours * 3_600_000L;
    }

    public String generarToken(UserDetails user, String tenantId) {
        String rol = user.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("");
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("tenant", tenantId)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key)
                .compact();
    }

    public boolean validarToken(String token) {
        try {
            parsear(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extraerUsername(String token) {
        return parsear(token).getPayload().getSubject();
    }

    public String extraerTenant(String token) {
        return parsear(token).getPayload().get("tenant", String.class);
    }

    public long getTtlSegundos() {
        return ttlMs / 1000L;
    }

    private Jws<Claims> parsear(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}

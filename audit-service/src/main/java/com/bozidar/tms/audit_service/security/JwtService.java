package com.bozidar.tms.audit_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${security.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    public CurrentUser validateAndGetUser(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        return new CurrentUser(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("firstName", String.class),
                claims.get("lastName", String.class),
                claims.get("roles", List.class)
        );
    }
}

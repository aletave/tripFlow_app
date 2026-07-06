package com.tripflow.catalog_service.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class TokenGeneratorTest {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void generate() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "ORGANIZER")
                //claim "nome" richiesto da review-service (autore_nome_snap NOT NULL);
                //user-auth dovra emetterlo nei token reali
                .claim("nome", "Utente Test")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 864000000L))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        System.out.println("token di test:");
        System.out.println(token);
    }
}
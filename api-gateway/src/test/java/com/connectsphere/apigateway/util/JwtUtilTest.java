package com.connectsphere.apigateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET =
            "this-is-a-very-long-secret-key-for-connectsphere-jwt-testing-123456";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);
    }

    private String generateToken(Date expirationDate) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject("10")
                .claim("username", "rahul")
                .claim("role", "USER")
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void isTokenValid_ShouldReturnTrue_ForValidToken() {
        String token = generateToken(new Date(System.currentTimeMillis() + 1000 * 60 * 60));

        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_ShouldReturnFalse_ForExpiredToken() {
        String token = generateToken(new Date(System.currentTimeMillis() - 1000 * 60));

        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_ShouldReturnFalse_ForInvalidToken() {
        assertFalse(jwtUtil.isTokenValid("invalid.jwt.token"));
    }

    @Test
    void extractUserId_ShouldReturnSubject() {
        String token = generateToken(new Date(System.currentTimeMillis() + 1000 * 60 * 60));

        assertEquals("10", jwtUtil.extractUserId(token));
    }

    @Test
    void extractRole_ShouldReturnRole() {
        String token = generateToken(new Date(System.currentTimeMillis() + 1000 * 60 * 60));

        assertEquals("USER", jwtUtil.extractRole(token));
    }

    @Test
    void extractUsername_ShouldReturnUsername() {
        String token = generateToken(new Date(System.currentTimeMillis() + 1000 * 60 * 60));

        assertEquals("rahul", jwtUtil.extractUsername(token));
    }
}
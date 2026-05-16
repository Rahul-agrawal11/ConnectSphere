package com.connectsphere.auth.security;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.enums.AccountStatus;
import com.connectsphere.auth.enums.AuthProvider;
import com.connectsphere.auth.enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User user;

    // Secret must be at least 32 chars for HMAC-SHA256
    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);

        user = User.builder()
                .id(1L)
                .username("rahul")
                .email("rahul@gmail.com")
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateToken(user);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractUserId_shouldReturnCorrectId() {
        String token = jwtUtil.generateToken(user);
        Long userId = jwtUtil.extractUserId(token);
        assertThat(userId).isEqualTo(1L);
    }

    @Test
    void extractAllClaims_shouldContainUsername() {
        String token = jwtUtil.generateToken(user);
        Claims claims = jwtUtil.extractAllClaims(token);
        assertThat(claims.get("username", String.class)).isEqualTo("rahul");
    }

    @Test
    void extractAllClaims_shouldContainRole() {
        String token = jwtUtil.generateToken(user);
        Claims claims = jwtUtil.extractAllClaims(token);
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void extractAllClaims_shouldContainEmail() {
        String token = jwtUtil.generateToken(user);
        Claims claims = jwtUtil.extractAllClaims(token);
        assertThat(claims.get("email", String.class)).isEqualTo("rahul@gmail.com");
    }

    @Test
    void isTokenValid_withValidToken_shouldReturnTrue() {
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_withExpiredToken_shouldReturnFalse() {
        // Create a JwtUtil with -1ms expiry so token is instantly expired
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, -1L);
        String token = expiredJwtUtil.generateToken(user);
        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_withGarbageToken_shouldReturnFalse() {
        assertThat(jwtUtil.isTokenValid("not.a.jwt.token")).isFalse();
    }

    @Test
    void isTokenValid_withTamperedToken_shouldReturnFalse() {
        String token = jwtUtil.generateToken(user);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }
}
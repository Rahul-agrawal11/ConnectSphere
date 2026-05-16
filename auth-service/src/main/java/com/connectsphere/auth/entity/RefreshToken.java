package com.connectsphere.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Stores issued refresh tokens.
 * Allows server-side token revocation (logout/security invalidation).
 * One active refresh token per user — new login replaces the old one.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The opaque refresh token string
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
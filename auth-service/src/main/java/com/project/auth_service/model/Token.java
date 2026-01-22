// src/main/java/com/project/auth_service/model/RefreshToken.java
package com.project.auth_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "token", indexes = { @Index(name = "idx_token_access_token", columnList = "access_token", unique = true),
        @Index(name = "idx_token_refresh_token", columnList = "refresh_token", unique = true),
        @Index(name = "idx_refresh_token_user", columnList = "user_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 1000000)
    private String accessToken;

    @Column(nullable = false, unique = true, length = 1000000)
    private String refreshToken;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant refreshExpiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}

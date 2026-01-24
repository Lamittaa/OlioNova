package com.project.auth_service.service;

import com.project.auth_service.exception.InvalidTokenException;
import com.project.auth_service.exception.TokenExpiredException;
import com.project.auth_service.exception.TokenRevokedException;
import com.project.auth_service.model.Token;
import com.project.auth_service.model.User;
import com.project.auth_service.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final  TokenRepository repo;




    public Token saveAccessToken(User user, String rawToken, Instant expiresAt,String refreshToken, Instant refreshExpiresAt) {
        repo.deleteAllByUser(user);
        Token accessToken = Token.builder()
                .user(user)
                .accessToken(rawToken)         
                .expiresAt(expiresAt)
                .revoked(false)
                .createdAt(Instant.now())
                .refreshToken(refreshToken)
                .refreshExpiresAt(refreshExpiresAt)
                .build();
        return repo.save(accessToken);
    }

    public Token validateUsable(String rawToken) {
        Token accessToken = repo.findByAccessToken(rawToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));

        if (accessToken.isRevoked()) {
            throw new TokenRevokedException("token revoked");
        }
        if (accessToken.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("token expired");
        }
        return accessToken;
    }
public Token validateUsableRefreshToken(String refreshToken) {
    Token rt = repo.findByRefreshToken(refreshToken)  // ✅ صح
            .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

    if (rt.isRevoked()) {
        throw new TokenRevokedException("Refresh token revoked");
    }
    if (rt.getRefreshExpiresAt().isBefore(Instant.now())) { // ✅ استخدمي refreshExpiresAt مش expiresAt
        throw new TokenExpiredException("Refresh token expired");
    }
    return rt;
}


    public void revoke(Token accessToken) {
        accessToken.setRevoked(true);
        repo.save(accessToken);
    }
    public Token save(Token token) {
    return repo.save(token);
}

}

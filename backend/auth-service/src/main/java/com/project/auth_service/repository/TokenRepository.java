package com.project.auth_service.repository;

import com.project.auth_service.model.Token;
import com.project.auth_service.model.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByAccessToken(String token);
    Optional<Token> findByRefreshToken(String refreshToken);
    List<Token> findAllByUser_Username(String username);
    void  deleteAllByUser(User user);

    String user(User user);
}

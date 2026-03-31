package com.project.auth_service.repository;

import com.project.auth_service.model.ActivationToken;
import com.project.auth_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActivationTokenRepository
        extends JpaRepository<ActivationToken, Long> {

    Optional<ActivationToken> findByToken(String token);

    Optional<ActivationToken> findByOtpCode(String otpCode);

    Optional<ActivationToken> findByUser_Id(Long userId);

    @Modifying
    @Query("DELETE FROM ActivationToken a WHERE a.user = :user")
    void deleteByUser(@Param("user") User user);
}
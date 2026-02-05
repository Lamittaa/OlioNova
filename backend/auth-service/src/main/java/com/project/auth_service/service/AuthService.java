package com.project.auth_service.service;
import java.util.List;

import com.project.auth_service.exception.EmployeeNotFoundException;
import com.project.auth_service.exception.InvalidTokenException;
import com.project.auth_service.exception.TokenExpiredException;
import com.project.auth_service.exception.UserNotFoundException;
import com.project.auth_service.model.ActivationToken;
import com.project.auth_service.model.Employee;
import com.project.auth_service.model.Token;
import com.project.auth_service.model.User;
import com.project.auth_service.repository.UserRepository;
import com.project.auth_service.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.auth_service.repository.ActivationTokenRepository;
import com.project.auth_service.repository.EmployeeRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final ActivationTokenRepository tokenRepo; // ✅ أضيفي هذا
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepo;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService uds;
    private final TokenService tokenService;
private final EmployeeRepository employeeRepo;

   @Transactional
public LoginResponseDto login(LoginRequestDto request) {
    log.info("Login attempt for username: {}", request.getUsername());

    try {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
    } catch (BadCredentialsException ex) {
        log.warn("Failed login for username: {}", request.getUsername());
        throw ex;
    }

    // ===== Load UserDetails =====
    UserDetails userDetails = uds.loadUserByUsername(request.getUsername());

    // ===== Load User entity =====
    User user = userRepo.findByUsername(request.getUsername())
            .orElseThrow(() ->
                    new UserNotFoundException("User not found: " + request.getUsername())
            );

Employee employee = employeeRepo.findByUserUsername(user.getUsername())
        .orElseThrow(() -> new EmployeeNotFoundException(
                "Employee not found for user: " + user.getUsername()
        ));

    // ===== Authorities =====
    List<String> authorities = userDetails.getAuthorities()
            .stream()
            .map(a -> a.getAuthority())
            .toList();

    // ===== Generate Tokens =====
    String accessToken = jwtService.generateAccessToken(
            user.getUsername(),
            authorities,
            employee.getId()
    );

    String refreshToken = jwtService.generateRefreshToken(userDetails);

    long refreshTtlSeconds = jwtService.getRefreshTokenTtlSeconds();
    long accessTtlSeconds = jwtService.getAccessTokenTtlSeconds();

    tokenService.saveAccessToken(
            user,
            accessToken,
            Instant.now().plusSeconds(accessTtlSeconds),
            refreshToken,
            Instant.now().plusSeconds(refreshTtlSeconds)
    );

    log.info("User {} logged in successfully with role {}",
            user.getUsername(),
            user.getRole().getName()
    );

    return LoginResponseDto.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(accessTtlSeconds)
            .refreshExpiresIn(refreshTtlSeconds)
            .username(user.getUsername())
            .role(user.getRole().getName())
            .build();
}


    @Transactional
public RefreshResponseDto refresh(RefreshRequestDto dto) {
    log.info("Refresh token request received.");

    // ===== Validate refresh token =====
    Token oldToken;
    try {
        oldToken = tokenService.validateUsableRefreshToken(dto.getRefreshToken());
    } catch (Exception ex) {
        log.warn("Invalid refresh token attempt: {}", ex.getMessage());
        throw ex;
    }

    // ===== Load user =====
    User user = oldToken.getUser();

    // ===== Load UserDetails =====
    UserDetails userDetails = uds.loadUserByUsername(user.getUsername());

    // ===== Load Employee =====
    Employee employee = employeeRepo.findByUserUsername(user.getUsername())
            .orElseThrow(() -> new EmployeeNotFoundException(
                    "Employee not found for user: " + user.getUsername()
            ));

    // ===== Authorities =====
    List<String> authorities = userDetails.getAuthorities()
            .stream()
            .map(a -> a.getAuthority())
            .toList();

    log.info("Rotating refresh token for user: {}", user.getUsername());

    // ===== Generate tokens =====
    String newAccess = jwtService.generateAccessToken(
            user.getUsername(),
            authorities,
            employee.getId()
    );

    String newRefresh = jwtService.generateRefreshToken(userDetails);

    long refreshTtlSeconds = jwtService.getRefreshTokenTtlSeconds();
    long accessTtlSeconds = jwtService.getAccessTokenTtlSeconds();

    // ===== Save tokens =====
    tokenService.saveAccessToken(
            user,
            newAccess,
            Instant.now().plusSeconds(accessTtlSeconds),
            newRefresh,
            Instant.now().plusSeconds(refreshTtlSeconds)
    );

    log.info("Refresh token rotation successful for {}", user.getUsername());

    // ===== Response =====
    return RefreshResponseDto.builder()
            .accessToken(newAccess)
            .refreshToken(newRefresh)
            .tokenType("Bearer")
            .expiresIn(accessTtlSeconds)
            .refreshExpiresIn(refreshTtlSeconds)
            .username(user.getUsername())
            .role(user.getRole().getName())
            .build();
}


    @Transactional
    public void logout(RevokeRequestDto dto) {
        log.info("Logout request received with refresh token.");

        Token token = tokenService.validateUsable(dto.getAccessToken());

        token.setRevoked(true);

        log.info("Tokens revoked manually for user {}", token.getUser().getUsername());
    }

    @Transactional
    public void logout(String userName) {
        log.info("Logout request received with username");

        tokenService.revokeAllUserTokens(userName);

        log.info("Tokens revoked manually for user {}", userName);
    }

    @Transactional
    public void setPassword(SetPasswordRequest req) {

        ActivationToken token = tokenRepo.findByToken(req.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid activation token"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Activation token expired");
        }

        User user = token.getUser();

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setEnabled(true);

        tokenRepo.delete(token);
    }

}

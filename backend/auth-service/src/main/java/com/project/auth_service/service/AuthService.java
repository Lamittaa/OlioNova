package com.project.auth_service.service;

import java.util.List;
import java.util.Optional;
import java.security.SecureRandom;
import java.time.Instant;

import com.project.auth_service.exception.EmployeeNotFoundException;
import com.project.auth_service.exception.InvalidTokenException;
import com.project.auth_service.exception.TokenExpiredException;
import com.project.auth_service.exception.UserNotFoundException;
import com.project.auth_service.exception.OtpExpiredException;
import com.project.auth_service.exception.OtpInvalidException;
import com.project.auth_service.model.ActivationToken;
import com.project.auth_service.model.Employee;
import com.project.auth_service.model.Token;
import com.project.auth_service.model.User;
import com.project.auth_service.repository.UserRepository;
import com.project.auth_service.repository.ActivationTokenRepository;
import com.project.auth_service.repository.EmployeeRepository;
import com.project.auth_service.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final ActivationTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepo;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService uds;
    private final TokenService tokenService;
    private final EmployeeRepository employeeRepo;
    private final EmailService emailService;

    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        log.info("Login attempt for username: {}", request.getUsername());
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));
        } catch (BadCredentialsException ex) {
            log.warn("Failed login for username: {}", request.getUsername());
            throw ex;
        }
        UserDetails userDetails = uds.loadUserByUsername(request.getUsername());
        User user = userRepo.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found: " + request.getUsername()));
        Employee employee = employeeRepo.findByUserUsername(user.getUsername())
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee not found for user: " + user.getUsername()));
        List<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .toList();
        String accessToken = jwtService.generateAccessToken(
                user.getUsername(), authorities, user.getId());
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        long refreshTtlSeconds = jwtService.getRefreshTokenTtlSeconds();
        long accessTtlSeconds = jwtService.getAccessTokenTtlSeconds();
        tokenService.saveAccessToken(
                user,
                accessToken,
                Instant.now().plusSeconds(accessTtlSeconds),
                refreshToken,
                Instant.now().plusSeconds(refreshTtlSeconds));
        log.info("User {} logged in successfully with role {}",
                user.getUsername(), user.getRole().getName());
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
        Token oldToken;
        try {
            oldToken = tokenService.validateUsableRefreshToken(dto.getRefreshToken());
        } catch (Exception ex) {
            log.warn("Invalid refresh token attempt: {}", ex.getMessage());
            throw ex;
        }
        User user = oldToken.getUser();
        UserDetails userDetails = uds.loadUserByUsername(user.getUsername());
        Employee employee = employeeRepo.findByUserUsername(user.getUsername())
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee not found for user: " + user.getUsername()));
        List<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .toList();
        log.info("Rotating refresh token for user: {}", user.getUsername());
        String newAccess = jwtService.generateAccessToken(
                user.getUsername(), authorities, employee.getId());
        String newRefresh = jwtService.generateRefreshToken(userDetails);
        long refreshTtlSeconds = jwtService.getRefreshTokenTtlSeconds();
        long accessTtlSeconds = jwtService.getAccessTokenTtlSeconds();
        tokenService.saveAccessToken(
                user,
                newAccess,
                Instant.now().plusSeconds(accessTtlSeconds),
                newRefresh,
                Instant.now().plusSeconds(refreshTtlSeconds));
        log.info("Refresh token rotation successful for {}", user.getUsername());
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
@Transactional
public void forgotPassword(String usernameOrEmail) {

    log.info("Forgot password request for: {}", usernameOrEmail);

    User user;

    Optional<User> userOpt = userRepo.findByUsername(usernameOrEmail);

    if (userOpt.isPresent()) {
        user = userOpt.get();
    } else {
        // إذا مش username → اعتبره email
        Employee employee = employeeRepo.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found with username or email: " + usernameOrEmail));

        user = employee.getUser();
    }

    Employee employee = employeeRepo.findByUserUsername(user.getUsername())
            .orElseThrow(() -> new EmployeeNotFoundException(
                    "Employee not found for user: " + user.getUsername()));

    String otpCode = String.format("%06d",
            new SecureRandom().nextInt(999999));

    ActivationToken otpToken = tokenRepo.findByUser_Id(user.getId())
            .orElse(ActivationToken.builder()
                    .token(java.util.UUID.randomUUID().toString())
                    .user(user)
                    .build());

    otpToken.setToken(java.util.UUID.randomUUID().toString());
    otpToken.setExpiresAt(Instant.now().plusSeconds(600));
    otpToken.setOtpCode(otpCode);
    otpToken.setOtpExpiresAt(Instant.now().plusSeconds(600));
    otpToken.setOtpVerified(false);

    tokenRepo.save(otpToken);

    emailService.sendOtpEmail(
            employee.getEmail(),
            employee.getFirstName() + " " + employee.getLastName(),
            otpCode);

    log.info("OTP sent to employee email: {}", employee.getEmail());
}

   @Transactional
public String verifyOtp(String usernameOrEmail, String otp) {

    log.info("OTP verification attempt for: {}", usernameOrEmail);

    User user;

    // أولاً حاول كـ username
    Optional<User> userOpt = userRepo.findByUsername(usernameOrEmail);

    if (userOpt.isPresent()) {
        user = userOpt.get();
    } else {
        // إذا Email
        Employee employee = employeeRepo.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found with username or email: " + usernameOrEmail));

        user = employee.getUser();
    }

    ActivationToken otpToken = tokenRepo.findByUser_Id(user.getId())
            .orElseThrow(() -> new OtpInvalidException("No OTP found for this user"));

    if (otpToken.getOtpExpiresAt().isBefore(Instant.now())) {
        tokenRepo.delete(otpToken);
        throw new OtpExpiredException("OTP expired");
    }

    if (!otpToken.getOtpCode().equals(otp)) {
        throw new OtpInvalidException("Invalid OTP");
    }

    otpToken.setOtpVerified(true);
    tokenRepo.save(otpToken);

    log.info("OTP verified successfully for: {}", user.getUsername());

    return otpToken.getToken();
}
}
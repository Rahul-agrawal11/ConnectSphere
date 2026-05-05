package com.connectsphere.auth.service.impl;

import com.connectsphere.auth.dto.request.*;
import com.connectsphere.auth.dto.response.AuthResponse;
import com.connectsphere.auth.dto.response.UserProfileResponse;
import com.connectsphere.auth.entity.RefreshToken;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.enums.AccountStatus;
import com.connectsphere.auth.enums.AuthProvider;
import com.connectsphere.auth.enums.Role;
import com.connectsphere.auth.exception.*;
import com.connectsphere.auth.repository.RefreshTokenRepository;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtUtil;
import com.connectsphere.auth.service.AuthService;
import com.connectsphere.auth.dto.event.OtpEmailEvent;
import com.connectsphere.auth.dto.request.VerifyOtpRequest;
import com.connectsphere.auth.publisher.NotificationPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    private final OtpService otpService;
    private final NotificationPublisher notificationPublisher;
    private final ObjectMapper objectMapper;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Registration ────────────────────────────────────────────────────

    @Override
    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Email is already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(
                    "Username is already taken: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .status(AccountStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {} ({})", saved.getUsername(), saved.getEmail());
        return mapToProfileResponse(saved);
    }

    // ── Login ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmailOrUsername())
                .or(() -> userRepository.findByUsername(request.getEmailOrUsername()))
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid email/username or password"));

        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new InvalidCredentialsException(
                    "Your account has been suspended. Contact support.");
        }
        if (user.getStatus() == AccountStatus.DEACTIVATED) {
            throw new InvalidCredentialsException(
                    "Your account has been deactivated.");
        }
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new InvalidCredentialsException(
                    "Please log in using " + user.getProvider().name() + " OAuth.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email/username or password");
        }

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = createRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ── Logout ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(Long userId) {
        User user = findUserById(userId);
        refreshTokenRepository.deleteByUser(user);
        log.info("User logged out: {}", user.getEmail());
    }

    // ── Token Refresh ───────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken storedToken = refreshTokenRepository
                .findByToken(refreshTokenStr)
                .orElseThrow(() -> new InvalidTokenException(
                        "Refresh token not found. Please log in again."));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new InvalidTokenException(
                    "Refresh token has expired. Please log in again.");
        }

        User user = storedToken.getUser();
        String newAccessToken = jwtUtil.generateToken(user);

        // Rotate refresh token on use (security best practice)
        String newRefreshToken = createRefreshToken(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    // ── Profile ─────────────────────────────────────────────────────────

    @Override
    public UserProfileResponse getProfile(Long userId) {
        return mapToProfileResponse(findUserById(userId));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getUsername() != null &&
                !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException(
                        "Username already taken: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getProfilePicUrl() != null) {
            user.setProfilePicUrl(request.getProfilePicUrl());
        }

        return mapToProfileResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new InvalidCredentialsException(
                    "Password change is not available for OAuth2 accounts.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all refresh tokens — force re-login after password change
        refreshTokenRepository.deleteByUser(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    // ── Account Management ──────────────────────────────────────────────

    @Override
    @Transactional
    public void deactivateAccount(Long userId) {
        User user = findUserById(userId);
        user.setStatus(AccountStatus.DEACTIVATED);
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);
        log.info("Account deactivated: {}", user.getEmail());
    }

    // ── Search ──────────────────────────────────────────────────────────

    @Override
    public List<UserProfileResponse> searchUsers(String query) {
        List<User> byUsername = userRepository.searchByUsername(query);
        List<User> byFullName = userRepository.searchByFullName(query);

        // Merge and deduplicate
        return byUsername.stream()
                .filter(u -> byFullName.stream()
                        .noneMatch(f -> f.getId().equals(u.getId())))
                .collect(Collectors.toCollection(() ->
                        new java.util.ArrayList<>(byUsername)))
                .stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    // ── Admin Operations ────────────────────────────────────────────────

    @Override
    @Transactional
    public void suspendUser(Long targetUserId) {
        User user = findUserById(targetUserId);
        user.setStatus(AccountStatus.SUSPENDED);
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);
        log.info("User suspended by admin: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void reactivateUser(Long targetUserId) {
        User user = findUserById(targetUserId);
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        log.info("User reactivated by admin: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void deleteUser(Long targetUserId) {
        User user = findUserById(targetUserId);
        refreshTokenRepository.deleteByUser(user);
        userRepository.delete(user);
        log.info("User permanently deleted by admin: {}", targetUserId);
    }

    @Override
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserProfileResponse getUserById(Long userId) {
        return mapToProfileResponse(findUserById(userId));
    }

    @Override
    public String sendOtp(RegisterRequest request) {
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already registered: " + request.getEmail());
        }

        if(userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken: " + request.getUsername());
        }

        try {
            String userData = objectMapper.writeValueAsString(request);
            otpService.storePendingUser(request.getEmail(), userData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process registration request");
        }

        String otp = otpService.generateAndStoreOtp(request.getEmail());
        notificationPublisher.sendOtpEmail(request.getEmail(), request.getFullName(), otp);
        return "OTp sent to " + request.getEmail() + ". Valid for 5 minutes.";
    }

    @Override
    public UserProfileResponse verifyOtpAndRegister(VerifyOtpRequest request) {

        // Validate OTP
        if(!otpService.validateOtp(request.getEmail(), request.getOtp())) {
            throw new InvalidCredentialsException("Invalid or expired OTP!");
        }

        // Fetch pending user data from Redis
        String userData = otpService.getPendingUser(request.getEmail());
        if(userData == null) {
            throw new InvalidCredentialsException("Registration session expired. Please register again.");
        }

        try {
            RegisterRequest registerRequest = objectMapper.readValue(userData, RegisterRequest.class);

            // Save user permanently to DB
            User user = User.builder()
                    .username(registerRequest.getUsername())
                    .email(registerRequest.getEmail())
                    .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                    .fullName(registerRequest.getFullName())
                    .role(Role.USER)
                    .provider(AuthProvider.LOCAL)
                    .status(AccountStatus.ACTIVE)
                    .build();

            User saved = userRepository.save(user);

            // Cleanup Redis
            otpService.deletePendingUser(request.getEmail());

            log.info("New user registered after OTP: {} ({})", saved.getUsername(), saved.getBio());
            return mapToProfileResponse(saved);
        } catch (Exception e) {
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }

    }

    // ── Private Helpers ─────────────────────────────────────────────────

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found with id: " + userId));
    }

//    private String createRefreshToken(User user) {
//        // Delete any existing refresh token for this user
//        refreshTokenRepository.findByUser(user)
//                .ifPresent(refreshTokenRepository::delete);
//
//        RefreshToken refreshToken = RefreshToken.builder()
//                .token(UUID.randomUUID().toString())
//                .user(user)
//                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
//                .build();
//
//        return refreshTokenRepository.save(refreshToken).getToken();
//    }

    private String createRefreshToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusMillis(refreshExpirationMs);

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .map(existing -> {
                    existing.setToken(tokenValue);
                    existing.setExpiresAt(expiry);
                    return existing;
                })
                .orElseGet(() -> RefreshToken.builder()
                        .token(tokenValue)
                        .user(user)
                        .expiresAt(expiry)
                        .build());

        return refreshTokenRepository.save(refreshToken).getToken();
    }

    private AuthResponse buildAuthResponse(User user, String accessToken,
                                           String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .bio(user.getBio())
                .profilePicUrl(user.getProfilePicUrl())
                .role(user.getRole().name())
                .provider(user.getProvider().name())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
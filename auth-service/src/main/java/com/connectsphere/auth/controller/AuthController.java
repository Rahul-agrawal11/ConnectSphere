package com.connectsphere.auth.controller;

import com.connectsphere.auth.dto.request.*;
import com.connectsphere.auth.dto.response.ApiResponse;
import com.connectsphere.auth.dto.response.AuthResponse;
import com.connectsphere.auth.dto.response.UserProfileResponse;
import com.connectsphere.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Auth REST controller — all endpoints versioned under /api/v1/auth/
 *
 * User identity headers injected by the gateway:
 *   X-User-Id   → authenticated user's database ID
 *   X-User-Role → USER or ADMIN
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, profile and admin endpoints")
public class AuthController {

    private final AuthService authService;

    // ── Public Endpoints ─────────────────────────────────────────────────

    @Operation(summary = "Send OTP to email for registration")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @Valid @RequestBody RegisterRequest request) {

//        UserProfileResponse profile = authService.register(request);
        String message = authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @Operation(summary = "Verify OTP and complete registration")
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<UserProfileResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        UserProfileResponse profile = authService.verifyOtpAndRegister(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User registered successfully", profile));
    }

    @Operation(summary = "Login with email/username and password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", auth));
    }

    @Operation(summary = "Refresh access token using a refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestParam String refreshToken) {

        AuthResponse auth = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", auth));
    }

    @Operation(summary = "Search users by username or full name")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> search(
            @RequestParam String query) {

        List<UserProfileResponse> results = authService.searchUsers(query);
        return ResponseEntity.ok(ApiResponse.success("Search results", results));
    }

    // ── Authenticated Endpoints ──────────────────────────────────────────

    @Operation(summary = "Logout — invalidates refresh token",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("X-User-Id") Long userId) {

        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @Operation(summary = "Get current user profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @RequestHeader("X-User-Id") Long userId) {

        UserProfileResponse profile = authService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", profile));
    }

    @Operation(summary = "Get any user's public profile by ID")
    @GetMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfileById(
            @PathVariable Long userId) {

        UserProfileResponse profile = authService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", profile));
    }

    @Operation(summary = "Update current user profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserProfileResponse updated = authService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", updated));
    }

    @Operation(summary = "Change password",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed. Please log in again."));
    }

    @Operation(summary = "Deactivate own account",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @RequestHeader("X-User-Id") Long userId) {

        authService.deactivateAccount(userId);
        return ResponseEntity.ok(ApiResponse.success("Account deactivated"));
    }

    // ── Admin Endpoints ──────────────────────────────────────────────────

    @Operation(summary = "[ADMIN] Get all users")
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllUsers() {
        return ResponseEntity.ok(
                ApiResponse.success("All users", authService.getAllUsers()));
    }

    @Operation(summary = "[ADMIN] Suspend a user account")
    @PutMapping("/admin/users/{targetUserId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long targetUserId) {
        authService.suspendUser(targetUserId);
        return ResponseEntity.ok(ApiResponse.success("User suspended"));
    }

    @Operation(summary = "[ADMIN] Reactivate a suspended user")
    @PutMapping("/admin/users/{targetUserId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(
            @PathVariable Long targetUserId) {
        authService.reactivateUser(targetUserId);
        return ResponseEntity.ok(ApiResponse.success("User reactivated"));
    }

    @Operation(summary = "[ADMIN] Permanently delete a user")
    @DeleteMapping("/admin/users/{targetUserId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long targetUserId) {
        authService.deleteUser(targetUserId);
        return ResponseEntity.ok(ApiResponse.success("User deleted"));
    }
}
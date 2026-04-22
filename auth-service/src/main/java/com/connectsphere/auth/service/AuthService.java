package com.connectsphere.auth.service;

import com.connectsphere.auth.dto.request.*;
import com.connectsphere.auth.dto.response.AuthResponse;
import com.connectsphere.auth.dto.response.UserProfileResponse;
import com.connectsphere.auth.entity.User;

import java.util.List;

/**
 * Auth service contract.
 * Defines all authentication and user management operations.
 */
public interface AuthService {

    // Authentication
    UserProfileResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(Long userId);
    AuthResponse refreshToken(String refreshToken);

    // Profile
    UserProfileResponse getProfile(Long userId);
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);

    // Account management
    void deactivateAccount(Long userId);

    // Search and discovery
    List<UserProfileResponse> searchUsers(String query);

    // Admin operations
    void suspendUser(Long targetUserId);
    void reactivateUser(Long targetUserId);
    void deleteUser(Long targetUserId);
    List<UserProfileResponse> getAllUsers();

    // Internal helper used by OAuth2 success handler
    UserProfileResponse getUserById(Long userId);
}
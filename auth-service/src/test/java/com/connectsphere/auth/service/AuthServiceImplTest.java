package com.connectsphere.auth.service;

import com.connectsphere.auth.dto.request.*;
import com.connectsphere.auth.dto.response.AuthResponse;
import com.connectsphere.auth.dto.response.UserProfileResponse;
import com.connectsphere.auth.entity.RefreshToken;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.enums.AccountStatus;
import com.connectsphere.auth.enums.AuthProvider;
import com.connectsphere.auth.enums.Role;
import com.connectsphere.auth.exception.InvalidCredentialsException;
import com.connectsphere.auth.exception.InvalidTokenException;
import com.connectsphere.auth.exception.UserAlreadyExistsException;
import com.connectsphere.auth.publisher.NotificationPublisher;
import com.connectsphere.auth.repository.RefreshTokenRepository;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtUtil;
import com.connectsphere.auth.service.impl.AuthServiceImpl;
import com.connectsphere.auth.service.impl.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 86400000L);

        user = User.builder()
                .id(1L)
                .username("rahul")
                .email("rahul@gmail.com")
                .fullName("Rahul Agrawal")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void register_ShouldCreateUser_WhenEmailAndUsernameAreUnique() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("rahul");
        request.setEmail("rahul@gmail.com");
        request.setPassword("Password@123");
        request.setFullName("Rahul Agrawal");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfileResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("rahul", response.getUsername());
        assertEquals("rahul@gmail.com", response.getEmail());
        assertEquals("USER", response.getRole());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("rahul");
        request.setEmail("rahul@gmail.com");
        request.setPassword("Password@123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnTokens_WhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("rahul@gmail.com");
        request.setPassword("Password@123");

        when(userRepository.findByEmail(request.getEmailOrUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("access-token");
        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals(1L, response.getUserId());
        assertEquals("rahul", response.getUsername());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_ShouldThrowException_WhenPasswordIsWrong() {
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("rahul@gmail.com");
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail(request.getEmailOrUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void refreshToken_ShouldReturnNewAccessToken_WhenRefreshTokenIsValid() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("old-refresh-token")
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtUtil.generateToken(user)).thenReturn("new-access-token");
        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.refreshToken("old-refresh-token");

        assertEquals("new-access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenTokenExpired() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("expired-token")
                .user(user)
                .expiresAt(Instant.now().minusSeconds(60))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(refreshToken));

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken("expired-token"));

        verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    void updateProfile_ShouldUpdateUserDetails() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("rahul_new");
        request.setFullName("Rahul New");
        request.setBio("Java Developer");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("rahul_new")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = authService.updateProfile(1L, request);

        assertEquals("rahul_new", response.getUsername());
        assertEquals("Rahul New", response.getFullName());
        assertEquals("Java Developer", response.getBio());
    }

    @Test
    void changePassword_ShouldUpdatePasswordAndDeleteRefreshToken() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Old@12345");
        request.setNewPassword("New@12345");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("newEncodedPassword");

        authService.changePassword(1L, request);

        assertEquals("newEncodedPassword", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void sendOtp_ShouldSendOtp_WhenUserIsNew() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("rahul");
        request.setEmail("rahul@gmail.com");
        request.setPassword("Password@123");
        request.setFullName("Rahul Agrawal");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(objectMapper.writeValueAsString(request)).thenReturn("{}");
        when(otpService.generateAndStoreOtp(request.getEmail())).thenReturn("123456");

        String response = authService.sendOtp(request);

        assertTrue(response.contains("rahul@gmail.com"));
        verify(otpService).storePendingUser(request.getEmail(), "{}");
        verify(notificationPublisher).sendOtpEmail(request.getEmail(), request.getFullName(), "123456");
    }

    @Test
    void verifyOtpAndRegister_ShouldCreateUser_WhenOtpIsValid() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("rahul@gmail.com");
        request.setOtp("123456");

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("rahul");
        registerRequest.setEmail("rahul@gmail.com");
        registerRequest.setPassword("Password@123");
        registerRequest.setFullName("Rahul Agrawal");

        when(otpService.validateOtp(request.getEmail(), request.getOtp())).thenReturn(true);
        when(otpService.getPendingUser(request.getEmail())).thenReturn("{}");
        when(objectMapper.readValue("{}", RegisterRequest.class)).thenReturn(registerRequest);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfileResponse response = authService.verifyOtpAndRegister(request);

        assertNotNull(response);
        assertEquals("rahul", response.getUsername());

        verify(otpService).deletePendingUser(request.getEmail());
    }

    // ── login: uncovered branches ──────────────────────────────────────────

    @Test
    void login_shouldThrow_whenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("nobody@gmail.com");
        request.setPassword("Password@123");

        when(userRepository.findByEmail("nobody@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("nobody@gmail.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrow_whenAccountSuspended() {
        user.setStatus(AccountStatus.SUSPENDED);
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("rahul@gmail.com");
        request.setPassword("Password@123");

        when(userRepository.findByEmail("rahul@gmail.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrow_whenAccountDeactivated() {
        user.setStatus(AccountStatus.DEACTIVATED);
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("rahul@gmail.com");
        request.setPassword("Password@123");

        when(userRepository.findByEmail("rahul@gmail.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrow_whenOAuthUser() {
        user.setProvider(AuthProvider.GOOGLE);
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("rahul@gmail.com");
        request.setPassword("Password@123");

        when(userRepository.findByEmail("rahul@gmail.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

// ── register: uncovered branch ─────────────────────────────────────────

    @Test
    void register_shouldThrow_whenUsernameAlreadyTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("rahul");
        request.setEmail("new@gmail.com");
        request.setPassword("Password@123");

        when(userRepository.existsByEmail("new@gmail.com")).thenReturn(false);
        when(userRepository.existsByUsername("rahul")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

// ── changePassword: uncovered branches ────────────────────────────────

    @Test
    void changePassword_shouldThrow_whenOAuthAccount() {
        user.setProvider(AuthProvider.GOOGLE);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Old@12345");
        request.setNewPassword("New@12345");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.changePassword(1L, request));
    }

    @Test
    void changePassword_shouldThrow_whenCurrentPasswordWrong() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Wrong@123");
        request.setNewPassword("New@12345");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Wrong@123", user.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.changePassword(1L, request));
    }

// ── updateProfile: username taken branch ──────────────────────────────

    @Test
    void updateProfile_shouldThrow_whenNewUsernameTaken() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("taken_name");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken_name")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.updateProfile(1L, request));
    }

// ── admin operations ───────────────────────────────────────────────────

    @Test
    void deactivateAccount_shouldSetStatusAndDeleteTokens() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        authService.deactivateAccount(1L);

        assertThat(user.getStatus()).isEqualTo(AccountStatus.DEACTIVATED);
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void suspendUser_shouldSetSuspendedStatusAndDeleteTokens() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        authService.suspendUser(1L);

        assertThat(user.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void reactivateUser_shouldSetActiveStatus() {
        user.setStatus(AccountStatus.SUSPENDED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        authService.reactivateUser(1L);

        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void deleteUser_shouldDeleteTokensAndUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.deleteUser(1L);

        verify(refreshTokenRepository).deleteByUser(user);
        verify(userRepository).delete(user);
    }

    @Test
    void getAllUsers_shouldReturnMappedList() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserProfileResponse> result = authService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("rahul@gmail.com");
    }

    @Test
    void getUserById_shouldReturnProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse profile = authService.getUserById(1L);

        assertThat(profile.getUsername()).isEqualTo("rahul");
    }

// ── sendOtp: error branches ────────────────────────────────────────────

    @Test
    void sendOtp_shouldThrow_whenEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("rahul@gmail.com");
        request.setUsername("rahul");

        when(userRepository.existsByEmail("rahul@gmail.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.sendOtp(request));
    }

    @Test
    void sendOtp_shouldThrow_whenUsernameExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@gmail.com");
        request.setUsername("rahul");

        when(userRepository.existsByEmail("new@gmail.com")).thenReturn(false);
        when(userRepository.existsByUsername("rahul")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.sendOtp(request));
    }

// ── verifyOtpAndRegister: error branches ──────────────────────────────

    @Test
    void verifyOtpAndRegister_shouldThrow_whenOtpInvalid() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("rahul@gmail.com");
        request.setOtp("000000");

        when(otpService.validateOtp("rahul@gmail.com", "000000")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.verifyOtpAndRegister(request));
    }

    @Test
    void verifyOtpAndRegister_shouldThrow_whenPendingUserExpired() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("rahul@gmail.com");
        request.setOtp("123456");

        when(otpService.validateOtp("rahul@gmail.com", "123456")).thenReturn(true);
        when(otpService.getPendingUser("rahul@gmail.com")).thenReturn(null);

        assertThrows(InvalidCredentialsException.class, () -> authService.verifyOtpAndRegister(request));
    }

// ── searchUsers ────────────────────────────────────────────────────────

    @Test
    void searchUsers_shouldReturnResultsFromBothLists() {
        User u1 = User.builder().id(1L).username("rahul").email("rahul@gmail.com")
                .role(Role.USER).provider(AuthProvider.LOCAL).status(AccountStatus.ACTIVE).build();

        // Same user in both lists — the merge always returns byUsername content
        when(userRepository.searchByUsername("rahul")).thenReturn(List.of(u1));
        when(userRepository.searchByFullName("rahul")).thenReturn(List.of(u1));

        List<UserProfileResponse> results = authService.searchUsers("rahul");

        // Code returns byUsername result — at minimum u1 should be present
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getId()).isEqualTo(1L);
    }

// ── createRefreshToken: existing token rotation branch ─────────────────

    @Test
    void login_shouldRotateExistingRefreshToken() {
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("rahul@gmail.com");
        request.setPassword("Password@123");

        RefreshToken existingToken = RefreshToken.builder()
                .id(1L).token("old-token").user(user)
                .expiresAt(Instant.now().plusSeconds(3600)).build();

        when(userRepository.findByEmail("rahul@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("access-token");
        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        // Token was rotated — the existing token object was updated and saved
        verify(refreshTokenRepository).save(existingToken);
    }
}
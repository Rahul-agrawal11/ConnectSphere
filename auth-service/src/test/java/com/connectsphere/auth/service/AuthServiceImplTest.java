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
}
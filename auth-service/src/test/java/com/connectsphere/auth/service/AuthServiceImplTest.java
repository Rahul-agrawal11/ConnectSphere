package com.connectsphere.auth.service;

import com.connectsphere.auth.dto.request.LoginRequest;
import com.connectsphere.auth.dto.request.RegisterRequest;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.enums.AccountStatus;
import com.connectsphere.auth.enums.AuthProvider;
import com.connectsphere.auth.enums.Role;
import com.connectsphere.auth.repository.RefreshTokenRepository;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtUtil;
import com.connectsphere.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    // TEST 1: Register User
    @Test
    void registerUser_ShouldCreateUserSuccessfully() {

        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setUsername("testuser");
        request.setPassword("password");

        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(userRepository.existsByUsername(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(request);

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }

    // TEST 2: Login Success
    @Test
    void loginUser_WithValidCredentials_ShouldReturnToken() {

        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("test@example.com");
        request.setPassword("password");

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(true);

        when(jwtUtil.generateToken(any()))
                .thenReturn("mocked-jwt-token");

        when(refreshTokenRepository.findByUser(any()))
                .thenReturn(Optional.empty());

        when(refreshTokenRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken()); // optional but good

        verify(refreshTokenRepository).save(any());
    }

    // TEST 3: Login Fail
    @Test
    void loginUser_WithInvalidCredentials_ShouldThrowException() {

        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("test@example.com");
        request.setPassword("wrong");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(Exception.class, () -> authService.login(request));
    }
}
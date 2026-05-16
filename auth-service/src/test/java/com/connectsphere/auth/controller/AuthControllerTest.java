package com.connectsphere.auth.controller;

import com.connectsphere.auth.dto.request.*;
import com.connectsphere.auth.dto.response.AuthResponse;
import com.connectsphere.auth.dto.response.UserProfileResponse;
import com.connectsphere.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        objectMapper = new ObjectMapper();
        mockMvc = standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void register_ShouldReturnOtpMessage() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("rahul");
        request.setEmail("rahul@gmail.com");
        request.setPassword("Password@123");
        request.setFullName("Rahul Agrawal");

        when(authService.sendOtp(any(RegisterRequest.class)))
                .thenReturn("OTP sent to rahul@gmail.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP sent to rahul@gmail.com"));
    }

    @Test
    void verifyOtp_ShouldReturnCreatedUser() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("rahul@gmail.com");
        request.setOtp("123456");

        UserProfileResponse profile = UserProfileResponse.builder()
                .id(1L)
                .username("rahul")
                .email("rahul@gmail.com")
                .role("USER")
                .build();

        when(authService.verifyOtpAndRegister(any(VerifyOtpRequest.class))).thenReturn(profile);

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("rahul"));
    }

    @Test
    void login_ShouldReturnAuthResponse() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("rahul@gmail.com");
        request.setPassword("Password@123");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .userId(1L)
                .username("rahul")
                .email("rahul@gmail.com")
                .role("USER")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.username").value("rahul"));
    }

    @Test
    void refresh_ShouldReturnNewToken() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .userId(1L)
                .username("rahul")
                .email("rahul@gmail.com")
                .role("USER")
                .build();

        when(authService.refreshToken("refresh-token")).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .param("refreshToken", "refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    @Test
    void getProfile_ShouldReturnUserProfile() throws Exception {
        UserProfileResponse profile = UserProfileResponse.builder()
                .id(1L)
                .username("rahul")
                .email("rahul@gmail.com")
                .role("USER")
                .build();

        when(authService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/auth/profile")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("rahul@gmail.com"));
    }

    @Test
    void updateProfile_ShouldReturnUpdatedProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("rahul_new");
        request.setFullName("Rahul New");

        UserProfileResponse profile = UserProfileResponse.builder()
                .id(1L)
                .username("rahul_new")
                .email("rahul@gmail.com")
                .fullName("Rahul New")
                .build();

        when(authService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(profile);

        mockMvc.perform(put("/api/v1/auth/profile")
                        .header("X-User-Id", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("rahul_new"));
    }

    @Test
    void search_ShouldReturnUsers() throws Exception {
        UserProfileResponse profile = UserProfileResponse.builder()
                .id(1L)
                .username("rahul")
                .email("rahul@gmail.com")
                .build();

        when(authService.searchUsers("rahul")).thenReturn(List.of(profile));

        mockMvc.perform(get("/api/v1/auth/search")
                        .param("query", "rahul"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("rahul"));
    }

    @Test
    void logout_ShouldReturnSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).logout(1L);
    }
}
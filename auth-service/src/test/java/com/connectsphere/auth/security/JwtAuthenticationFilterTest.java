package com.connectsphere.auth.security;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.enums.AccountStatus;
import com.connectsphere.auth.enums.AuthProvider;
import com.connectsphere.auth.enums.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtUtil jwtUtil;
    @Mock UserDetailsServiceImpl userDetailsService;
    @Mock FilterChain filterChain;

    @InjectMocks
    JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        // Clear security context between tests
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_withNoAuthHeader_shouldPassThroughWithoutAuthentication()
            throws ServletException, IOException {
        // No Authorization header set
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_withNonBearerHeader_shouldPassThrough()
            throws ServletException, IOException {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void doFilter_withValidToken_shouldSetAuthentication()
            throws ServletException, IOException {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        Claims claims = mock(Claims.class);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractUserId(token)).thenReturn(1L);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(claims.get("email", String.class)).thenReturn("rahul@gmail.com");

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("rahul@gmail.com")
                .password("encoded")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        when(userDetailsService.loadUserByUsername("rahul@gmail.com")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("rahul@gmail.com");
    }

    @Test
    void doFilter_withInvalidToken_shouldNotSetAuthentication()
            throws ServletException, IOException {
        String token = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.isTokenValid(token)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_withValidTokenButNullEmail_shouldNotSetAuthentication()
            throws ServletException, IOException {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        Claims claims = mock(Claims.class);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractUserId(token)).thenReturn(1L);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(claims.get("email", String.class)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_whenUserDetailsServiceThrows_shouldStillCallFilterChain()
            throws ServletException, IOException {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        Claims claims = mock(Claims.class);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractUserId(token)).thenReturn(1L);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(claims.get("email", String.class)).thenReturn("rahul@gmail.com");
        when(userDetailsService.loadUserByUsername("rahul@gmail.com"))
                .thenThrow(new RuntimeException("DB error"));

        // Exception must be swallowed — filter chain must still proceed
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
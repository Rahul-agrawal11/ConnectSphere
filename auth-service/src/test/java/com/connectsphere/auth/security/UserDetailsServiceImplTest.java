package com.connectsphere.auth.security;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.enums.AccountStatus;
import com.connectsphere.auth.enums.AuthProvider;
import com.connectsphere.auth.enums.Role;
import com.connectsphere.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserDetailsServiceImpl userDetailsService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .username("rahul")
                .email("rahul@gmail.com")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void loadUserByUsername_byEmail_shouldReturnUserDetails() {
        when(userRepository.findByEmail("rahul@gmail.com")).thenReturn(Optional.of(activeUser));

        UserDetails details = userDetailsService.loadUserByUsername("rahul@gmail.com");

        assertThat(details.getUsername()).isEqualTo("rahul@gmail.com");
        assertThat(details.getPassword()).isEqualTo("encodedPassword");
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    @Test
    void loadUserByUsername_byUsername_shouldReturnUserDetails() {
        when(userRepository.findByEmail("rahul")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("rahul")).thenReturn(Optional.of(activeUser));

        UserDetails details = userDetailsService.loadUserByUsername("rahul");

        assertThat(details.getUsername()).isEqualTo("rahul@gmail.com");
    }

    @Test
    void loadUserByUsername_notFound_shouldThrowUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void loadUserByUsername_suspendedUser_shouldReturnLockedAccount() {
        User suspended = User.builder()
                .id(2L).username("suspended").email("sus@gmail.com")
                .passwordHash("hash").role(Role.USER)
                .provider(AuthProvider.LOCAL).status(AccountStatus.SUSPENDED)
                .build();
        when(userRepository.findByEmail("sus@gmail.com")).thenReturn(Optional.of(suspended));

        UserDetails details = userDetailsService.loadUserByUsername("sus@gmail.com");

        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    void loadUserByUsername_deactivatedUser_shouldReturnDisabledAccount() {
        User deactivated = User.builder()
                .id(3L).username("deact").email("deact@gmail.com")
                .passwordHash("hash").role(Role.USER)
                .provider(AuthProvider.LOCAL).status(AccountStatus.DEACTIVATED)
                .build();
        when(userRepository.findByEmail("deact@gmail.com")).thenReturn(Optional.of(deactivated));

        UserDetails details = userDetailsService.loadUserByUsername("deact@gmail.com");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_oauthUserNullPassword_shouldUseEmptyPassword() {
        User oauthUser = User.builder()
                .id(4L).username("googleuser").email("google@gmail.com")
                .passwordHash(null)   // OAuth2 users have no password
                .role(Role.USER)
                .provider(AuthProvider.GOOGLE).status(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findByEmail("google@gmail.com")).thenReturn(Optional.of(oauthUser));

        UserDetails details = userDetailsService.loadUserByUsername("google@gmail.com");

        assertThat(details.getPassword()).isEmpty();
    }
}
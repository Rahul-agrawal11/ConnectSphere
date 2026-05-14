package com.connectsphere.auth.repository;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.enums.AccountStatus;
import com.connectsphere.auth.enums.AuthProvider;
import com.connectsphere.auth.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User createUser() {
        return User.builder()
                .username("rahul11")
                .email("rahul11@gmail.com")
                .fullName("Rahul Agrawal")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should find user by email")
    void findByEmail_ShouldReturnUser() {
        User savedUser = userRepository.save(createUser());

        Optional<User> found = userRepository.findByEmail(savedUser.getEmail());

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("rahul11");
    }

    @Test
    @DisplayName("Should find user by username")
    void findByUsername_ShouldReturnUser() {
        User savedUser = userRepository.save(createUser());

        Optional<User> found = userRepository.findByUsername(savedUser.getUsername());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("rahul11@gmail.com");
    }

    @Test
    @DisplayName("Should check email exists")
    void existsByEmail_ShouldReturnTrue() {
        userRepository.save(createUser());

        boolean exists = userRepository.existsByEmail("rahul11@gmail.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should check username exists")
    void existsByUsername_ShouldReturnTrue() {
        userRepository.save(createUser());

        boolean exists = userRepository.existsByUsername("rahul11");

        assertThat(exists).isTrue();
    }
}
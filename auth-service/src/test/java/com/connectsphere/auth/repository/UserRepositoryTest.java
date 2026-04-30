package com.connectsphere.auth.repository;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.enums.AccountStatus;
import com.connectsphere.auth.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User createUser() {
        User user = new User();
        user.setUsername("Rahul011");
        user.setEmail("rahul@gmail.com");
        user.setFullName("Test User");
        user.setPasswordHash("password");
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }

    @Test
    @DisplayName("Should find user by email")
    void findByEmail_ShouldReturnUser() {
        User user = createUser();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("rahul@gmail.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("Rahul011");
    }

}

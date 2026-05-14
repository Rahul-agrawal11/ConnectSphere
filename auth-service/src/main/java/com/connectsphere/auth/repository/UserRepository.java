package com.connectsphere.auth.repository;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.enums.AccountStatus;
import com.connectsphere.auth.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByRole(Role role);

    List<User> findAllByStatus(AccountStatus status);

    // Case-insensitive username search for user discovery
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "AND u.status = 'ACTIVE'")
    List<User> searchByUsername(@Param("query") String query);

    // Case-insensitive full name search
    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "AND u.status = 'ACTIVE'")
    List<User> searchByFullName(@Param("query") String query);

    Optional<User> findByProviderAndProviderId(
            com.connectsphere.auth.enums.AuthProvider provider,
            String providerId);
}
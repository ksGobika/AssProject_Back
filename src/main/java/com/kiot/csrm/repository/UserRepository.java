package com.kiot.csrm.repository;

import com.kiot.csrm.entity.Role;
import com.kiot.csrm.entity.User;
import com.kiot.csrm.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByStatus(UserStatus status);
    List<User> findByRole(Role role);
    long countByStatus(UserStatus status);
}

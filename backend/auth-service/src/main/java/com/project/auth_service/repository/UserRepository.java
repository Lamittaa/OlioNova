package com.project.auth_service.repository;

import com.project.auth_service.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"role", "role.authorities"})
    Optional<User> findByUsername(String username);
     boolean existsByUsername(String username);
    boolean existsByRole_Id(Long roleId);
}

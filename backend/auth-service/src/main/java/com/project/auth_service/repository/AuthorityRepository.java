package com.project.auth_service.repository;

import com.project.auth_service.model.Authority;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {
 Optional<Authority> findByNameIgnoreCase(String name);
    List<Authority> findByNameInIgnoreCase(List<String> names);
}

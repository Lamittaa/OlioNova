package com.project.auth_service.repository;

import com.project.auth_service.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // ===================== Exists Checks =====================

    boolean existsByNationalId(String nationalId);

    boolean existsByEmail(String email);

    // ===================== Profile =====================

    Optional<Employee> findByUserUsername(String username);

    // ===================== Search =====================

    Optional<Employee> findByNationalId(String nationalId);
}

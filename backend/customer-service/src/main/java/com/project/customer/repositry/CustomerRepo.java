package com.project.customer.repositry;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.customer.model.Customer;

@Repository
public interface  CustomerRepo extends JpaRepository<Customer,Long> {

   Boolean existsByNationalId(String nationalId);
   Optional<Customer> findByNationalId(String nationalId);
} 

package com.project.order.repository;

import com.project.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderStatusRepo extends JpaRepository<OrderStatus, Long> {
   Optional<OrderStatus> findByStatusNameIgnoreCase(String statusName);
    boolean existsByStatusNameIgnoreCase(String statusName);
}

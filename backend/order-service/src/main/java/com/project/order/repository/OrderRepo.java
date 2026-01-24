package com.project.order.repository;

import com.project.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepo extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);



    List<Order> findByCustomerIdAndCreatedAtBetween(Long customerId, LocalDateTime from, LocalDateTime to);
}

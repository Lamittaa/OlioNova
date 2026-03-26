package com.project.order.repository;

import com.project.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepo extends JpaRepository<OrderItem, Long> {
  List<OrderItem> findByOrderId(Long orderId);

    Optional<OrderItem> findByIdAndOrderId(Long id, Long orderId);

    void deleteByIdAndOrderId(Long id, Long orderId);
    List<OrderItem> findAllByIdIn(List<Long> ids);
}

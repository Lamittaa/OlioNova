package com.project.payment.repository;

import com.project.payment.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
Optional<Payment> findByOrderId(Long orderId);
   // ===== للتقرير اليومي =====
    List<Payment> findByPaymentDateBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    // ===== مجموع المبلغ =====
    @Query("""
        SELECT COALESCE(SUM(p.totalPrice), 0)
        FROM Payment p
        WHERE p.paymentDate BETWEEN :start AND :end
    """)
    BigDecimal sumTotalBetween(LocalDateTime start, LocalDateTime end);
}

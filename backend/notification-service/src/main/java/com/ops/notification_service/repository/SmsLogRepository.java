package com.ops.notification_service.repository;

import com.ops.notification_service.model.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    boolean existsByOrderIdAndSmsSentTrue(Long orderId);
}

package com.ops.tracking_service.repository;

import com.ops.tracking_service.model.BatchTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BatchTrackingRepository extends JpaRepository<BatchTracking, String> {
    Optional<BatchTracking> findByTrackingCode(String trackingCode);

    boolean existsByTrackingCode(String trackingCode);
}

package com.zaytoun.aiprediction.repository;

import com.zaytoun.aiprediction.model.PredictionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionLogRepository extends JpaRepository<PredictionLog, Long> {
}

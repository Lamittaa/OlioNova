package com.project.queue_service.repository;

import com.project.queue_service.model.QueueCounter;
import com.project.queue_service.model.QueueType;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface QueueCounterRepo extends JpaRepository<QueueCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QueueCounter> findByQueueTypeAndQueueDate(QueueType queueType, LocalDate queueDate);
}
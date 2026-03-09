package com.project.queue_service.repository;

import com.project.queue_service.model.QueueCounter;
import com.project.queue_service.model.QueueType;

import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

@Repository
public interface QueueCounterRepo extends JpaRepository<QueueCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QueueCounter> findByQueueTypeAndQueueDate(QueueType queueType, LocalDate queueDate);
}
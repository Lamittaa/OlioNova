package com.project.queue_service.service;

import com.project.queue_service.model.QueueCounter;
import com.project.queue_service.repository.QueueCounterRepo;
import com.project.queue_service.repository.QueueTicketRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
class QueueCounterCreator {
    private final QueueCounterRepo queueCounterRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QueueCounter createCounterRow(String queueType, LocalDate day) {
        QueueCounter counter = new QueueCounter();
        counter.setQueueType(queueType);
        counter.setQueueDate(day);
        counter.setNextTicketNumber(1);
        return queueCounterRepo.save(counter);
    }
}
package com.project.queue_service.service;

import com.project.queue_service.model.QueueCounter;
import com.project.queue_service.repository.QueueCounterRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueCounterService {

    private final QueueCounterRepo queueCounterRepo;
    private final QueueCounterCreator queueCounterCreator;

    @Transactional
    public Integer getAndIncrementTicketNumber(String queueType) {
        LocalDate today = LocalDate.now();

        QueueCounter counter = queueCounterRepo.findByQueueTypeAndQueueDate(queueType, today)
                .orElseGet(() -> {
                    try {
                        queueCounterCreator.createCounterRow(queueType, today);
                    } catch (DataIntegrityViolationException ex) {
                    }
                    return queueCounterRepo.findByQueueTypeAndQueueDate(queueType, today)
                            .orElseThrow(() ->
                                    new IllegalStateException("Unable to find queue counter or create a new one!"));
                });


        int ticketNumber = counter.getNextTicketNumber();

        counter.setNextTicketNumber(ticketNumber + 1);
        queueCounterRepo.save(counter);

        return ticketNumber;
    }

}

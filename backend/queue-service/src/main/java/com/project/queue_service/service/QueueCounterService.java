package com.project.queue_service.service;

import com.project.queue_service.model.QueueCounter;
import com.project.queue_service.model.QueueType;
import com.project.queue_service.repository.QueueCounterRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class QueueCounterService {

    private static final Logger log = LoggerFactory.getLogger(QueueCounterService.class);

    private final QueueCounterRepo queueCounterRepo;
    private final QueueCounterCreator queueCounterCreator;

    public QueueCounterService(QueueCounterRepo queueCounterRepo, QueueCounterCreator queueCounterCreator) {
        this.queueCounterRepo = queueCounterRepo;
        this.queueCounterCreator = queueCounterCreator;
    }

    @Transactional
    public Integer getAndIncrementTicketNumber(QueueType queueType) {

        LocalDate today = LocalDate.now();

        QueueCounter counter =
                queueCounterRepo.findByQueueTypeAndQueueDate(queueType, today)
                        .orElseGet(() -> {
                            try {
                                queueCounterCreator.createCounterRow(queueType, today);
                            } catch (DataIntegrityViolationException ex) {
                                log.warn("Counter already created by another transaction");
                            }

                            return queueCounterRepo
                                    .findByQueueTypeAndQueueDate(queueType, today)
                                    .orElseThrow(() ->
                                            new IllegalStateException(
                                                    "Unable to find queue counter or create a new one!"
                                            )
                                    );
                        });

        int ticketNumber = counter.getNextTicketNumber();

        counter.setNextTicketNumber(ticketNumber + 1);
        queueCounterRepo.save(counter);

        return ticketNumber;
    }
}

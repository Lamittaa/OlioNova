package com.project.queue_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "QUEUE_COUNTER",
        uniqueConstraints = @UniqueConstraint(name = "uq_daily_ticket_counter",
                columnNames = {"queue_type", "queue_date"}
        )
)
@Getter
@Setter
public class QueueCounter {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String queueType;
    @Column(nullable = false)
    private LocalDate queueDate;
    @Column(nullable = false)
    private Integer nextTicketNumber;

}
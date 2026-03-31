package com.project.queue_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "queue_ticket", uniqueConstraints = {
        @UniqueConstraint(name = "uq_daily_ticket_type", columnNames = { "queue_type", "queue_date", "ticket_number" }),
        @UniqueConstraint(name = "uq_serving_teller", columnNames = { "queue_type", "teller_id", "ticket_number" })

})
@Getter
@Setter
public class QueueTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name = "order_id")
    private Long orderId; 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueType queueType;
    @Column(nullable = false)
    private LocalDate queueDate;
    @Column(nullable = false)
    private Integer ticketNumber;
    private TicketStatus ticketStatus;
    @Column(name = "missed_at")
    private LocalDateTime missedAt;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;

}
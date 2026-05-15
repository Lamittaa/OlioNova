package com.project.queue_service.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "QUEUE_COUNTER", uniqueConstraints = @UniqueConstraint(name = "uq_daily_ticket_counter", columnNames = {
                "queue_type", "queue_date" }))
public class QueueCounter {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private QueueType queueType;
        @Column(nullable = false)
        private LocalDate queueDate;
        @Column(nullable = false)
        private Integer nextTicketNumber;

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public QueueType getQueueType() {
                return queueType;
        }

        public void setQueueType(QueueType queueType) {
                this.queueType = queueType;
        }

        public LocalDate getQueueDate() {
                return queueDate;
        }

        public void setQueueDate(LocalDate queueDate) {
                this.queueDate = queueDate;
        }

        public Integer getNextTicketNumber() {
                return nextTicketNumber;
        }

        public void setNextTicketNumber(Integer nextTicketNumber) {
                this.nextTicketNumber = nextTicketNumber;
        }
}

package com.project.queue_service.dto;

import lombok.Data;

@Data
public class QueueTicketResponse {

    private Long orderId;
    private Integer ticketNumber;
    private String ticketStatus;
}
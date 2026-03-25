package com.project.productionStages.dto;

import lombok.Data;

@Data
public class QueueTicketResponse {

    private Long orderId;
    private Integer ticketNumber;
    private String ticketStatus;
}

package com.project.queue_service.mapper;

import com.project.queue_service.dto.QueueTicketResponse;
import com.project.queue_service.model.QueueTicket;

public class QueueTicketMapper{

public static QueueTicketResponse mapToResponse(QueueTicket t) {

    QueueTicketResponse dto = new QueueTicketResponse();

    dto.setOrderId(t.getOrderId());
    dto.setTicketNumber(t.getTicketNumber());
    dto.setTicketStatus(t.getTicketStatus().name());

    return dto;
}

}
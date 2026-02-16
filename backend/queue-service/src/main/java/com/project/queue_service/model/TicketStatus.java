package com.project.queue_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum TicketStatus {
    WAITING(1), SERVING(2), COMPLETED(3), CANCELLED(4);
    private final Integer id;

    public static TicketStatus fromId(Integer ticketStatusId){
        return Arrays.stream(TicketStatus.values()).filter(t->t.id.equals(ticketStatusId))
                .findAny().orElseThrow(()->new IllegalArgumentException("No Ticket Status found with id: "+ticketStatusId));
    }

}
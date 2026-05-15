package com.project.queue_service.model;

import java.util.Arrays;

public enum TicketStatus {
    WAITING(1), SERVING(2), COMPLETED(3), CANCELLED(4), MISSED(5);

    private final Integer id;

    TicketStatus(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public static TicketStatus fromId(Integer ticketStatusId){
        return Arrays.stream(TicketStatus.values()).filter(t->t.id.equals(ticketStatusId))
                .findAny().orElseThrow(()->new IllegalArgumentException("No Ticket Status found with id: "+ticketStatusId));
    }

}

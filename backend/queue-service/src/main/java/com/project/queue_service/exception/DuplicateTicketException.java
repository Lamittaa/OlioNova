
package com.project.queue_service.exception;

public class DuplicateTicketException extends RuntimeException {

    public DuplicateTicketException(String message) {
        super(message);
    }
}
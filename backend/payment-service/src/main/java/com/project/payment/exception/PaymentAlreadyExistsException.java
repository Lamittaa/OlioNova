package com.project.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409
public class PaymentAlreadyExistsException extends RuntimeException {

    public PaymentAlreadyExistsException(String message) {
        super(message);
    }
}


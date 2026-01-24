package com.project.order.exception;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Long customerId) {
        super("Customer with id " + customerId + " not found");
    }
}

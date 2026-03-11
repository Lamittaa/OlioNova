package com.project.productionStages.exception;

public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message){
        super(message);
    }
}
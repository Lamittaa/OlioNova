package com.project.queue_service.dto;

import java.time.Instant;
import java.util.List;

public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String code;
    private List<FieldErrorDto> errors;

    public ErrorResponse() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<FieldErrorDto> getErrors() {
        return errors;
    }

    public void setErrors(List<FieldErrorDto> errors) {
        this.errors = errors;
    }

    public static class Builder {
        private final ErrorResponse response = new ErrorResponse();

        public Builder timestamp(Instant timestamp) {
            response.setTimestamp(timestamp);
            return this;
        }

        public Builder status(int status) {
            response.setStatus(status);
            return this;
        }

        public Builder error(String error) {
            response.setError(error);
            return this;
        }

        public Builder message(String message) {
            response.setMessage(message);
            return this;
        }

        public Builder path(String path) {
            response.setPath(path);
            return this;
        }

        public Builder code(String code) {
            response.setCode(code);
            return this;
        }

        public Builder errors(List<FieldErrorDto> errors) {
            response.setErrors(errors);
            return this;
        }

        public ErrorResponse build() {
            return response;
        }
    }
}

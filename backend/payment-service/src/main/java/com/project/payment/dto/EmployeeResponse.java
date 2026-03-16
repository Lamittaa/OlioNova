package com.project.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeResponse {

    private Long   id;
    private String username;
    private String firstName;
    private String lastName;

    public String getFullName() {
        if (firstName == null && lastName == null) return "Unknown";
        if (firstName == null) return lastName;
        if (lastName  == null) return firstName;
        return firstName + " " + lastName;
    }
}
package com.project.payment.client;

import com.project.payment.config.FeignAuthForwardConfig;
import com.project.payment.dto.EmployeeResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "AUTH-SERVICE",                      
    configuration = FeignAuthForwardConfig.class
)
public interface EmployeeClient {

    @GetMapping("/api/employees/internal/{id}")  
    EmployeeResponse getEmployee(@PathVariable Long id);
}
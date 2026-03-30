package com.project.payment.client;

import com.project.payment.dto.DecreaseAvailabilityRequest;
import com.project.payment.dto.ProductResponse;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "order-service")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponse getProduct(@PathVariable Long id);

@PutMapping("/api/products/{id}/inventory/availability")void decreaseAvailability(
        @PathVariable Long id,
        @RequestBody DecreaseAvailabilityRequest body);
}
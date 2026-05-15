package com.ops.tracking_service.client;

import com.ops.tracking_service.config.FeignAuthForwardConfig;
import com.ops.tracking_service.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "CUSTOMER-SERVICE", configuration = FeignAuthForwardConfig.class)
public interface CustomerClient {

    @GetMapping("/api/customers/{customerId}")
    CustomerResponse getCustomerById(@PathVariable Long customerId);
}

package com.project.order.client;

import com.project.order.config.FeignAuthForwardConfig;
import com.project.order.dto.CustomerByNationalIdResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "CUSTOMER-SERVICE", configuration = FeignAuthForwardConfig.class)
public interface CustomerClient {

    @GetMapping("/api/customers/{id}/membership")
    MembershipResponse getMembership(@PathVariable Long id);

    record MembershipResponse(boolean isMembership) {}
    

    @GetMapping("/api/customers/by-national-id")
    CustomerByNationalIdResponse getByNationalId(
            @RequestParam String nationalId
    );
}

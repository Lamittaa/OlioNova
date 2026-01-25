package com.project.payment.client;

import com.project.payment.config.FeignAuthForwardConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(
    name = "ORDER-SERVICE",
    configuration = FeignAuthForwardConfig.class
)
public interface OrderClient {

    // 1️⃣ التأكد إن الطلب موجود
    @GetMapping("/api/orders/{id}")
    void getOrderById(@PathVariable Long id);

    // 2️⃣ تحديث حالة الطلب إلى PAID
    @PutMapping("/api/orders/{id}/pay")
    void markOrderAsPaid(@PathVariable Long id);
}

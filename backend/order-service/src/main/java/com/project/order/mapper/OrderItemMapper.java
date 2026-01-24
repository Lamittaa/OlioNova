package com.project.order.mapper;

import com.project.order.dto.*;
import com.project.order.model.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {

    public OrderItemResponse toResponse(OrderItem item) {

        if ("SERVICE".equalsIgnoreCase(item.getProductType())
                || "OLIVE".equalsIgnoreCase(item.getProductType())) {

            OliveServiceItemResponse dto = new OliveServiceItemResponse();
            mapBase(item, dto);

            dto.setOliveType(item.getOliveType());
            dto.setBagsCount(item.getBagsCount());
            dto.setNote(item.getNote());

            return dto;
        }

        // PURCHASE
        PurchaseItemResponse dto = new PurchaseItemResponse();
        mapBase(item, dto);

        return dto;
    }

    private void mapBase(OrderItem item, OrderItemResponse dto) {
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setProductType(item.getProductType());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setTotalPrice(item.getTotalPrice());
    }
}

package com.project.order.mapper;

import com.project.order.dto.OliveServiceItemResponse;
import com.project.order.dto.OrderItemResponse;
import com.project.order.dto.OrderResponse;
import com.project.order.dto.PurchaseItemResponse;
import com.project.order.model.Order;
import com.project.order.model.OrderItem;
import com.project.order.model.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setStatus(order.getStatus() != null ? order.getStatus().getStatusName() : null);
        if (order.getCreatedAt() != null) {
            response.setCreatedAt(order.getCreatedAt());
        }
        if (order.getUpdatedAt() != null) {
            response.setUpdatedAt(order.getUpdatedAt());
        }
        return response;
    }

    public OrderItemResponse mapItem(OrderItem item) {
        if (item == null) {
            return null;
        }

        if (isServiceLike(item)) {
            OliveServiceItemResponse dto = new OliveServiceItemResponse();
            fillCommon(item, dto);
            dto.setOliveType(item.getOliveType());
            dto.setBagsCount(item.getBagsCount());
            dto.setNote(item.getNote());
            return dto;
        }

        PurchaseItemResponse dto = new PurchaseItemResponse();
        fillCommon(item, dto);
        return dto;
    }

    private void fillCommon(OrderItem item, OrderItemResponse dto) {
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setProductType(item.getProductType());
        dto.setQuantity(item.getQuantity());
        dto.setStatus(item.getStatus() != null ? item.getStatus().getStatusName() : null);
    }

    public String map(OrderStatus status) {
        return status == null ? null : status.getStatusName();
    }

    public boolean isServiceLike(OrderItem item) {
        if (item == null) {
            return false;
        }

        if (item.getProductType() != null && "SERVICE".equalsIgnoreCase(item.getProductType())) {
            return true;
        }

        return item.getOliveType() != null || item.getBagsCount() != null || item.getNote() != null;
    }
}



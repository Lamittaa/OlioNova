
package com.project.order.mapper;

import com.project.order.dto.CreateOrderStatusResponse;
import com.project.order.dto.OrderStatusResponse;
import com.project.order.model.OrderStatus;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusMapper {

    public CreateOrderStatusResponse toCreateOrderStatusResponse(OrderStatus entity) {
        if (entity == null) {
            return null;
        }

        CreateOrderStatusResponse response = new CreateOrderStatusResponse();
        response.setId(entity.getId());
        response.setStatusName(entity.getStatusName());
        return response;
    }

    public OrderStatusResponse toOrderStatusResponse(OrderStatus entity) {
        if (entity == null) {
            return null;
        }

        OrderStatusResponse response = new OrderStatusResponse();
        response.setId(entity.getId());
        response.setStatusName(entity.getStatusName());
        return response;
    }

    public void updateOrderStatusFromDto(String statusName, @MappingTarget OrderStatus entity) {
        if (statusName != null) {
            entity.setStatusName(statusName.trim());
        }
    }
}

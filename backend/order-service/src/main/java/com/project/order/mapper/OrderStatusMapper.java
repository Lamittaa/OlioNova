
package com.project.order.mapper;

import com.project.order.dto.CreateOrderStatusResponse;
import com.project.order.dto.OrderStatusResponse;
import com.project.order.model.OrderStatus;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OrderStatusMapper {

    CreateOrderStatusResponse toCreateOrderStatusResponse(OrderStatus entity);

    OrderStatusResponse toOrderStatusResponse(OrderStatus entity);

    default void updateOrderStatusFromDto(String statusName, @MappingTarget OrderStatus entity) {
        if (statusName != null) {
            entity.setStatusName(statusName.trim());
        }
    }
}

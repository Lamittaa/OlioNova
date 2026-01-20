package com.project.order.mapper;

import com.project.order.dto.CreateOrderItemRequest;
import com.project.order.dto.OrderItemResponse;
import com.project.order.dto.OrderResponse;
import com.project.order.dto.UpdateOrderItemRequest;
import com.project.order.model.Order;
import com.project.order.model.OrderItem;
import com.project.order.model.OrderStatus;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // Entity -> DTO
    OrderResponse toOrderResponse(Order order);

    OrderItemResponse toOrderItemResponse(OrderItem item);

    // DTO -> Entity
    OrderItem toOrderItemEntity(CreateOrderItemRequest dto);

    // Update item
    default void updateOrderItemFromDto(UpdateOrderItemRequest dto, @MappingTarget OrderItem entity) {
        if (dto.getQuantity() != null) entity.setQuantity(dto.getQuantity());
        if (dto.getOliveType() != null) entity.setOliveType(dto.getOliveType());
        if (dto.getBagsCount() != null) entity.setBagsCount(dto.getBagsCount());
        if (dto.getNote() != null) entity.setNote(dto.getNote());
    }

    // ✅ هذا اللي بيحل المشكلة: OrderStatus -> String
    default String map(OrderStatus status) {
        if (status == null) return null;
        return status.getStatusName(); // إذا عندك اسم مختلف عدليه
    }
}

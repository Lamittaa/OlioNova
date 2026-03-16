package com.project.order.mapper;

import com.project.order.dto.OliveServiceItemResponse;
import com.project.order.dto.OrderItemResponse;
import com.project.order.dto.OrderResponse;
import com.project.order.dto.PurchaseItemResponse;
import com.project.order.model.Order;
import com.project.order.model.OrderItem;
import com.project.order.model.OrderStatus;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    default OrderItemResponse mapItem(OrderItem item) {

        if (item.getProductType() == null) {
            throw new IllegalStateException(
                    "OrderItem productType is NULL for itemId=" + item.getId());
        }

        if ("SERVICE".equalsIgnoreCase(item.getProductType())) {

            OliveServiceItemResponse dto = new OliveServiceItemResponse();
            fillCommon(item, dto);

            dto.setOliveType(item.getOliveType());
            dto.setBagsCount(item.getBagsCount());
            dto.setNote(item.getNote());

            return dto;
        }

        if ("PURCHASE".equalsIgnoreCase(item.getProductType())) {

            PurchaseItemResponse dto = new PurchaseItemResponse();
            fillCommon(item, dto);
            return dto;
        }

        throw new IllegalStateException(
                "Unknown productType: " + item.getProductType());
    }

    default void fillCommon(OrderItem item, OrderItemResponse dto) {
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setProductType(item.getProductType());
        dto.setQuantity(item.getQuantity());
        dto.setStatus(
                item.getStatus() != null
                        ? item.getStatus().getStatusName()
                        : null);
    }

    default String map(OrderStatus status) {
        return status == null ? null : status.getStatusName();
    }
}

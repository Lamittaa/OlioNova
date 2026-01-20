package com.project.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.project.order.dto.CreateProductRequest;
import com.project.order.dto.ProductResponse;
import com.project.order.dto.UpdateProductRequest;
import com.project.order.model.ProductLookup;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // Create
    ProductLookup toEntity(CreateProductRequest dto);

    // Response
    ProductResponse toProductResponse(ProductLookup entity);


    // Update (partial manual)
    default void updateProductFromDto(UpdateProductRequest dto,
                                      @MappingTarget ProductLookup entity) {

        if (dto.getProductName() != null) {
            entity.setProductName(dto.getProductName());
        }

        if (dto.getProductType() != null) {
            entity.setProductType(dto.getProductType());
        }

        if (dto.getInventory() != null) {
            entity.setInventory(dto.getInventory());
        }

        if (dto.getPrice() != null) {
            entity.setPrice(dto.getPrice());
        }

        if (dto.getUnit() != null) {
            entity.setUnit(dto.getUnit());
        }
    }
}

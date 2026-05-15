package com.project.order.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.project.order.dto.CreateProductRequest;
import com.project.order.dto.ProductResponse;
import com.project.order.dto.UpdateProductRequest;
import com.project.order.model.ProductLookup;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inventoryAvailabilityQuantity", source = "inventoryTotalQuantity")
    @Mapping(target = "memberDiscount", ignore = true)
    @Mapping(target = "active", constant = "true")
    ProductLookup toEntity(CreateProductRequest dto);

    ProductResponse toProductResponse(ProductLookup entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inventoryAvailabilityQuantity", ignore = true)
    @Mapping(target = "memberDiscount", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateProductFromDto(UpdateProductRequest dto, @MappingTarget ProductLookup entity);
}

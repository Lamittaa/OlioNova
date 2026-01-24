package com.project.customer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.project.customer.dto.CreateCustomerRequest;
import com.project.customer.dto.CreateCustomerResponse;
import com.project.customer.dto.CustomerResponse;
import com.project.customer.dto.UpdateCustomerRequest;
import com.project.customer.model.Customer;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    Customer toEntity(CreateCustomerRequest dto);

    @Mapping(source = "city.id", target = "cityId")
    CreateCustomerResponse toCreateCustomerResponse(Customer entity);

    @Mapping(source = "city.id", target = "cityId")
    CustomerResponse toCustomerResponse(Customer entity);

    default void updateCustomerFromDto(UpdateCustomerRequest dto, @MappingTarget Customer entity)
    {
        if (dto.getFirstName() != null) {
            entity.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            entity.setLastName(dto.getLastName());
        }
        if (dto.getPhoneNumber() != null) {
            entity.setPhoneNumber(dto.getPhoneNumber());
        }
        
    }
}

    


package com.project.customer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.project.customer.dto.CreateCityRequest;
import com.project.customer.dto.CreateCityResponse;
import com.project.customer.dto.CityResponse;
import com.project.customer.dto.UpdateCityRequest;
import com.project.customer.model.CityLookup;

@Mapper(componentModel = "spring")
public interface CityMapper {

    CityLookup toEntity(CreateCityRequest dto);

    CreateCityResponse toCreateCityResponse(CityLookup entity);

    CityResponse toCityResponse(CityLookup entity);

    default void updateCityFromDto(UpdateCityRequest dto, @MappingTarget CityLookup entity) {
        if (dto.getCityName() != null) {
            entity.setCityName(dto.getCityName());
        }
    }
}

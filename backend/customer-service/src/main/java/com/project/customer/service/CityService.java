package com.project.customer.service;

import java.util.List;

import com.project.customer.exception.EntityInUseException;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.customer.dto.CityResponse;
import com.project.customer.dto.CreateCityRequest;
import com.project.customer.dto.CreateCityResponse;
import com.project.customer.dto.UpdateCityRequest;
import com.project.customer.exception.EntityAlreadyExistsException;
import com.project.customer.exception.ResourceNotFoundException;
import com.project.customer.mapper.CityMapper;
import com.project.customer.model.CityLookup;
import com.project.customer.repositry.CityRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CityService {

    private final CityRepo cityRepo;
    private final CityMapper cityMapper;

    public CreateCityResponse createCity(CreateCityRequest request) {

        String name = request.getCityName().trim();

        if (cityRepo.existsByCityNameIgnoreCase(name)) {
            throw new EntityAlreadyExistsException("City already exists with name: " + name);
        }

        CityLookup city = cityMapper.toEntity(request);
        city.setCityName(name);

        CityLookup saved = cityRepo.save(city);
        return cityMapper.toCreateCityResponse(saved);
    }

    @Transactional(readOnly = true)
    public CityResponse getCityById(Long id) {
        CityLookup city = cityRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + id));

        return cityMapper.toCityResponse(city);
    }

    @Transactional(readOnly = true)
    public List<CityResponse> getAllCities() {
        return cityRepo.findAll()
                .stream()
                .map(cityMapper::toCityResponse)
                .toList();
    }

    public CityResponse updateCity(Long id, UpdateCityRequest request) {
        CityLookup city = cityRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + id));

        cityMapper.updateCityFromDto(request, city);

        CityLookup saved = cityRepo.save(city);
        return cityMapper.toCityResponse(saved);
    }

    public void deleteCity(Long id) {
        CityLookup city = cityRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + id));
        try {
            cityRepo.delete(city);
            cityRepo.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new EntityInUseException("Cannot delete city because it is used by another entities.");
        }
    }

    public CityLookup findById(@NotNull(message = "City ID cannot be null") Long cityId) {
        CityLookup city = cityRepo.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + cityId));
        return city;
    }
}

package com.project.customer.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.customer.dto.CreateCustomerRequest;
import com.project.customer.dto.CreateCustomerResponse;
import com.project.customer.dto.CustomerResponse;
import com.project.customer.dto.UpdateCustomerRequest;
import com.project.customer.exception.EntityAlreadyExistsException;
import com.project.customer.exception.ResourceNotFoundException;
import com.project.customer.mapper.CustomerMapper;
import com.project.customer.model.CityLookup;
import com.project.customer.repositry.CityRepo;
import com.project.customer.repositry.CustomerRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepo customerRepo;
    private final CustomerMapper customerMapper;
    private final CityRepo cityRepo;

    public CreateCustomerResponse createCustomer(CreateCustomerRequest req) {
        if (customerRepo.existsByNationalId(req.getNationalId())) {
            throw new EntityAlreadyExistsException("Customer with National ID already exists");
        }

        CityLookup city = cityRepo.findById(req.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City with ID " + req.getCityId() + " does not exist"));

        var entity = customerMapper.toEntity(req);
        entity.setCity(city);

        var saved = customerRepo.save(entity);
        return customerMapper.toCreateCustomerResponse(saved);
    }

    public CustomerResponse getCustomerById(Long id) {
        var customer = customerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with ID " + id + " does not exist"));
        return customerMapper.toCustomerResponse(customer);
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepo.findAll().stream()
                .map(customerMapper::toCustomerResponse)
                .toList();
    }

    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest req) {
        var customer = customerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with ID " + id + " does not exist"));

        customerMapper.updateCustomerFromDto(req, customer);

        // إذا cityId عندك إجباري في DTO (@NotNull) خلّيها بدون if
        CityLookup city = cityRepo.findById(req.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City with ID " + req.getCityId() + " does not exist"));
        customer.setCity(city);

        var saved = customerRepo.save(customer);
        return customerMapper.toCustomerResponse(saved);
    }

    public void deleteCustomer(Long id) {
        if (!customerRepo.existsById(id)) {
            throw new ResourceNotFoundException("Customer with ID " + id + " does not exist");
        }
        customerRepo.deleteById(id);
    }

    // ✅ SEARCH
    public CustomerResponse searchByNationalId(String nationalId) {
        var customer = customerRepo.findByNationalId(nationalId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with National ID " + nationalId + " not found"));
        return customerMapper.toCustomerResponse(customer);
    }

    // ✅ ADMIN: Update National ID
public CustomerResponse updateNationalId(Long id, String newNationalId) {

    var customer = customerRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Customer with ID " + id + " does not exist"
            ));

    if (newNationalId.equals(customer.getNationalId())) {
        return customerMapper.toCustomerResponse(customer);
    }

    // منع التكرار
    if (customerRepo.existsByNationalId(newNationalId)) {
        throw new EntityAlreadyExistsException("Customer with National ID already exists");
    }

    customer.setNationalId(newNationalId);

    var saved = customerRepo.save(customer);
    return customerMapper.toCustomerResponse(saved);
}
@Transactional(readOnly = true)
public boolean getMembershipByCustomerId(Long id) {
    var customer = customerRepo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

    return Boolean.TRUE.equals(customer.getIsMember()); // أو customer.isMember حسب اسم الحقل عندك
}

}

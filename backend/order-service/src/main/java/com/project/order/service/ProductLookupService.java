package com.project.order.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.order.dto.CreateProductRequest;
import com.project.order.dto.InventoryResponse;
import com.project.order.dto.ProductResponse;
import com.project.order.dto.UpdateInventoryRequest;
import com.project.order.dto.UpdateProductRequest;
import com.project.order.exception.BusinessRuleViolationException;
import com.project.order.exception.EntityAlreadyExistsException;
import com.project.order.exception.ResourceNotFoundException;
import com.project.order.mapper.ProductMapper;
import com.project.order.model.ProductLookup;
import com.project.order.repository.ProductLookupRepo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductLookupService {

    private final ProductLookupRepo productRepo;
    private final ProductMapper productMapper;

    public ProductResponse createProduct(CreateProductRequest request) {
        String name = request.getProductName().trim();

        if (productRepo.existsByProductNameIgnoreCase(name)) {
            throw new EntityAlreadyExistsException(
                    "Product already exists with name: " + name + " (If it was deactivated, activate it instead)");
        }

        ProductLookup product = productMapper.toEntity(request);

        product.setProductName(name);
        product.setProductType("Purchase");
        product.setUnit(request.getUnit().trim().toUpperCase());
        product.setActive(true);

        ProductLookup saved = productRepo.save(product);
        return productMapper.toProductResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        ProductLookup product = productRepo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        return productMapper.toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepo.findByActiveTrueOrderByProductNameAsc()
                .stream()
                .map(productMapper::toProductResponse)
                .toList();
    }

    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        ProductLookup product = productRepo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        String newName = request.getProductName().trim();
        String currentName = product.getProductName() == null ? "" : product.getProductName().trim();

        if (!newName.equalsIgnoreCase(currentName) && productRepo.existsByProductNameIgnoreCase(newName)) {
            throw new EntityAlreadyExistsException("Product already exists with name: " + newName);
        }

        productMapper.updateProductFromDto(request, product);

        product.setProductName(newName);
        product.setProductType(request.getProductType().trim().toUpperCase());
        product.setUnit(request.getUnit().trim().toUpperCase());

        ProductLookup saved = productRepo.save(product);
        return productMapper.toProductResponse(saved);
    }

    public ProductResponse updateInventory(Long id, UpdateInventoryRequest request) {
    ProductLookup product = productRepo.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

    product.setInventoryTotalQuantity(request.getInventoryTotalQuantity());
  

    ProductLookup saved = productRepo.save(product);
    return productMapper.toProductResponse(saved);
}

    public void deleteProduct(Long id) {
        ProductLookup product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (Boolean.FALSE.equals(product.getActive())) {
            return;
        }

        product.setActive(false);
        productRepo.save(product);
    }

    public void activateProduct(Long id) {
        ProductLookup product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (Boolean.TRUE.equals(product.getActive())) {
            return;
        }

        product.setActive(true);
        productRepo.save(product);
    }
    public void decreaseAvailability(Long id, Integer quantity) {
    ProductLookup product = productRepo.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Product not found with id: " + id));

    int current = product.getInventoryAvailabilityQuantity() == null
            ? 0 : product.getInventoryAvailabilityQuantity();

    product.setInventoryAvailabilityQuantity(current - quantity);
    productRepo.save(product);
}

public InventoryResponse getInventory(Long id) {
    ProductLookup product = productRepo.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Product not found with id: " + id));

    InventoryResponse response = new InventoryResponse();
    response.setInventoryTotalQuantity(product.getInventoryTotalQuantity());
    response.setInventoryAvailabilityQuantity(product.getInventoryAvailabilityQuantity());

    return response;
}

public ProductResponse updateTotalInventory(Long id, UpdateInventoryRequest request) {
    ProductLookup product = productRepo.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Product not found with id: " + id));

    product.setInventoryTotalQuantity(request.getInventoryTotalQuantity());

    ProductLookup saved = productRepo.save(product);
    return productMapper.toProductResponse(saved);
}


// في ProductLookupService
public ProductResponse addInventory(Long id, UpdateInventoryRequest request) {
    ProductLookup product = productRepo.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Product not found with id: " + id));

    if (product.getInventoryTotalQuantity() != null) {
        throw new BusinessRuleViolationException(
                "Inventory already exists. Use PATCH to update.");
    }

    product.setInventoryTotalQuantity(request.getInventoryTotalQuantity());
    product.setInventoryAvailabilityQuantity(request.getInventoryTotalQuantity());

    ProductLookup saved = productRepo.save(product);
    return productMapper.toProductResponse(saved);
}
}

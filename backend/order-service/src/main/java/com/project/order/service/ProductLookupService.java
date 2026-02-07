package com.project.order.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.order.dto.CreateProductRequest;
import com.project.order.dto.ProductResponse;
import com.project.order.dto.UpdateInventoryRequest;
import com.project.order.dto.UpdateProductRequest;
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

    // ✅ POST /api/products
    public ProductResponse createProduct(CreateProductRequest request) {
        String name = request.getProductName().trim();

        // يمنع التكرار حتى لو المنتج inactive (بسبب unique)
        if (productRepo.existsByProductNameIgnoreCase(name)) {
            throw new EntityAlreadyExistsException(
                "Product already exists with name: " + name + " (If it was deactivated, activate it instead)"
            );
        }

        ProductLookup product = productMapper.toEntity(request);

        // توحيد القيم قبل التخزين
        product.setProductName(name);
        product.setProductType(request.getProductType().trim().toUpperCase());
        product.setUnit(request.getUnit().trim().toUpperCase());

        product.setActive(true);

        ProductLookup saved = productRepo.save(product);
        return productMapper.toProductResponse(saved);
    }

    // ✅ GET /api/products/{id} (active only)
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        ProductLookup product = productRepo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        return productMapper.toProductResponse(product);
    }

    // ✅ GET /api/products (active only)
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepo.findByActiveTrueOrderByProductNameAsc()
                .stream()
                .map(productMapper::toProductResponse)
                .toList();
    }

    // ✅ PUT /api/products/{id} (active only)
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        ProductLookup product = productRepo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        String newName = request.getProductName().trim();
        String currentName = product.getProductName() == null ? "" : product.getProductName().trim();

        // منع التكرار لو الاسم تغيّر
        if (!newName.equalsIgnoreCase(currentName) && productRepo.existsByProductNameIgnoreCase(newName)) {
            throw new EntityAlreadyExistsException("Product already exists with name: " + newName);
        }

        // تحديث الحقول
        productMapper.updateProductFromDto(request, product);

        // توحيد القيم قبل التخزين (حتى لو المستخدم كتب small)
        product.setProductName(newName);
        product.setProductType(request.getProductType().trim().toUpperCase());
        product.setUnit(request.getUnit().trim().toUpperCase());

        ProductLookup saved = productRepo.save(product);
        return productMapper.toProductResponse(saved);
    }

    // ✅ PATCH /api/products/{id}/inventory (active only)
    public ProductResponse updateInventory(Long id, UpdateInventoryRequest request) {
        ProductLookup product = productRepo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setInventory(request.getInventory());

        ProductLookup saved = productRepo.save(product);
        return productMapper.toProductResponse(saved);
    }

    // ✅ DELETE /api/products/{id} ==> Deactivate (soft delete)
    public void deleteProduct(Long id) {
        ProductLookup product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // idempotent
        if (Boolean.FALSE.equals(product.getActive())) {
            return;
        }

        product.setActive(false);
        productRepo.save(product);
    }



    // ✅ PATCH /api/products/{id}/activate
    public void activateProduct(Long id) {
        ProductLookup product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // idempotent
        if (Boolean.TRUE.equals(product.getActive())) {
            return;
        }

        product.setActive(true);
        productRepo.save(product);
    }
}

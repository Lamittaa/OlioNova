package com.project.order.controller;

import com.project.order.dto.CreateProductRequest;
import com.project.order.dto.DecreaseAvailabilityRequest;
import com.project.order.dto.ProductResponse;
import com.project.order.dto.UpdateInventoryRequest;
import com.project.order.dto.UpdateProductRequest;
import com.project.order.dto.ErrorResponse;
import com.project.order.dto.InventoryResponse;
import com.project.order.service.ProductLookupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Products",
        description = "Product lookup catalog used when creating orders (e.g., OLIVE press service, JIFT, GALLON)."
)
public class ProductLookupController {

    private final ProductLookupService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('PRODUCT_CREATE')")
    @Operation(summary = "Create a new product")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Product created successfully",
                    content = @Content(
                            schema = @Schema(implementation = ProductResponse.class),
                            examples = @ExampleObject(
                                    name = "Created product",
                                    value = """
                                    {
                                      "id": 12,
                                      "productName": "Olive Pressing",
                                      "productType": "JIFT",
                                      "inventory": 0,
                                      "price": 0.60,
                                      "unit": "KG"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Invalid productType/unit",
                                    value = """
                                    {
                                      "timestamp": "2026-01-16T01:00:00Z",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "Validation failed",
                                      "path": "/api/products",
                                      "code": "VALIDATION_ERROR",
                                      "errors": [
                                        { "field": "productType", "message": "Product type must be one of: OLIVE, JIFT, GALLON", "rejectedValue": "jeft" }
                                      ]
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Duplicate product name",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Duplicate name",
                                    value = """
                                    {
                                      "timestamp": "2026-01-16T01:00:00Z",
                                      "status": 409,
                                      "error": "Conflict",
                                      "message": "Product already exists with name: Olive Pressing (If it was deactivated, activate it instead)",
                                      "path": "/api/products",
                                      "code": "ENTITY_ALREADY_EXISTS"
                                    }
                                    """
                            )
                    )
            )
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Valid
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateProductRequest.class),
                            examples = @ExampleObject(
                                    name = "Create product request (case-insensitive)",
                                    value = """
                                    {
                                      "productName": "Olive Pressing",
                                      "productType": "jift",
                                      "inventory": 0,
                                      "price": 0.60,
                                      "unit": "kg"
                                    }
                                    """
                            )
                    )
            )
            CreateProductRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT') and hasAuthority('PRODUCT_READ')")
    @Operation(summary = "Get product by ID (active only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product returned successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found (missing or deactivated)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Not found",
                                    value = """
                                    {
                                      "timestamp": "2026-01-16T01:00:00Z",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "Product not found with id: 2",
                                      "path": "/api/products/2",
                                      "code": "RESOURCE_NOT_FOUND"
                                    }
                                    """
                            )
                    )
            )
    })
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable @Min(value = 1, message = "Product ID must be greater than 0") Long id
    ) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT') and hasAuthority('PRODUCT_READ')")
    @Operation(summary = "Get all products (active only)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Products returned successfully",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = ProductResponse.class)),
                            examples = @ExampleObject(
                                    name = "Active products list",
                                    value = """
                                    [
                                      {
                                        "id": 3,
                                        "productName": "Olive Press Service",
                                        "productType": "OLIVE",
                                        "inventory": null,
                                        "price": 25.00,
                                        "unit": "KG"
                                      },
                                      {
                                        "id": 5,
                                        "productName": "Jift Bag",
                                        "productType": "JIFT",
                                        "inventory": 120,
                                        "price": 1.20,
                                        "unit": "PCS"
                                      }
                                    ]
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Update a product (active only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found (missing or deactivated)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate product name",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable @Min(value = 1, message = "Product ID must be greater than 0") Long id,
            @Valid
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UpdateProductRequest.class),
                            examples = @ExampleObject(
                                    name = "Update product request",
                                    value = """
                                    {
                                      "productName": "Jift Bag",
                                      "productType": "JIFT",
                                      "inventory": 100,
                                      "price": 1.10,
                                      "unit": "pcs"
                                    }
                                    """
                            )
                    )
            )
            UpdateProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PatchMapping("/{id}/inventory")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT') and hasAuthority('PRODUCT_UPDATE_INVENTORY')")
    @Operation(summary = "Update product inventory (active only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found (missing or deactivated)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponse> updateInventory(
            @PathVariable @Min(value = 1, message = "Product ID must be greater than 0") Long id,
            @Valid
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UpdateInventoryRequest.class),
                            examples = @ExampleObject(
                                    name = "Update inventory request",
                                    value = """
                                    { "inventory": 150 }
                                    """
                            )
                    )
            )
            UpdateInventoryRequest request
    ) {
        return ResponseEntity.ok(productService.updateInventory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('PRODUCT_DELETE')")
    @Operation(summary = "Deactivate a product (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deactivated (or already inactive)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteProduct(
            @PathVariable @Min(value = 1, message = "Product ID must be greater than 0") Long id
    ) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Activate a product (undo soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product activated (or already active)"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> activateProduct(
            @PathVariable @Min(value = 1, message = "Product ID must be greater than 0") Long id
    ) {
        productService.activateProduct(id);
        return ResponseEntity.noContent().build();
    }

@PutMapping("/{id}/inventory/availability")
public ResponseEntity<Void> decreaseAvailability(
        @PathVariable Long id,
        @Valid @RequestBody DecreaseAvailabilityRequest request) {
    productService.decreaseAvailability(id, request.getQuantity());
    return ResponseEntity.noContent().build();
}
@GetMapping("/{id}/inventory")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT') and hasAuthority('PRODUCT_READ')")
@Operation(summary = "Get product inventory", description = "Returns total and available inventory for a product")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory returned successfully",
                content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
})
public ResponseEntity<InventoryResponse> getInventory(
        @PathVariable @Min(value = 1, message = "Product ID must be greater than 0") Long id) {
    return ResponseEntity.ok(productService.getInventory(id));
}


@PatchMapping("/{id}/inventory/total")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT') and hasAuthority('PRODUCT_UPDATE_INVENTORY')")
@Operation(summary = "Update total inventory only", description = "Updates inventoryTotalQuantity only without touching availability")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory updated successfully",
                content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
})
public ResponseEntity<ProductResponse> updateTotalInventory(
        @PathVariable @Min(value = 1, message = "Product ID must be greater than 0") Long id,
        @Valid @RequestBody UpdateInventoryRequest request) {
    return ResponseEntity.ok(productService.updateTotalInventory(id, request));
}

@PostMapping("/{id}/inventory")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
@Operation(summary = "Add inventory for the first time")
public ResponseEntity<ProductResponse> addInventory(
        @PathVariable @Min(value = 1) Long id,
        @Valid @RequestBody UpdateInventoryRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(productService.addInventory(id, request));
}



}

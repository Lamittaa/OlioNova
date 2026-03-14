package com.project.customer.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.project.customer.dto.CreateCustomerRequest;
import com.project.customer.dto.CreateCustomerResponse;
import com.project.customer.dto.CustomerResponse;
import com.project.customer.dto.UpdateCustomerNationalIdRequest;
import com.project.customer.dto.UpdateCustomerRequest;
import com.project.customer.service.CustomerService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customer Management", description = "Customer CRUD operations")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Create customer", description = "Creates a new customer using national ID and personal information.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Customer creation payload", required = true, content = @Content(examples = @ExampleObject(name = "CreateCustomerRequest", value = """
            {
              "nationalId": "987654321",
              "firstName": "Ahmad",
              "lastName": "Khalil",
              "phoneNumber": "0599123456",
              "cityId": 3
            }
            """))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing CUSTOMER_CREATE authority)"),
            @ApiResponse(responseCode = "409", description = "Customer with the same National ID already exists")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') and hasAuthority('CUSTOMER_CREATE')")
    public ResponseEntity<CreateCustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        log.info("Create customer nationalId={}", request.getNationalId());
        var res = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @Operation(summary = "Get customer by ID", description = "Retrieves a customer using database ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found", content = @Content(examples = @ExampleObject(value = """
                    {
                      "id": 1,
                      "nationalId": "987654321",
                      "firstName": "Ahmad",
                      "lastName": "Khalil",
                      "phoneNumber": "0599123456",
                      "cityName": "Bethlehem"
                    }
                    """))),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing CUSTOMER_READ authority)"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT') and hasAuthority('CUSTOMER_READ')")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        var res = customerService.getCustomerById(id);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Get all customers", description = "Returns all registered customers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customers retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing CUSTOMER_READ authority)")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT') and hasAuthority('CUSTOMER_READ')")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        var res = customerService.getAllCustomers();
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Update customer", description = "Updates customer personal information.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated customer information", required = true, content = @Content(examples = @ExampleObject(name = "UpdateCustomerRequest", value = """
            {
              "firstName": "Ahmad",
              "lastName": "Hassan",
              "phoneNumber": "0599887766",
              "cityId": 2
            }
            """))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing CUSTOMER_UPDATE authority)"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') and hasAuthority('CUSTOMER_UPDATE')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        log.info("Update customer id={}", id);
        var res = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Delete customer", description = "Deletes a customer permanently by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Customer deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Admin only / missing CUSTOMER_DELETE authority)"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('CUSTOMER_DELETE')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        log.info("Delete customer id={}", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search customer by National ID", description = "Searches for a customer using their national ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "400", description = "National ID is empty or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing CUSTOMER_SEARCH authority)"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/search/{national_id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT') and hasAuthority('CUSTOMER_SEARCH')")
    public ResponseEntity<CustomerResponse> searchCustomerByNationalId(
            @Parameter(description = "National ID must be exactly 9 digits (numeric only)", example = "987654321", schema = @Schema(pattern = "^[0-9]{9}$", minLength = 9, maxLength = 9)) @PathVariable("national_id") @NotBlank(message = "National ID must not be empty") @Pattern(regexp = "^[0-9]{9}$", message = "National ID must be exactly 9 digits") String nationalId) {
        log.info("Search customer by nationalId={}", nationalId);
        var res = customerService.searchByNationalId(nationalId);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Admin: Update customer National ID", description = "Allows ADMIN to update customer's National ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "National ID updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin only"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "409", description = "National ID already exists")
    })
    @PatchMapping("/{id}/national-id")
    @PreAuthorize("hasRole('ADMIN', 'RECEPTIONIST') and hasAuthority('CUSTOMER_UPDATE_NATIONAL_ID')")
    public ResponseEntity<CustomerResponse> updateNationalId(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerNationalIdRequest request) {
        var res = customerService.updateNationalId(id, request.getNationalId());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}/membership")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT') and hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Get customer membership", description = "Returns whether the customer is a member (true/false). Used by Order Service for pricing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membership status returned", content = @Content(examples = @ExampleObject(value = """
                    { "isMembership": true }
                    """))),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing CUSTOMER_READ authority)"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<MembershipResponse> getMembership(@PathVariable Long id) {
        boolean isMember = customerService.getMembershipByCustomerId(id);
        return ResponseEntity.ok(new MembershipResponse(isMember));
    }

    @GetMapping("/by-national-id")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public CustomerResponse getByNationalId(
            @RequestParam String nationalId) {
        log.info("Get customer by nationalId={} (internal)", nationalId);
        return customerService.searchByNationalId(nationalId);
    }

    public record MembershipResponse(boolean isMembership) {
    }

}

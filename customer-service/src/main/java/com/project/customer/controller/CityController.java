package com.project.customer.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.project.customer.dto.CityResponse;
import com.project.customer.dto.CreateCityRequest;
import com.project.customer.dto.CreateCityResponse;
import com.project.customer.dto.UpdateCityRequest;
import com.project.customer.service.CityService;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name = "Cities",
    description = "Used to link customers to cities."
)
public class CityController {

    private final CityService cityService;

    // ================= CREATE =================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') and hasAuthority('CITY_CREATE')")
    @Operation(
        summary = "Create a new city",
        description = "Creates a new city in the city lookup table.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "City creation payload",
            required = true,
            content = @Content(
                examples = @ExampleObject(
                    name = "CreateCityRequest",
                    value = """
                    {
                      "cityName": "Bethlehem"
                    }
                    """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "City created successfully",
            content = @Content(
                schema = @Schema(implementation = CreateCityResponse.class),
                examples = @ExampleObject(
                    name = "CreatedCity",
                    value = """
                    {
                      "id": 1,
                      "cityName": "Bethlehem"
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Validation error (e.g., empty city name)"),
        @ApiResponse(responseCode = "403", description = "Forbidden (missing CITY_CREATE authority)"),
        @ApiResponse(responseCode = "409", description = "City already exists (duplicate city name)")
    })
    public ResponseEntity<CreateCityResponse> createCity(
            @Valid @RequestBody CreateCityRequest request
    ) {
        return new ResponseEntity<>(cityService.createCity(request), HttpStatus.CREATED);
    }

    // ================= GET BY ID =================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT') and hasAuthority('CITY_READ')")
    @Operation(
        summary = "Get city by ID",
        description = "Returns a single city by its ID."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "City found",
            content = @Content(
                schema = @Schema(implementation = CityResponse.class),
                examples = @ExampleObject(
                    name = "CityResponse",
                    value = """
                    {
                      "id": 1,
                      "cityName": "Bethlehem"
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid ID (must be positive)"),
        @ApiResponse(responseCode = "403", description = "Forbidden (missing CITY_READ authority)"),
        @ApiResponse(responseCode = "404", description = "City not found")
    })
    public ResponseEntity<CityResponse> getCityById(
            @Parameter(
                description = "City ID (must be positive)",
                example = "1",
                schema = @Schema(minimum = "1")
            )
            @PathVariable
            @Min(value = 1, message = "City ID must be greater than 0")
            Long id
    ) {
        return ResponseEntity.ok(cityService.getCityById(id));
    }

    // ================= GET ALL =================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT') and hasAuthority('CITY_READ')")
    @Operation(
        summary = "Get all cities",
        description = "Returns a list of all cities in the lookup table."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Cities returned successfully",
            content = @Content(
                schema = @Schema(implementation = CityResponse.class),
                examples = @ExampleObject(
                    name = "CityListExample",
                    value = """
                    [
                      {"id": 1, "cityName": "Bethlehem"},
                      {"id": 2, "cityName": "Beit Jala"}
                    ]
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "403", description = "Forbidden (missing CITY_READ authority)")
    })
    public ResponseEntity<List<CityResponse>> getAllCities() {
        return ResponseEntity.ok(cityService.getAllCities());
    }

    // ================= UPDATE =================

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') and hasAuthority('CITY_UPDATE')")
    @Operation(
        summary = "Update a city",
        description = "Updates the city name for the given city ID.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "City update payload",
            required = true,
            content = @Content(
                examples = @ExampleObject(
                    name = "UpdateCityRequest",
                    value = """
                    {
                      "cityName": "Beit Sahour"
                    }
                    """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "City updated successfully",
            content = @Content(
                schema = @Schema(implementation = CityResponse.class),
                examples = @ExampleObject(
                    name = "UpdatedCity",
                    value = """
                    {
                      "id": 1,
                      "cityName": "Beit Sahour"
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Validation error (e.g., empty city name or invalid id)"),
        @ApiResponse(responseCode = "403", description = "Forbidden (missing CITY_UPDATE authority)"),
        @ApiResponse(responseCode = "404", description = "City not found"),
        @ApiResponse(responseCode = "409", description = "City name already exists (duplicate)")
    })
    public ResponseEntity<CityResponse> updateCity(
            @Parameter(
                description = "City ID (must be positive)",
                example = "1",
                schema = @Schema(minimum = "1")
            )
            @PathVariable
            @Min(value = 1, message = "City ID must be greater than 0")
            Long id,

            @Valid @RequestBody UpdateCityRequest request
    ) {
        return ResponseEntity.ok(cityService.updateCity(id, request));
    }

    // ================= DELETE =================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('CITY_DELETE')")
    @Operation(
        summary = "Delete a city",
        description = "Deletes a city by ID. May fail if the city is referenced by customers."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "City deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid ID (must be positive)"),
        @ApiResponse(responseCode = "403", description = "Forbidden (Admin only / missing CITY_DELETE authority)"),
        @ApiResponse(responseCode = "404", description = "City not found"),
        @ApiResponse(responseCode = "409", description = "Conflict (city is used by customers / cannot delete)")
    })
    public ResponseEntity<Void> deleteCity(
            @Parameter(
                description = "City ID (must be positive)",
                example = "1",
                schema = @Schema(minimum = "1")
            )
            @PathVariable
            @Min(value = 1, message = "City ID must be greater than 0")
            Long id
    ) {
        cityService.deleteCity(id);
        return ResponseEntity.noContent().build();
    }
}

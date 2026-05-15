package com.zaytoun.aiprediction.controller;

import com.zaytoun.aiprediction.dto.DatasetImageResponse;
import com.zaytoun.aiprediction.dto.UpdateYieldRequest;
import com.zaytoun.aiprediction.service.DatasetService;
import com.zaytoun.aiprediction.validation.BatchIdRules;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dataset")
@RequiredArgsConstructor
@Validated
public class DatasetController {
    private final DatasetService datasetService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DatasetImageResponse storeImage(@RequestPart("file") MultipartFile file,
                                           @RequestParam
                                           @NotBlank
                                           @Size(max = 64, message = BatchIdRules.MESSAGE)
                                           @Pattern(regexp = BatchIdRules.REGEX, message = BatchIdRules.MESSAGE)
                                           String batchId,
                                           @RequestParam(required = false) String cultivar,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate harvestDate,
                                           @RequestParam(required = false) Double actualYieldPercent) {
        return datasetService.storeDatasetImage(file, batchId, cultivar, harvestDate, actualYieldPercent);
    }

    @PutMapping("/{batchId}/yield")
    public DatasetImageResponse updateYield(@PathVariable
                                            @NotBlank
                                            @Size(max = 64, message = BatchIdRules.MESSAGE)
                                            @Pattern(regexp = BatchIdRules.REGEX, message = BatchIdRules.MESSAGE)
                                            String batchId,
                                            @Valid @RequestBody UpdateYieldRequest request) {
        return datasetService.updateBatchYield(batchId, request.getActualYieldPercent());
    }
}

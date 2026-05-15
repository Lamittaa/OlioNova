package com.zaytoun.aiprediction.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class TrainingRequest {
    @Pattern(regexp = "linear|xgboost|bpnn", message = "modelType must be one of: linear, xgboost, bpnn")
    private String modelType = "linear";
    private boolean forceActivate;
    private Map<String, Object> hyperparameters = new HashMap<>();
}

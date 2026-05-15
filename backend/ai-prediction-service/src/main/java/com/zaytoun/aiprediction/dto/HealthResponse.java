package com.zaytoun.aiprediction.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HealthResponse {
    String status;
    String activeModelVersion;
    long trainingSampleCount;
    long storedImageCount;
}

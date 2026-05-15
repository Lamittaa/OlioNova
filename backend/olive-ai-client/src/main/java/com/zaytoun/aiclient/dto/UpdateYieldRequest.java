package com.zaytoun.aiclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateYieldRequest {
    private Double actualYieldPercent;
}

package com.project.productionStages.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerResponse {

    private String name; 

    private List<ItemResponse> items;
}
package com.project.productionStages.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ContainerViewResponse {
    private String containerName;
    private String status;
}
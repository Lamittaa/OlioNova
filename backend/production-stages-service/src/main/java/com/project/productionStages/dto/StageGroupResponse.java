package com.project.productionStages.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StageGroupResponse {
    private String name;
    private String stageType;
    private Integer stageOrder;
    private List<ContainerViewResponse> containers;
}
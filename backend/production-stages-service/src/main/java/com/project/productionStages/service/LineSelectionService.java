package com.project.productionStages.service;

import com.project.productionStages.model.ProductionStage;
import com.project.productionStages.model.StageStatus;
import com.project.productionStages.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LineSelectionService {

    private final ProductionStageRepository stageRepository;

    public String chooseBestLine(){

        String[] lines = {"A","B"};

        for(String line : lines){

            Optional<ProductionStage> stage =
                    stageRepository.findByLineAndCurrentStatus(
                            line,
                            StageStatus.IN_PROGRESS
                    );

            if(stage.isEmpty()){
                return line;
            }
        }

        return "A";
    }
}
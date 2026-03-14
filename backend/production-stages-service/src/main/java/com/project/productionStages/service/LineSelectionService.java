package com.project.productionStages.service;

import com.project.productionStages.logic.LineChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LineSelectionService {

    private final LineChooser lineChooser; 

   
    public String chooseBestLine() {
        return lineChooser.chooseBestLine();
    }
}
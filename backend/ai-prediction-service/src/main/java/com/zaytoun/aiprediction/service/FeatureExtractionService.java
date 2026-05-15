package com.zaytoun.aiprediction.service;

import com.zaytoun.aiprediction.ml.FeatureVector;
import com.zaytoun.aiprediction.util.OliveImageProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class FeatureExtractionService {
    private final OliveImageProcessor oliveImageProcessor;

    public FeatureVector extract(Path imagePath) {
        return oliveImageProcessor.extract(imagePath);
    }
}

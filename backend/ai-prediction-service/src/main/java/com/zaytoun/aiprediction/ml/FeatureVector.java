package com.zaytoun.aiprediction.ml;

import lombok.Builder;
import lombok.Value;

import java.util.LinkedHashMap;
import java.util.Map;

@Value
@Builder
public class FeatureVector {
    double rMean;
    double gMean;
    double bMean;
    Map<String, Double> features;
    boolean segmentationSuccess;

    public Map<String, Double> toPersistedMap() {
        LinkedHashMap<String, Double> values = new LinkedHashMap<>();
        values.put("R", rMean);
        values.put("G", gMean);
        values.put("B", bMean);
        values.putAll(features);
        return values;
    }

    public double[] toArray() {
        return toPersistedMap().values().stream().mapToDouble(Double::doubleValue).toArray();
    }
}

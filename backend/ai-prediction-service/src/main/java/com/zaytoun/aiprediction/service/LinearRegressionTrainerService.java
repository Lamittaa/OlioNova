package com.zaytoun.aiprediction.service;

import com.zaytoun.aiprediction.ml.FeatureVector;
import com.zaytoun.aiprediction.ml.ModelArtifact;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.stat.StatUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LinearRegressionTrainerService {

    public ModelArtifact train(List<FeatureVector> featureVectors, List<Double> labels, String modelType, String version) {
        if (featureVectors.size() < 8) {
            throw new IllegalStateException("At least 8 labeled samples are required before training");
        }

        List<String> featureNames = new ArrayList<>(featureVectors.get(0).toPersistedMap().keySet());
        int n = featureVectors.size();
        int featureCount = featureNames.size();
        double[][] x = new double[n][featureCount];
        double[] y = labels.stream().mapToDouble(Double::doubleValue).toArray();

        Map<String, Double> means = new LinkedHashMap<>();
        Map<String, Double> stdDevs = new LinkedHashMap<>();

        for (int j = 0; j < featureCount; j++) {
            double[] column = new double[n];
            for (int i = 0; i < n; i++) {
                double value = featureVectors.get(i).toPersistedMap().get(featureNames.get(j));
                x[i][j] = value;
                column[i] = value;
            }
            double mean = StatUtils.mean(column);
            double variance = StatUtils.variance(column, mean);
            double std = Math.sqrt(Math.max(variance, 1e-12));
            means.put(featureNames.get(j), mean);
            stdDevs.put(featureNames.get(j), std);
            for (int i = 0; i < n; i++) {
                x[i][j] = (x[i][j] - mean) / std;
            }
        }

        int splitIndex = Math.max((int) Math.floor(n * 0.8), 1);
        double[][] xTrain = Arrays.copyOfRange(x, 0, splitIndex);
        double[] yTrain = Arrays.copyOfRange(y, 0, splitIndex);
        double[][] xTest = Arrays.copyOfRange(x, splitIndex, n);
        double[] yTest = Arrays.copyOfRange(y, splitIndex, n);
        if (xTest.length == 0) {
            xTest = xTrain;
            yTest = yTrain;
        }

        double lambda = 1.0;
        RealVector yVector = new ArrayRealVector(yTrain);
        RealMatrix design = MatrixUtils.createRealMatrix(xTrain.length, featureCount + 1);
        for (int i = 0; i < xTrain.length; i++) {
            design.setEntry(i, 0, 1.0);
            for (int j = 0; j < featureCount; j++) {
                design.setEntry(i, j + 1, xTrain[i][j]);
            }
        }
        RealMatrix xtx = design.transpose().multiply(design);
        RealMatrix ridgePenalty = MatrixUtils.createRealIdentityMatrix(featureCount + 1).scalarMultiply(lambda);
        ridgePenalty.setEntry(0, 0, 0.0);
        RealVector betaVector = new SingularValueDecomposition(xtx.add(ridgePenalty)).getSolver()
                .solve(design.transpose().operate(yVector));

        double intercept = betaVector.getEntry(0);
        double[] weights = new double[featureCount];
        for (int i = 0; i < featureCount; i++) {
            weights[i] = betaVector.getEntry(i + 1);
        }

        double[] predictions = new double[yTest.length];
        for (int i = 0; i < xTest.length; i++) {
            predictions[i] = intercept;
            for (int j = 0; j < weights.length; j++) {
                predictions[i] += weights[j] * xTest[i][j];
            }
        }

        double meanY = StatUtils.mean(yTest);
        double ssTot = 0;
        double ssRes = 0;
        double mae = 0;
        for (int i = 0; i < yTest.length; i++) {
            ssTot += Math.pow(yTest[i] - meanY, 2);
            ssRes += Math.pow(yTest[i] - predictions[i], 2);
            mae += Math.abs(yTest[i] - predictions[i]);
        }
        double r2 = ssTot == 0 ? 1.0 : 1.0 - (ssRes / ssTot);
        double rmse = Math.sqrt(ssRes / Math.max(yTest.length, 1));
        mae = mae / Math.max(yTest.length, 1);
        double residualStdDev = Math.sqrt(ssRes / Math.max(yTest.length - 1, 1));

        return ModelArtifact.builder()
                .modelVersion(version)
                .modelType(modelType)
                .trainingDate(LocalDateTime.now())
                .trainingSamples(n)
                .weights(weights)
                .intercept(intercept)
                .r2(r2)
                .rmse(rmse)
                .mae(mae)
                .residualStdDev(residualStdDev)
                .featureMeans(means)
                .featureStdDevs(stdDevs)
                .active(false)
                .build();
    }

    public double predict(ModelArtifact artifact, FeatureVector vector) {
        List<String> ordered = new ArrayList<>(vector.toPersistedMap().keySet());
        double prediction = artifact.getIntercept();
        for (int i = 0; i < ordered.size() && i < artifact.getWeights().length; i++) {
            String featureName = ordered.get(i);
            double raw = vector.toPersistedMap().get(featureName);
            double mean = artifact.getFeatureMeans().getOrDefault(featureName, 0.0);
            double std = artifact.getFeatureStdDevs().getOrDefault(featureName, 1.0);
            prediction += artifact.getWeights()[i] * ((raw - mean) / Math.max(std, 1e-6));
        }
        return prediction;
    }
}

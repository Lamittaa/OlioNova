package com.zaytoun.aiprediction.util;

import com.zaytoun.aiprediction.exception.ImageProcessingException;
import com.zaytoun.aiprediction.ml.ColorIndexCalculator;
import com.zaytoun.aiprediction.ml.FeatureVector;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Component
@Slf4j
public class OliveImageProcessor {

    @Value("${ai.segmentation.lab-l-min:0}")
    private double lMin = 0;
    @Value("${ai.segmentation.lab-a-min:115}")
    private double aMin = 115;
    @Value("${ai.segmentation.lab-b-min:115}")
    private double bMin = 115;
    @Value("${ai.segmentation.lab-l-max:255}")
    private double lMax = 255;
    @Value("${ai.segmentation.lab-a-max:190}")
    private double aMax = 190;
    @Value("${ai.segmentation.lab-b-max:190}")
    private double bMax = 190;

    public FeatureVector extract(Path imagePath) {
        Mat source = imread(imagePath.toString());
        if (source == null || source.empty()) {
            throw new ImageProcessingException("Could not load image: " + imagePath);
        }

        Mat blurred = new Mat();
        GaussianBlur(source, blurred, new org.bytedeco.opencv.opencv_core.Size(5, 5), 0);

        Mat lab = new Mat();
        cvtColor(blurred, lab, COLOR_BGR2Lab);

        Mat mask = new Mat();
        inRange(lab,
                new Mat(1, 1, CV_8UC3, new org.bytedeco.opencv.opencv_core.Scalar(lMin, aMin, bMin, 0)),
                new Mat(1, 1, CV_8UC3, new org.bytedeco.opencv.opencv_core.Scalar(lMax, aMax, bMax, 0)),
                mask);

        Mat kernel = getStructuringElement(MORPH_ELLIPSE, new org.bytedeco.opencv.opencv_core.Size(5, 5));
        morphologyEx(mask, mask, MORPH_OPEN, kernel);
        morphologyEx(mask, mask, MORPH_CLOSE, kernel);

        if (countNonZero(mask) == 0) {
            log.warn("Segmentation failed, no fruit pixels detected for image {}", imagePath);
            return FeatureVector.builder()
                    .rMean(0)
                    .gMean(0)
                    .bMean(0)
                    .features(Map.of())
                    .segmentationSuccess(false)
                    .build();
        }

        MatVector channels = new MatVector();
        split(source, channels);
        Mat blue = channels.get(0);
        Mat green = channels.get(1);
        Mat red = channels.get(2);

        double rMean = mean(red, mask).get(0);
        double gMean = mean(green, mask).get(0);
        double bMean = mean(blue, mask).get(0);
        Map<String, Double> features = ColorIndexCalculator.compute(rMean, gMean, bMean);

        return FeatureVector.builder()
                .rMean(rMean)
                .gMean(gMean)
                .bMean(bMean)
                .features(features)
                .segmentationSuccess(true)
                .build();
    }
}

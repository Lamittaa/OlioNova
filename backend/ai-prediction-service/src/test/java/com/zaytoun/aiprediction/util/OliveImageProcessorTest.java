package com.zaytoun.aiprediction.util;

import com.zaytoun.aiprediction.ml.FeatureVector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OliveImageProcessorTest {
    private final OliveImageProcessor processor = new OliveImageProcessor();
    private Path tempFile;

    @AfterEach
    void cleanup() throws Exception {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void shouldSegmentFruitOnBlueBackground() throws Exception {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 200, 200);
        g.setColor(new Color(145, 155, 40));
        g.fillOval(40, 60, 120, 80);
        g.dispose();

        tempFile = Files.createTempFile("olive-sample", ".png");
        ImageIO.write(image, "png", tempFile.toFile());

        FeatureVector vector = processor.extract(tempFile);
        assertTrue(vector.isSegmentationSuccess());
        assertTrue(vector.getRMean() > 0);
        assertTrue(vector.getGMean() > 0);
        assertTrue(vector.getBMean() >= 0);
    }
}

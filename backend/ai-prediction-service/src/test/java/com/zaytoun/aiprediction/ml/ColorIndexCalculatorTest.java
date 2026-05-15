package com.zaytoun.aiprediction.ml;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ColorIndexCalculatorTest {

    @Test
    void shouldComputeRequiredIndexes() {
        Map<String, Double> values = ColorIndexCalculator.compute(100, 120, 80);
        assertEquals(46, values.size());
        assertTrue(values.containsKey("GLI"));
        assertTrue(values.containsKey("I35"));
        assertFalse(values.values().stream().anyMatch(d -> d.isNaN()));
        assertFalse(values.values().stream().anyMatch(d -> d.isInfinite()));
    }
}

package com.zaytoun.aiprediction.ml;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ColorIndexCalculator {
    private static final double EPS = 1e-6;

    private ColorIndexCalculator() {
    }

    public static Map<String, Double> compute(double r, double g, double b) {
        LinkedHashMap<String, Double> m = new LinkedHashMap<>();
        double GLI = (2 * g - r - b) / (2 * g + r + b + EPS);
        double HI = (2 * r - g - b) / (g - b + EPS);
        double NGRDI = (g - r) / (g + r + EPS);
        double GRAY = 0.299 * r + 0.587 * g + 0.114 * b;
        double EXG = 2 * g - r - b;
        double EXR = 1.4 * r - g;
        double EXGR = EXG - EXR;
        double VEG = g / (Math.pow(r + EPS, 0.667) * Math.pow(b + EPS, 0.333));
        double CIVE = 0.441 * r - 0.811 * g + 0.385 * b + 18.78745;
        double COM = EXG + CIVE + VEG;
        double GLAI = (3 * g - 2.4 * r - b) / (4 * g + r + b + EPS);
        double I1 = r + g + b;
        double I2 = r - b;
        double I3 = g - b;
        double I4 = r - g;
        double I5 = (r - g) / (r + g + EPS);
        double I6 = (g - b) / (g + b + EPS);
        double I7 = (r - b) / (r + b + EPS);
        double I8 = (r - g) / (r - b + EPS);
        double I9 = (g - b) / (r - b + EPS);
        double I10 = r / (g + EPS);
        double I11 = r / (b + EPS);
        double I12 = g / (b + EPS);
        double I13 = (r - g) / (r + g + EPS);
        double I14 = (r - b) / (r + b + EPS);
        double I15 = (g - b) / (g + b + EPS);
        double I16 = (2 * r - 2 * g + EPS) / (r + g + EPS);
        double I17 = (2 * r - 2 * b + EPS) / (r + b + EPS);
        double I18 = (2 * g - 2 * b + EPS) / (g + b + EPS);
        double I19 = r / (r + g + b + EPS);
        double I20 = g / (r + g + b + EPS);
        double I21 = b / (r + g + b + EPS);
        double I22 = (r - g) / (r + g + b + EPS);
        double I23 = (r - b) / (r + g + b + EPS);
        double I24 = (g - b) / (r + g + b + EPS);
        double I25 = Math.sqrt(r * r + g * g + b * b);
        double I26 = Math.sqrt(r * r - r * g + g * g);
        double I27 = Math.sqrt(r * r - r * b + b * b);
        double I28 = Math.sqrt(g * g - g * b + b * b);
        double I29 = (r - 0.5 * g - 0.5 * b) / (r + g + b + EPS);
        double I30 = (g - 0.5 * r - 0.5 * b) / (r + g + b + EPS);
        double I31 = (b - 0.5 * r - 0.5 * g) / (r + g + b + EPS);
        double I32 = Math.pow(r - g, 2) + Math.pow(r - b, 2) + Math.pow(g - b, 2);
        double I33 = Math.pow(r - g, 2) + Math.pow(r - b, 2);
        double I34 = Math.pow(r - g, 2) + Math.pow(g - b, 2);
        double I35 = Math.pow(r - b, 2) + Math.pow(g - b, 2);

        m.put("GLI", GLI);
        m.put("HI", HI);
        m.put("NGRDI", NGRDI);
        m.put("GRAY", GRAY);
        m.put("EXG", EXG);
        m.put("EXR", EXR);
        m.put("EXGR", EXGR);
        m.put("VEG", VEG);
        m.put("CIVE", CIVE);
        m.put("COM", COM);
        m.put("GLAI", GLAI);
        m.put("I1", I1);
        m.put("I2", I2);
        m.put("I3", I3);
        m.put("I4", I4);
        m.put("I5", I5);
        m.put("I6", I6);
        m.put("I7", I7);
        m.put("I8", I8);
        m.put("I9", I9);
        m.put("I10", I10);
        m.put("I11", I11);
        m.put("I12", I12);
        m.put("I13", I13);
        m.put("I14", I14);
        m.put("I15", I15);
        m.put("I16", I16);
        m.put("I17", I17);
        m.put("I18", I18);
        m.put("I19", I19);
        m.put("I20", I20);
        m.put("I21", I21);
        m.put("I22", I22);
        m.put("I23", I23);
        m.put("I24", I24);
        m.put("I25", I25);
        m.put("I26", I26);
        m.put("I27", I27);
        m.put("I28", I28);
        m.put("I29", I29);
        m.put("I30", I30);
        m.put("I31", I31);
        m.put("I32", I32);
        m.put("I33", I33);
        m.put("I34", I34);
        m.put("I35", I35);
        return m;
    }
}

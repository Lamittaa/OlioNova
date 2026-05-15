package com.zaytoun.aiprediction.validation;

import java.util.regex.Pattern;

public final class BatchIdRules {

    public static final String REGEX = "^[A-Za-z0-9](?:[A-Za-z0-9_-]{0,63})$";
    public static final String MESSAGE =
            "batchId must be 1-64 characters and contain only letters, numbers, hyphens, or underscores";

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private BatchIdRules() {
    }

    public static String requireValid(String batchId) {
        String normalized = batchId == null ? "" : batchId.trim();
        if (!PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(MESSAGE);
        }
        return normalized;
    }
}

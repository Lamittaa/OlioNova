package com.project.productionStages.model;

import java.util.Arrays;

public enum DashboardSort {
    QUEUE, STATUS, ID;

    public static DashboardSort of(String sort) {
        return Arrays.stream(DashboardSort.values()).filter(s -> s.name().equalsIgnoreCase(sort))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid sort name!"));
    }
}
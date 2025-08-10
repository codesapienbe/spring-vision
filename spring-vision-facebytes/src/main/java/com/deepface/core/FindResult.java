package com.deepface.core;

public record FindResult(
    String imagePath,
    double distance,
    double threshold,
    boolean matched
) {}

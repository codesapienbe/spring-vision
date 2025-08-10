package com.deepface.core;

import java.util.Map;

public record AnalysisResult(
    Integer age,
    String gender,
    String dominantEmotion,
    Map<String, Double> emotionDistribution,
    Map<String, Double> raceDistribution,
    FaceRegion faceRegion
) {}

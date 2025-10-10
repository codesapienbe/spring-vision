package io.github.codesapienbe.springvision.facebytes.core;

import java.util.Map;

/**
 * Represents the result of analyzing a single face for various attributes.
 * This record consolidates the predicted age, gender, emotion, and race distributions for a detected face.
 *
 * @param age                 The estimated age of the person.
 * @param gender              The predicted gender (e.g., "male", "female").
 * @param dominantEmotion     The emotion with the highest prediction score (e.g., "happy", "sad").
 * @param emotionDistribution A map containing the probability distribution for various emotions.
 * @param raceDistribution    A map containing the probability distribution for various racial ancestries.
 * @param faceRegion          The {@link FaceRegion} where the analysis was performed.
 */
public record AnalysisResult(
    Integer age,
    String gender,
    String dominantEmotion,
    Map<String, Double> emotionDistribution,
    Map<String, Double> raceDistribution,
    FaceRegion faceRegion
) {
}

package io.github.codesapienbe.springvision.persistence.dto;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.persistence.enums.SimilarityMetric;

import java.util.List;

/**
 * Result wrapper for face lookup matches.
 *
 * @param detectedFace the detected face from the query image
 * @param matches      the list of matching faces from the database
 * @param metric       the similarity metric used for the search
 */
public record FaceMatchResult(
    Detection detectedFace,
    List<SimilaritySearchResult> matches,
    SimilarityMetric metric
) {
}

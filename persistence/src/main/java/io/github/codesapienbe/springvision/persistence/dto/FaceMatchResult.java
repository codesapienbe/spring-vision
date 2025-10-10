package io.github.codesapienbe.springvision.persistence.dto;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.persistence.enums.SimilarityMetric;

import java.util.List;

/**
 * Result wrapper for face lookup matches.
 */
public record FaceMatchResult(
    Detection detectedFace,
    List<SimilaritySearchResult> matches,
    SimilarityMetric metric
) {
}

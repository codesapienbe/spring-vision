package com.springvision.jpa.dto;

import com.springvision.core.Detection;
import com.springvision.jpa.enums.SimilarityMetric;

import java.util.List;

/**
 * Result wrapper for face lookup matches.
 */
public record FaceMatchResult(
    Detection detectedFace,
    List<SimilaritySearchResult> matches,
    SimilarityMetric metric
) {} 
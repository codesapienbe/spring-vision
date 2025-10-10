package io.github.codesapienbe.springvision.persistence.dto;

import io.github.codesapienbe.springvision.persistence.enums.SimilarityMetric;

import java.util.Set;

/**
 * DTO representing a similarity search request.
 *
 * @param queryEmbedding   the query embedding vector to search for
 * @param modelName        the name of the model to search within
 * @param imageHash        optional hash to exclude the same image from results
 * @param metric           the similarity metric to use for comparison
 * @param threshold        minimum similarity threshold for results
 * @param limit            maximum number of results to return
 * @param includePersonIds optional set of person IDs to restrict search to
 * @param excludePersonIds optional set of person IDs to exclude from search
 */
public record SimilaritySearchRequest(
    float[] queryEmbedding,
    String modelName,
    String imageHash,
    SimilarityMetric metric,
    Double threshold,
    Integer limit,
    Set<String> includePersonIds,
    Set<String> excludePersonIds
) {
}

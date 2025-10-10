package io.github.codesapienbe.springvision.persistence.dto;

import io.github.codesapienbe.springvision.persistence.enums.SimilarityMetric;

import java.util.Set;

/**
 * Options for face lookup operations.
 */
public class FaceLookupOptions {

    private String modelName = "arcface";
    private SimilarityMetric metric = SimilarityMetric.COSINE;
    private Double threshold = 0.7;
    private Integer limit = 10;
    private Set<String> includePersonIds = java.util.Set.of();
    private Set<String> excludePersonIds = java.util.Set.of();

    /**
     * Default constructor.
     */
    public FaceLookupOptions() {
    }

    /**
     * Gets the model name.
     *
     * @return the model name
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Sets the model name.
     *
     * @param modelName the model name
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Gets the similarity metric.
     *
     * @return the similarity metric
     */
    public SimilarityMetric getMetric() {
        return metric;
    }

    /**
     * Sets the similarity metric.
     *
     * @param metric the similarity metric
     */
    public void setMetric(SimilarityMetric metric) {
        this.metric = metric;
    }

    /**
     * Gets the similarity threshold.
     *
     * @return the threshold
     */
    public Double getThreshold() {
        return threshold;
    }

    /**
     * Sets the similarity threshold.
     *
     * @param threshold the threshold
     */
    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    /**
     * Gets the maximum number of results.
     *
     * @return the limit
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * Sets the maximum number of results.
     *
     * @param limit the limit
     */
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    /**
     * Gets the set of person IDs to include in results.
     *
     * @return the include person IDs
     */
    public Set<String> getIncludePersonIds() {
        return includePersonIds;
    }

    /**
     * Sets the set of person IDs to include in results.
     *
     * @param includePersonIds the include person IDs
     */
    public void setIncludePersonIds(Set<String> includePersonIds) {
        this.includePersonIds = includePersonIds;
    }

    /**
     * Gets the set of person IDs to exclude from results.
     *
     * @return the exclude person IDs
     */
    public Set<String> getExcludePersonIds() {
        return excludePersonIds;
    }

    /**
     * Sets the set of person IDs to exclude from results.
     *
     * @param excludePersonIds the exclude person IDs
     */
    public void setExcludePersonIds(Set<String> excludePersonIds) {
        this.excludePersonIds = excludePersonIds;
    }
}

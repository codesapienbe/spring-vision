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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public SimilarityMetric getMetric() {
        return metric;
    }

    public void setMetric(SimilarityMetric metric) {
        this.metric = metric;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Set<String> getIncludePersonIds() {
        return includePersonIds;
    }

    public void setIncludePersonIds(Set<String> includePersonIds) {
        this.includePersonIds = includePersonIds;
    }

    public Set<String> getExcludePersonIds() {
        return excludePersonIds;
    }

    public void setExcludePersonIds(Set<String> excludePersonIds) {
        this.excludePersonIds = excludePersonIds;
    }
}

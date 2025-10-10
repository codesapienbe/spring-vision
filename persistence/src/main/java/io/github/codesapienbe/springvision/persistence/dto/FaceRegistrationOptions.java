package io.github.codesapienbe.springvision.persistence.dto;

import java.util.Map;

/**
 * Options for registering a face into the vector store.
 */
public class FaceRegistrationOptions {

    private String modelName = "arcface";
    private Map<String, Object> metadata = java.util.Map.of();

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
